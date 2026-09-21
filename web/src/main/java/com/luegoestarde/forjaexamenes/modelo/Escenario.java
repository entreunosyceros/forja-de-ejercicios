// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Escenario {

    private String id;
    private String modulo;
    private String titulo;
    private String enunciado;
    private Map<String, Object> parametros;
    private List<Criterio> criterios;
    private String solucionReferencia;
    private String generadoEn;
    private Integer dificultad;
    private String pistaGeneral;
    private String usuario;
    private String fechaHora;
    private String usuarioAcceso;
    /** Cache: la solución de referencia cumple todos los criterios. */
    private Boolean solucionReferenciaValida;
    private Double notaSolucionReferencia;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public Map<String, Object> getParametros() { return parametros; }
    public void setParametros(Map<String, Object> parametros) { this.parametros = parametros; }

    public List<Criterio> getCriterios() { return criterios; }
    public void setCriterios(List<Criterio> criterios) { this.criterios = criterios; }

    public String getSolucionReferencia() { return solucionReferencia; }
    public void setSolucionReferencia(String solucionReferencia) { this.solucionReferencia = solucionReferencia; }

    public String getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(String generadoEn) { this.generadoEn = generadoEn; }

    public Integer getDificultad() { return dificultad; }
    public void setDificultad(Integer dificultad) { this.dificultad = dificultad; }

    public String getPistaGeneral() { return pistaGeneral; }
    public void setPistaGeneral(String pistaGeneral) { this.pistaGeneral = pistaGeneral; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public String getUsuarioAcceso() { return usuarioAcceso; }
    public void setUsuarioAcceso(String usuarioAcceso) { this.usuarioAcceso = usuarioAcceso; }

    public Boolean getSolucionReferenciaValida() { return solucionReferenciaValida; }
    public void setSolucionReferenciaValida(Boolean solucionReferenciaValida) {
        this.solucionReferenciaValida = solucionReferenciaValida;
    }

    public Double getNotaSolucionReferencia() { return notaSolucionReferencia; }
    public void setNotaSolucionReferencia(Double notaSolucionReferencia) {
        this.notaSolucionReferencia = notaSolucionReferencia;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Criterio {
        private String esperado;
        private String pista;
        private String tipo;
        private String patron;
        private List<String> terminos;
        private int peso;
        private String banderas;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getPatron() { return patron; }
        public void setPatron(String patron) { this.patron = patron; }
        public List<String> getTerminos() { return terminos; }
        public void setTerminos(List<String> terminos) { this.terminos = terminos; }
        public int getPeso() { return peso; }
        public void setPeso(int peso) { this.peso = peso; }
        public String getBanderas() { return banderas; }
        /** Nombre canónico; acepta también el legacy {@code flags} al deserializar. */
        @JsonAlias("flags")
        public void setBanderas(String banderas) { this.banderas = banderas; }
        public String getEsperado() { return esperado; }
        public void setEsperado(String esperado) { this.esperado = esperado; }
        public String getPista() { return pista; }
        public void setPista(String pista) { this.pista = pista; }
    }
}
