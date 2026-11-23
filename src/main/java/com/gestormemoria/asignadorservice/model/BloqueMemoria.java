package com.gestormemoria.asignadorservice.model;

public class BloqueMemoria {

    private int inicio;
    private int tamano;
    private int procesoId;

    public BloqueMemoria(int inicio, int tamano, int procesoId) {
        this.inicio = inicio;
        this.tamano = tamano;
        this.procesoId = procesoId;
    }

    public int getInicio() {
        return inicio;
    }

    public void setInicio(int inicio) {
        this.inicio = inicio;
    }

    public int getTamano() {
        return tamano;
    }

    public void setTamano(int tamano) {
        this.tamano = tamano;
    }

    public int getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(int procesoId) {
        this.procesoId = procesoId;
    }

    public boolean estaLibre() {
        return procesoId == -1;
    }
}
