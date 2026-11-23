package com.gestormemoria.asignadorservice.dto;

public record DecisionOperacionDTO(int posicion, String tipo, int procesoId, int tamanoSolicitado,
                                  boolean aceptada, String causaRechazo, int libreTotal,
                                  int numeroHuecos, int huecoMayor, String mensaje) { }
