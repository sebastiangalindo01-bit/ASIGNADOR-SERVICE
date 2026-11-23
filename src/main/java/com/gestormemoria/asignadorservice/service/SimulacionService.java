
package com.gestormemoria.asignadorservice.service;

import com.gestormemoria.asignadorservice.dto.SimulacionRequestDTO;
import com.gestormemoria.asignadorservice.dto.SimulacionResponseDTO;
import com.gestormemoria.asignadorservice.dto.DecisionOperacionDTO;
import com.gestormemoria.asignadorservice.nativebridge.MemoriaNative;
import com.gestormemoria.asignadorservice.model.BloqueMemoria;
import com.gestormemoria.asignadorservice.model.EstrategiaEnum;
import com.gestormemoria.asignadorservice.model.ResultadoOperacion;
import com.sun.jna.ptr.IntByReference;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SimulacionService {

    private final MemoriaNative memoriaNative;
    private final String motor;

    @Autowired
    public SimulacionService(ObjectProvider<MemoriaNative> memoriaNative, @Value("${asignador.motor:native}") String motor) {
        this.memoriaNative = memoriaNative.getIfAvailable();
        this.motor = motor;
    }

    public SimulacionService(MemoriaNative memoriaNative) {
        this.memoriaNative = memoriaNative;
        this.motor = "native";
    }

    // =========================================================
    // EJECUCION PRINCIPAL: BIBLIOTECA NATIVA C
    // =========================================================

    public SimulacionResponseDTO ejecutarNativo(SimulacionRequestDTO dto) {

        validarSolicitud(dto);
        if ("java".equalsIgnoreCase(motor)) return ejecutarJava(dto);
        if (memoriaNative == null) throw new IllegalStateException("El motor nativo no está disponible.");

        int capacidadBloques = dto.getnBloques() + dto.getnOps();

        int[] estadoOcupante = new int[capacidadBloques];
        int[] estadoTamano = new int[capacidadBloques];
        int[] causasRechazo = new int[dto.getnOps()];
        int[] libresAlRechazar = new int[dto.getnOps()];
        int[] huecosAlRechazar = new int[dto.getnOps()];
        int[] mayoresAlRechazar = new int[dto.getnOps()];

        IntByReference numeroBloquesFinal = new IntByReference();
        IntByReference fragmentacionInterna = new IntByReference();
        IntByReference fragmentacionExterna = new IntByReference();
        IntByReference huecoMayor = new IntByReference();

        int codigoEstrategia = dto.getEstrategia();

        int codigoRetorno = memoriaNative.simular_detallado(
                dto.getBloqueInicio(),
                dto.getBloqueTamano(),
                dto.getnBloques(),
                dto.getOpTipo(),
                dto.getOpProceso(),
                dto.getOpTamano(),
                dto.getnOps(),
                codigoEstrategia,
                estadoOcupante,
                estadoTamano,
                numeroBloquesFinal,
                causasRechazo,
                fragmentacionInterna,
                fragmentacionExterna,
                huecoMayor,
                libresAlRechazar,
                huecosAlRechazar,
                mayoresAlRechazar
        );

        SimulacionResponseDTO respuesta = new SimulacionResponseDTO();

        respuesta.setMapaId(dto.getMapaId());
        respuesta.setSecuenciaId(dto.getSecuenciaId());
        respuesta.setCodigoRetorno(codigoRetorno);

        EstrategiaEnum estrategia =
                EstrategiaEnum.deCodigo(codigoEstrategia);

        respuesta.setEstrategia(estrategia.name());
        respuesta.setCodigoEstrategia(codigoEstrategia);

        // Si C devuelve un error, no se deben leer sus métricas
        // ni el mapa final, porque la biblioteca no garantiza
        // que esos valores hayan sido escritos.
        if (codigoRetorno != 0) {
            throw new IllegalArgumentException(mensajeError(codigoRetorno));
        }

        int cantidadBloques = numeroBloquesFinal.getValue();

        if (cantidadBloques < 0 || cantidadBloques > capacidadBloques) {
            throw new IllegalStateException(
                    "La biblioteca C devolvió una cantidad de bloques inválida."
            );
        }

        // Reconstruir el mapa final.
        List<BloqueMemoria> mapaFinal = new ArrayList<>();

        int posicionActual = dto.getBloqueInicio()[0];

        for (int i = 0; i < cantidadBloques; i++) {

            int tamano = estadoTamano[i];
            int procesoId = estadoOcupante[i];

            if (tamano <= 0) {
                throw new IllegalStateException(
                        "La biblioteca C devolvió un bloque con tamaño inválido."
                );
            }

            BloqueMemoria bloque = new BloqueMemoria(
                    posicionActual,
                    tamano,
                    procesoId
            );

            mapaFinal.add(bloque);
            posicionActual += tamano;
        }

        // Convertir las causas de rechazo a una lista.
        List<Integer> listaCausas = new ArrayList<>();

        for (int causa : causasRechazo) {
            listaCausas.add(causa);
        }

        respuesta.setEstado("COMPLETADA");
        respuesta.setMensaje("Simulación ejecutada correctamente.");

        respuesta.setNumeroBloquesFinal(cantidadBloques);
        respuesta.setMapaFinal(mapaFinal);
        respuesta.setCausasRechazo(listaCausas);
        respuesta.setDecisiones(decisiones(dto, causasRechazo, libresAlRechazar, huecosAlRechazar, mayoresAlRechazar));

        respuesta.setFragmentacionInterna(
                fragmentacionInterna.getValue()
        );

        respuesta.setFragmentacionExterna(
                fragmentacionExterna.getValue()
        );

        respuesta.setHuecoMayor(huecoMayor.getValue());

        return respuesta;
    }

    private String mensajeError(int codigo) {
        return switch (codigo) {
            case -4 -> "No se puede liberar el proceso porque no tiene memoria asignada.";
            case -5 -> "El proceso ya tiene memoria asignada y no puede asignarse otra vez.";
            case -2 -> "La estrategia seleccionada no es válida.";
            case -3 -> "El mapa de memoria no es válido o no está contiguo.";
            case -6, -7 -> "La secuencia contiene una operación inválida.";
            default -> "No fue posible completar la simulación. Revisa el mapa y la secuencia.";
        };
    }

    private List<DecisionOperacionDTO> decisiones(SimulacionRequestDTO dto, int[] causas, int[] libres, int[] huecos, int[] mayores) {
        List<DecisionOperacionDTO> items = new ArrayList<>();
        for (int i = 0; i < dto.getnOps(); i++) {
            boolean asignar = dto.getOpTipo()[i] == 0;
            boolean aceptada = causas[i] == 0;
            String causa = causas[i] == 1 ? "SIN_MEMORIA" : causas[i] == 2 ? "FRAGMENTACION_EXTERNA" : null;
            String mensaje = "Operación completada.";
            if (causa != null) {
                int pedido = dto.getOpTamano()[i];
                mensaje = causa.equals("SIN_MEMORIA")
                        ? "P" + dto.getOpProceso()[i] + " pidió " + pedido + " KB. Hay " + libres[i] + " KB libres en total, insuficientes para atender la solicitud."
                        : "P" + dto.getOpProceso()[i] + " pidió " + pedido + " KB. Hay " + libres[i] + " KB libres en " + huecos[i] + " huecos y el más grande es de " + mayores[i] + " KB. Esto es fragmentación externa: hay memoria suficiente en total, pero no en un espacio contiguo.";
            }
            items.add(new DecisionOperacionDTO(i + 1, asignar ? "ASIGNAR" : "LIBERAR", dto.getOpProceso()[i], dto.getOpTamano()[i], aceptada, causa, libres[i], huecos[i], mayores[i], mensaje));
        }
        return items;
    }

    private SimulacionResponseDTO ejecutarJava(SimulacionRequestDTO dto) {
        EstrategiaEnum estrategia = EstrategiaEnum.deCodigo(dto.getEstrategia());
        List<BloqueMemoria> memoria = new ArrayList<>();
        for (int i = 0; i < dto.getnBloques(); i++) memoria.add(new BloqueMemoria(dto.getBloqueInicio()[i], dto.getBloqueTamano()[i], -1));
        Map<Integer, Integer> pedidos = new java.util.HashMap<>();
        int[] causas = new int[dto.getnOps()], libres = new int[dto.getnOps()], huecos = new int[dto.getnOps()], mayores = new int[dto.getnOps()];
        int[] cursorDireccion = { dto.getBloqueInicio()[0] };
        for (int i = 0; i < dto.getnOps(); i++) {
            int tipo = dto.getOpTipo()[i], proceso = dto.getOpProceso()[i], tamano = dto.getOpTamano()[i];
            if (tipo == 0) {
                if (buscarProceso(memoria, proceso) != null) throw new IllegalArgumentException("El proceso P" + proceso + " ya tiene memoria asignada.");
                BloqueMemoria bloque = buscarBloqueDisponible(memoria, tamano, estrategia, cursorDireccion[0]);
                if (bloque == null) {
                    long libre = 0; int mayor = 0, count = 0;
                    for (BloqueMemoria b : memoria) if (b.estaLibre()) { libre += b.getTamano(); mayor = Math.max(mayor, b.getTamano()); count++; }
                    causas[i] = libre < tamano ? 1 : 2; libres[i] = (int) libre; huecos[i] = count; mayores[i] = mayor;
                    continue;
                }
                int resto = bloque.getTamano() - tamano;
                if (resto >= 8) {
                    bloque.setTamano(tamano);
                    memoria.add(new BloqueMemoria(bloque.getInicio() + tamano, resto, -1));
                    memoria.sort(Comparator.comparingInt(BloqueMemoria::getInicio));
                }
                bloque.setProcesoId(proceso);
                pedidos.put(proceso, tamano);
                cursorDireccion[0] = bloque.getInicio() + bloque.getTamano();
            } else {
                BloqueMemoria bloque = buscarProceso(memoria, proceso);
                if (bloque == null) throw new IllegalArgumentException("No se puede liberar P" + proceso + " porque no tiene memoria asignada.");
                bloque.setProcesoId(-1); pedidos.remove(proceso); fusionarBloquesLibres(memoria);
            }
        }
        int libre = 0, mayor = 0, interna = 0;
        for (BloqueMemoria b : memoria) {
            if (b.estaLibre()) { libre += b.getTamano(); mayor = Math.max(mayor, b.getTamano()); }
            else interna += b.getTamano() - pedidos.getOrDefault(b.getProcesoId(), b.getTamano());
        }
        SimulacionResponseDTO r = new SimulacionResponseDTO();
        r.setMapaId(dto.getMapaId()); r.setSecuenciaId(dto.getSecuenciaId()); r.setEstrategia(estrategia.name()); r.setCodigoEstrategia(estrategia.getCodigo());
        r.setCodigoRetorno(0); r.setEstado("COMPLETADA"); r.setMensaje("Simulación ejecutada correctamente con el motor Java.");
        r.setNumeroBloquesFinal(memoria.size()); r.setMapaFinal(memoria); r.setCausasRechazo(java.util.Arrays.stream(causas).boxed().toList());
        r.setDecisiones(decisiones(dto, causas, libres, huecos, mayores)); r.setFragmentacionInterna(interna); r.setFragmentacionExterna(libre - mayor); r.setHuecoMayor(mayor);
        return r;
    }

    // =========================================================
    // SIMULADOR JAVA ANTERIOR
    // Se conserva como respaldo, no se utiliza en el endpoint
    // principal mientras ejecutamos mediante C.
    // =========================================================

    public List<ResultadoOperacion> ejecutar(SimulacionRequestDTO dto) {

        validarSolicitud(dto);

        List<BloqueMemoria> memoria = new ArrayList<>();

        for (int i = 0; i < dto.getnBloques(); i++) {

            memoria.add(new BloqueMemoria(
                    dto.getBloqueInicio()[i],
                    dto.getBloqueTamano()[i],
                    -1
            ));
        }

        memoria.sort(Comparator.comparingInt(BloqueMemoria::getInicio));

        EstrategiaEnum estrategia =
                EstrategiaEnum.deCodigo(dto.getEstrategia());

        List<ResultadoOperacion> resultados = new ArrayList<>();
        int[] cursorDireccion = { dto.getBloqueInicio()[0] };

        for (int i = 0; i < dto.getnOps(); i++) {

            int tipo = dto.getOpTipo()[i];
            int procesoId = dto.getOpProceso()[i];
            int tamano = dto.getOpTamano()[i];

            if (tipo == 0) {

                resultados.add(
                        asignar(memoria, i + 1, procesoId, tamano, estrategia, cursorDireccion)
                );

            } else if (tipo == 1) {

                resultados.add(
                        liberar(memoria, i + 1, procesoId)
                );

            } else {

                resultados.add(new ResultadoOperacion(
                        i + 1,
                        tipo,
                        procesoId,
                        false,
                        "Tipo de operación no válido."
                ));
            }
        }

        return resultados;
    }

    private ResultadoOperacion asignar(
            List<BloqueMemoria> memoria,
            int posicion,
            int procesoId,
            int tamano,
            EstrategiaEnum estrategia,
            int[] cursorDireccion) {

        if (procesoId < 0 || tamano <= 0) {
            return new ResultadoOperacion(
                    posicion, 0, procesoId, false,
                    "Identificador o tamaño de asignación no válido."
            );
        }

        if (buscarProceso(memoria, procesoId) != null) {
            return new ResultadoOperacion(
                    posicion, 0, procesoId, false,
                    "El proceso ya tiene memoria asignada."
            );
        }

        BloqueMemoria elegido = buscarBloqueDisponible(
                memoria, tamano, estrategia, cursorDireccion[0]
        );

        if (elegido == null) {
            return new ResultadoOperacion(
                    posicion, 0, procesoId, false,
                    "No existe un bloque libre con tamaño suficiente."
            );
        }

        int espacioOriginal = elegido.getTamano();

        elegido.setProcesoId(procesoId);

        if (espacioOriginal - tamano >= 8) {

            int nuevoInicio = elegido.getInicio() + tamano;
            int espacioRestante = espacioOriginal - tamano;

            elegido.setTamano(tamano);

            BloqueMemoria bloqueLibre = new BloqueMemoria(
                    nuevoInicio,
                    espacioRestante,
                    -1
            );

            memoria.add(bloqueLibre);

            memoria.sort(Comparator.comparingInt(
                    BloqueMemoria::getInicio
            ));
        }

        cursorDireccion[0] = elegido.getInicio() + elegido.getTamano();

        return new ResultadoOperacion(
                posicion, 0, procesoId, true,
                "Memoria asignada correctamente."
        );
    }

    private ResultadoOperacion liberar(
            List<BloqueMemoria> memoria,
            int posicion,
            int procesoId) {

        if (procesoId < 0) {
            return new ResultadoOperacion(
                    posicion, 1, procesoId, false,
                    "Identificador de proceso no válido."
            );
        }

        BloqueMemoria bloque = buscarProceso(memoria, procesoId);

        if (bloque == null) {
            return new ResultadoOperacion(
                    posicion, 1, procesoId, false,
                    "El proceso no tiene memoria asignada."
            );
        }

        bloque.setProcesoId(-1);

        fusionarBloquesLibres(memoria);

        return new ResultadoOperacion(
                posicion, 1, procesoId, true,
                "Memoria liberada correctamente."
        );
    }

    private BloqueMemoria buscarProceso(
            List<BloqueMemoria> memoria,
            int procesoId) {

        for (BloqueMemoria bloque : memoria) {

            if (!bloque.estaLibre()
                    && bloque.getProcesoId() == procesoId) {
                return bloque;
            }
        }

        return null;
    }

    private BloqueMemoria buscarBloqueDisponible(
            List<BloqueMemoria> memoria,
            int tamano,
            EstrategiaEnum estrategia) {
        return buscarBloqueDisponible(memoria, tamano, estrategia,
                memoria.isEmpty() ? 0 : memoria.get(0).getInicio());
    }

    private BloqueMemoria buscarBloqueDisponible(
            List<BloqueMemoria> memoria,
            int tamano,
            EstrategiaEnum estrategia,
            int cursorDireccion) {

        List<BloqueMemoria> candidatos = new ArrayList<>();

        for (BloqueMemoria bloque : memoria) {

            if (bloque.estaLibre() && bloque.getTamano() >= tamano) {
                candidatos.add(bloque);
            }
        }

        if (candidatos.isEmpty()) {
            return null;
        }

        return switch (estrategia) {

            case FIRST_FIT -> candidatos.get(0);

            case BEST_FIT -> candidatos.stream()
                    .min(Comparator.comparingInt(
                            BloqueMemoria::getTamano
                    ))
                    .orElse(null);

            case WORST_FIT -> candidatos.stream()
                    .max(Comparator.comparingInt(
                            BloqueMemoria::getTamano
                    ))
                    .orElse(null);

            case NEXT_FIT -> candidatos.stream()
                    .filter(b -> b.getInicio() + b.getTamano() > cursorDireccion)
                    .findFirst().orElse(candidatos.get(0));
        };
    }

    private void fusionarBloquesLibres(
            List<BloqueMemoria> memoria) {

        memoria.sort(Comparator.comparingInt(
                BloqueMemoria::getInicio
        ));

        int i = 0;

        while (i < memoria.size() - 1) {

            BloqueMemoria actual = memoria.get(i);
            BloqueMemoria siguiente = memoria.get(i + 1);

            boolean ambosLibres =
                    actual.estaLibre() && siguiente.estaLibre();

            boolean adyacentes =
                    actual.getInicio() + actual.getTamano()
                            == siguiente.getInicio();

            if (ambosLibres && adyacentes) {

                actual.setTamano(
                        actual.getTamano() + siguiente.getTamano()
                );

                memoria.remove(i + 1);

            } else {
                i++;
            }
        }
    }

    // =========================================================
    // VALIDACIONES
    // =========================================================

    private void validarSolicitud(SimulacionRequestDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "La solicitud no puede estar vacía."
            );
        }

        if (dto.getnBloques() <= 0
                || dto.getBloqueInicio() == null
                || dto.getBloqueTamano() == null
                || dto.getBloqueInicio().length != dto.getnBloques()
                || dto.getBloqueTamano().length != dto.getnBloques()) {

            throw new IllegalArgumentException(
                    "La información de los bloques no es válida."
            );
        }

        if (dto.getnOps() <= 0
                || dto.getOpTipo() == null
                || dto.getOpProceso() == null
                || dto.getOpTamano() == null
                || dto.getOpTipo().length != dto.getnOps()
                || dto.getOpProceso().length != dto.getnOps()
                || dto.getOpTamano().length != dto.getnOps()) {

            throw new IllegalArgumentException(
                    "La información de las operaciones no es válida."
            );
        }

        // La biblioteca C espera bloques ordenados y contiguos.
        for (int i = 0; i < dto.getnBloques(); i++) {

            if (dto.getBloqueInicio()[i] < 0
                    || dto.getBloqueTamano()[i] <= 0) {

                throw new IllegalArgumentException(
                        "Las direcciones y tamaños de los bloques deben ser válidos."
                );
            }

            if (i > 0) {

                long finBloqueAnterior =
                        (long) dto.getBloqueInicio()[i - 1]
                                + dto.getBloqueTamano()[i - 1];

                if (finBloqueAnterior != dto.getBloqueInicio()[i]) {
                    throw new IllegalArgumentException(
                            "Los bloques deben estar ordenados y ser contiguos."
                    );
                }
            }
        }

        EstrategiaEnum.deCodigo(dto.getEstrategia());
    }
}
