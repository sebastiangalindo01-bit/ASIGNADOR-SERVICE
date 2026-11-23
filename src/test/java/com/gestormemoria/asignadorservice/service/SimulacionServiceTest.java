
package com.gestormemoria.asignadorservice.service;

import com.gestormemoria.asignadorservice.dto.SimulacionRequestDTO;
import com.gestormemoria.asignadorservice.model.ResultadoOperacion;
import com.gestormemoria.asignadorservice.nativebridge.MemoriaNative;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SimulacionServiceTest {

    private final MemoriaNative memoriaNative = mock(MemoriaNative.class);

    private final SimulacionService simulacionService =
            new SimulacionService(memoriaNative);

    private SimulacionRequestDTO crearSolicitud(
            int[] tipos,
            int[] procesos,
            int[] tamanos,
            int estrategia) {

        SimulacionRequestDTO dto = new SimulacionRequestDTO();

        dto.setnBloques(1);
        dto.setBloqueInicio(new int[]{0});
        dto.setBloqueTamano(new int[]{1000});

        dto.setnOps(tipos.length);
        dto.setOpTipo(tipos);
        dto.setOpProceso(procesos);
        dto.setOpTamano(tamanos);

        dto.setEstrategia(estrategia);

        return dto;
    }

    @Test
    void debeAsignarMemoriaCorrectamente() {

        SimulacionRequestDTO dto = crearSolicitud(
                new int[]{0},
                new int[]{1},
                new int[]{200},
                0
        );

        List<ResultadoOperacion> resultados =
                simulacionService.ejecutar(dto);

        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).isAceptada());
        assertEquals(
                "Memoria asignada correctamente.",
                resultados.get(0).getMensaje()
        );
    }

    @Test
    void noDebeAsignarDosVecesAlMismoProceso() {

        SimulacionRequestDTO dto = crearSolicitud(
                new int[]{0, 0},
                new int[]{1, 1},
                new int[]{200, 100},
                0
        );

        List<ResultadoOperacion> resultados =
                simulacionService.ejecutar(dto);

        assertTrue(resultados.get(0).isAceptada());
        assertFalse(resultados.get(1).isAceptada());
    }

    @Test
    void debeRechazarUnaAsignacionSinEspacioSuficiente() {

        SimulacionRequestDTO dto = crearSolicitud(
                new int[]{0},
                new int[]{1},
                new int[]{1200},
                0
        );

        List<ResultadoOperacion> resultados =
                simulacionService.ejecutar(dto);

        assertFalse(resultados.get(0).isAceptada());
    }

    @Test
    void debeLiberarLaMemoriaDeUnProceso() {

        SimulacionRequestDTO dto = crearSolicitud(
                new int[]{0, 1},
                new int[]{1, 1},
                new int[]{200, 0},
                0
        );

        List<ResultadoOperacion> resultados =
                simulacionService.ejecutar(dto);

        assertTrue(resultados.get(0).isAceptada());
        assertTrue(resultados.get(1).isAceptada());
        assertEquals(
                "Memoria liberada correctamente.",
                resultados.get(1).getMensaje()
        );
    }

    @Test
    void debeFusionarBloquesLibresAdyacentes() {

        SimulacionRequestDTO dto = crearSolicitud(
                new int[]{0, 0, 0, 1, 1, 0},
                new int[]{1, 2, 3, 1, 2, 4},
                new int[]{100, 200, 100, 0, 0, 250},
                0
        );

        List<ResultadoOperacion> resultados =
                simulacionService.ejecutar(dto);

        assertTrue(resultados.get(0).isAceptada());
        assertTrue(resultados.get(1).isAceptada());
        assertTrue(resultados.get(2).isAceptada());
        assertTrue(resultados.get(3).isAceptada());
        assertTrue(resultados.get(4).isAceptada());

        // P1 y P2 se liberan y sus bloques se fusionan.
        // P4 necesita 250 unidades y debe poder asignarse.
        assertTrue(resultados.get(5).isAceptada());
    }

    @Test
    void noDebeLiberarUnProcesoInexistente() {

        SimulacionRequestDTO dto = crearSolicitud(
                new int[]{1},
                new int[]{99},
                new int[]{0},
                0
        );

        List<ResultadoOperacion> resultados =
                simulacionService.ejecutar(dto);

        assertFalse(resultados.get(0).isAceptada());
    }
}
