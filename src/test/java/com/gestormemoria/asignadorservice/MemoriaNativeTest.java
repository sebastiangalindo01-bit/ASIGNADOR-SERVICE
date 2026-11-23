
package com.gestormemoria.asignadorservice;

import com.gestormemoria.asignadorservice.nativebridge.MemoriaNative;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoriaNativeTest {

    @Test
    void debeEjecutarSimulacionDesdeC() {

        // Cargar biblioteca nativa
        String ruta = Path.of(
                "../gestor-memoria-click/c/libmemoria.dylib"
        ).toAbsolutePath().normalize().toString();

        MemoriaNative memoria = Native.load(
                ruta,
                MemoriaNative.class
        );

        // Mapa: un bloque libre de 100 KB
        int[] bloqueInicio = {0};
        int[] bloqueTamano = {100};
        int nBloques = 1;

        // Operación: asignar 40 KB al proceso 1
        // 0 = ASIGNAR
        int[] opTipo = {0};
        int[] opProceso = {1};
        int[] opTamano = {40};
        int nOps = 1;

        // First Fit
        int estrategia = 0;

        // Arreglos de salida
        int capacidad = nBloques + nOps;

        int[] estadoOcupante = new int[capacidad];
        int[] estadoTamano = new int[capacidad];
        int[] causaRechazo = new int[nOps];

        IntByReference nBloquesFinal = new IntByReference();
        IntByReference fragInterna = new IntByReference();
        IntByReference fragExterna = new IntByReference();
        IntByReference huecoMayor = new IntByReference();

        // Ejecutar función C
        int resultado = memoria.simular(
                bloqueInicio,
                bloqueTamano,
                nBloques,
                opTipo,
                opProceso,
                opTamano,
                nOps,
                estrategia,
                estadoOcupante,
                estadoTamano,
                nBloquesFinal,
                causaRechazo,
                fragInterna,
                fragExterna,
                huecoMayor
        );

        // Mostrar resultados
        System.out.println("Código de retorno: " + resultado);
        System.out.println("Bloques finales: " + nBloquesFinal.getValue());
        System.out.println("Ocupantes: " + java.util.Arrays.toString(estadoOcupante));
        System.out.println("Tamaños: " + java.util.Arrays.toString(estadoTamano));
        System.out.println("Causa rechazo: " + java.util.Arrays.toString(causaRechazo));

        // Validaciones
        assertEquals(0, resultado);
        assertEquals(2, nBloquesFinal.getValue());
        assertEquals(1, estadoOcupante[0]);
        assertEquals(40, estadoTamano[0]);
        assertEquals(-1, estadoOcupante[1]);
        assertEquals(60, estadoTamano[1]);
    }
}
