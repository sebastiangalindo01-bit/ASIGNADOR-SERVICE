
package com.gestormemoria.asignadorservice.service;

import com.gestormemoria.asignadorservice.dto.SimulacionRequestDTO;
import com.gestormemoria.asignadorservice.dto.SimulacionResponseDTO;
import com.gestormemoria.asignadorservice.nativebridge.MemoriaNative;
import com.gestormemoria.asignadorservice.model.BloqueMemoria;
import com.gestormemoria.asignadorservice.model.EstrategiaEnum;
import com.gestormemoria.asignadorservice.model.ResultadoOperacion;
import com.sun.jna.ptr.IntByReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SimulacionService {

    private final MemoriaNative memoriaNative;

    public SimulacionService(MemoriaNative memoriaNative) {
        this.memoriaNative = memoriaNative;
    }

    // =========================================================
    // EJECUCION PRINCIPAL: BIBLIOTECA NATIVA C
    // =========================================================

    public SimulacionResponseDTO ejecutarNativo(SimulacionRequestDTO dto) {

        validarSolicitud(dto);

        int capacidadBloques = dto.getnBloques() + dto.getnOps();

        int[] estadoOcupante = new int[capacidadBloques];
        int[] estadoTamano = new int[capacidadBloques];
        int[] causasRechazo = new int[dto.getnOps()];

        IntByReference numeroBloquesFinal = new IntByReference();
        IntByReference fragmentacionInterna = new IntByReference();
        IntByReference fragmentacionExterna = new IntByReference();
        IntByReference huecoMayor = new IntByReference();

        int codigoEstrategia = dto.getEstrategia();

        int codigoRetorno = memoriaNative.simular(
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
                huecoMayor
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
            respuesta.setEstado("ERROR");
            respuesta.setMensaje(
                    "La biblioteca C no pudo completar la simulación. "
                            + "Código de retorno: " + codigoRetorno
            );
            return respuesta;
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

        respuesta.setFragmentacionInterna(
                fragmentacionInterna.getValue()
        );

        respuesta.setFragmentacionExterna(
                fragmentacionExterna.getValue()
        );

        respuesta.setHuecoMayor(huecoMayor.getValue());

        return respuesta;
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

        for (int i = 0; i < dto.getnOps(); i++) {

            int tipo = dto.getOpTipo()[i];
            int procesoId = dto.getOpProceso()[i];
            int tamano = dto.getOpTamano()[i];

            if (tipo == 0) {

                resultados.add(
                        asignar(memoria, i + 1, procesoId, tamano, estrategia)
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
            EstrategiaEnum estrategia) {

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
                memoria, tamano, estrategia
        );

        if (elegido == null) {
            return new ResultadoOperacion(
                    posicion, 0, procesoId, false,
                    "No existe un bloque libre con tamaño suficiente."
            );
        }

        int espacioOriginal = elegido.getTamano();

        elegido.setProcesoId(procesoId);

        if (espacioOriginal > tamano) {

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

        // DEPURACIÓN TEMPORAL
        System.out.println("========== DATOS RECIBIDOS ==========");
        System.out.println("nBloques: " + dto.getnBloques());
        System.out.println("bloqueInicio: " + java.util.Arrays.toString(dto.getBloqueInicio()));
        System.out.println("bloqueTamano: " + java.util.Arrays.toString(dto.getBloqueTamano()));
        System.out.println("nOps: " + dto.getnOps());
        System.out.println("opTipo: " + java.util.Arrays.toString(dto.getOpTipo()));
        System.out.println("opProceso: " + java.util.Arrays.toString(dto.getOpProceso()));
        System.out.println("opTamano: " + java.util.Arrays.toString(dto.getOpTamano()));
        System.out.println("estrategia: " + dto.getEstrategia());
        System.out.println("=====================================");

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
