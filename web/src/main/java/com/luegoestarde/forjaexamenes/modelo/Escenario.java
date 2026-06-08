package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String usuario;
    private String fechaHora;
    private String usuarioAcceso;

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

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public String getUsuarioAcceso() { return usuarioAcceso; }
    public void setUsuarioAcceso(String usuarioAcceso) { this.usuarioAcceso = usuarioAcceso; }

    @JsonIgnoreProperties(ignoreUnknown = true)
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
        public void setBanderas(String banderas) { this.banderas = banderas; }
        /** Compatibilidad JSON legacy {@code flags}. */
        public String getFlags() { return banderas; }
        public void setFlags(String flags) { this.banderas = flags; }
        public String getEsperado() { return esperado; }
        public void setEsperado(String esperado) { this.esperado = esperado; }
        public String getPista() { return pista; }
        public void setPista(String pista) { this.pista = pista; }
    }
}
