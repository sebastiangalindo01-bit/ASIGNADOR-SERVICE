
package com.gestormemoria.asignadorservice.dto;

import com.gestormemoria.asignadorservice.model.BloqueMemoria;
import java.util.List;

public class SimulacionResponseDTO {

    private Long mapaId;
    private Long secuenciaId;

    private String estrategia;
    private int codigoEstrategia;

    private int codigoRetorno;
    private String estado;
    private String mensaje;

    private int numeroBloquesFinal;
    private List<BloqueMemoria> mapaFinal;

    private List<Integer> causasRechazo;
    private List<DecisionOperacionDTO> decisiones;

    private int fragmentacionInterna;
    private int fragmentacionExterna;
    private int huecoMayor;

    public SimulacionResponseDTO() {
    }

    public Long getMapaId() {
        return mapaId;
    }

    public void setMapaId(Long mapaId) {
        this.mapaId = mapaId;
    }

    public Long getSecuenciaId() {
        return secuenciaId;
    }

    public void setSecuenciaId(Long secuenciaId) {
        this.secuenciaId = secuenciaId;
    }

    public String getEstrategia() {
        return estrategia;
    }

    public void setEstrategia(String estrategia) {
        this.estrategia = estrategia;
    }

    public int getCodigoEstrategia() {
        return codigoEstrategia;
    }

    public void setCodigoEstrategia(int codigoEstrategia) {
        this.codigoEstrategia = codigoEstrategia;
    }

    public int getCodigoRetorno() {
        return codigoRetorno;
    }

    public void setCodigoRetorno(int codigoRetorno) {
        this.codigoRetorno = codigoRetorno;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public int getNumeroBloquesFinal() {
        return numeroBloquesFinal;
    }

    public void setNumeroBloquesFinal(int numeroBloquesFinal) {
        this.numeroBloquesFinal = numeroBloquesFinal;
    }

    public List<BloqueMemoria> getMapaFinal() {
        return mapaFinal;
    }

    public void setMapaFinal(List<BloqueMemoria> mapaFinal) {
        this.mapaFinal = mapaFinal;
    }

    public List<Integer> getCausasRechazo() {
        return causasRechazo;
    }

    public void setCausasRechazo(List<Integer> causasRechazo) {
        this.causasRechazo = causasRechazo;
    }

    public List<DecisionOperacionDTO> getDecisiones() { return decisiones; }
    public void setDecisiones(List<DecisionOperacionDTO> decisiones) { this.decisiones = decisiones; }

    public int getFragmentacionInterna() {
        return fragmentacionInterna;
    }

    public void setFragmentacionInterna(int fragmentacionInterna) {
        this.fragmentacionInterna = fragmentacionInterna;
    }

    public int getFragmentacionExterna() {
        return fragmentacionExterna;
    }

    public void setFragmentacionExterna(int fragmentacionExterna) {
        this.fragmentacionExterna = fragmentacionExterna;
    }

    public int getHuecoMayor() {
        return huecoMayor;
    }

    public void setHuecoMayor(int huecoMayor) {
        this.huecoMayor = huecoMayor;
    }
}
