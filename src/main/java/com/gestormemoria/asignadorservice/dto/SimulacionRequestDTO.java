package com.gestormemoria.asignadorservice.dto;

public class SimulacionRequestDTO {
    private Long mapaId;
    private Long secuenciaId;
    private int[] bloqueInicio;
    private int[] bloqueTamano;
    private int nBloques;
    private int[] opTipo;
    private int[] opProceso;
    private int[] opTamano;
    private int nOps;
    private int estrategia;

    public Long getMapaId() { return mapaId; }
    public void setMapaId(Long mapaId) { this.mapaId = mapaId; }

    public Long getSecuenciaId() { return secuenciaId; }
    public void setSecuenciaId(Long secuenciaId) { this.secuenciaId = secuenciaId; }

    public int[] getBloqueInicio() { return bloqueInicio; }
    public void setBloqueInicio(int[] bloqueInicio) { this.bloqueInicio = bloqueInicio; }

    public int[] getBloqueTamano() { return bloqueTamano; }
    public void setBloqueTamano(int[] bloqueTamano) { this.bloqueTamano = bloqueTamano; }

    public int getnBloques() { return nBloques; }
    public void setnBloques(int nBloques) { this.nBloques = nBloques; }

    public int[] getOpTipo() { return opTipo; }
    public void setOpTipo(int[] opTipo) { this.opTipo = opTipo; }

    public int[] getOpProceso() { return opProceso; }
    public void setOpProceso(int[] opProceso) { this.opProceso = opProceso; }

    public int[] getOpTamano() { return opTamano; }
    public void setOpTamano(int[] opTamano) { this.opTamano = opTamano; }

    public int getnOps() { return nOps; }
    public void setnOps(int nOps) { this.nOps = nOps; }

    public int getEstrategia() { return estrategia; }
    public void setEstrategia(int estrategia) { this.estrategia = estrategia; }
}