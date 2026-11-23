package com.gestormemoria.asignadorservice.nativebridge;

import com.sun.jna.Library;
import com.sun.jna.ptr.IntByReference;

public interface MemoriaNative extends Library {

    int simular(
            int[] bloqueInicio,
            int[] bloqueTamano,
            int nBloques,

            int[] opTipo,
            int[] opProceso,
            int[] opTamano,
            int nOps,

            int estrategia,

            int[] estadoOcupante,
            int[] estadoTamano,
            IntByReference nBloquesFinal,
            int[] causaRechazo,
            IntByReference fragInterna,
            IntByReference fragExterna,
            IntByReference huecoMayor
    );

    int simular_detallado(
            int[] bloqueInicio, int[] bloqueTamano, int nBloques,
            int[] opTipo, int[] opProceso, int[] opTamano, int nOps, int estrategia,
            int[] estadoOcupante, int[] estadoTamano, IntByReference nBloquesFinal,
            int[] causaRechazo, IntByReference fragInterna, IntByReference fragExterna,
            IntByReference huecoMayor, int[] libreEnRechazo, int[] huecosEnRechazo,
            int[] mayorEnRechazo
    );
}
