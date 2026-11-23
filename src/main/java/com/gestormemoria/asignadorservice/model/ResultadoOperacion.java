package com.gestormemoria.asignadorservice.model;

public class ResultadoOperacion {

    private int posicion;
    private int tipoOperacion;
    private int procesoId;
    private boolean aceptada;
    private String mensaje;

    public ResultadoOperacion(
            int posicion,
            int tipoOperacion,
            int procesoId,
            boolean aceptada,
            String mensaje) {

        this.posicion = posicion;
        this.tipoOperacion = tipoOperacion;
        this.procesoId = procesoId;
        this.aceptada = aceptada;
        this.mensaje = mensaje;
    }

    public int getPosicion() {
        return posicion;
    }

    public int getTipoOperacion() {
        return tipoOperacion;
    }

    public int getProcesoId() {
        return procesoId;
    }

    public boolean isAceptada() {
        return aceptada;
    }

    public String getMensaje() {
        return mensaje;
    }
}
