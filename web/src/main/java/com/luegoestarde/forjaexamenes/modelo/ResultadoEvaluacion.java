// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultadoEvaluacion {

    private String examenId;
    private String modulo;
    private double nota;
    private boolean aprobado;
    private double pesoObtenido;
    private int pesoTotal;
    private String retroalimentacion;
    private List<DetalleCriterio> detalles;
    private String solucionReferencia;
    private String respuestaAlumno;
    private String usuario;
    private String fechaHoraInicio;
    private String fechaHoraEvaluacion;
    private Long tiempoSegundos;
    /** Nota solo por criterios comprobables (regex/términos), antes del juicio IA. */
    private Double notaElementos;
    /** Texto honesto: «X sobre los elementos que sé comprobar». */
    private String etiquetaNota;
    /** elementos | elementos_y_ia */
    private String medicion;
    private String modoCombinacion;
    private CorrectorIa correctorIa;

    public String getExamenId() { return examenId; }
    public void setExamenId(String examenId) { this.examenId = examenId; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public double getNota() { return nota; }
    public void setNota(double nota) { this.nota = nota; }

    public boolean isAprobado() { return aprobado; }
    public void setAprobado(boolean aprobado) { this.aprobado = aprobado; }

    public double getPesoObtenido() { return pesoObtenido; }
    public void setPesoObtenido(double pesoObtenido) { this.pesoObtenido = pesoObtenido; }

    public int getPesoTotal() { return pesoTotal; }
    public void setPesoTotal(int pesoTotal) { this.pesoTotal = pesoTotal; }

    public String getRetroalimentacion() { return retroalimentacion; }
    public void setRetroalimentacion(String retroalimentacion) { this.retroalimentacion = retroalimentacion; }

    @JsonProperty("feedback")
    public void setFeedbackLegacy(String feedback) { this.retroalimentacion = feedback; }

    public List<DetalleCriterio> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCriterio> detalles) { this.detalles = detalles; }

    public String getSolucionReferencia() { return solucionReferencia; }
    public void setSolucionReferencia(String solucionReferencia) { this.solucionReferencia = solucionReferencia; }

    public String getRespuestaAlumno() { return respuestaAlumno; }
    public void setRespuestaAlumno(String respuestaAlumno) { this.respuestaAlumno = respuestaAlumno; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getFechaHoraInicio() { return fechaHoraInicio; }
    public void setFechaHoraInicio(String fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }

    public String getFechaHoraEvaluacion() { return fechaHoraEvaluacion; }
    public void setFechaHoraEvaluacion(String fechaHoraEvaluacion) { this.fechaHoraEvaluacion = fechaHoraEvaluacion; }

    public Long getTiempoSegundos() { return tiempoSegundos; }
    public void setTiempoSegundos(Long tiempoSegundos) { this.tiempoSegundos = tiempoSegundos; }

    public Double getNotaElementos() { return notaElementos; }
    public void setNotaElementos(Double notaElementos) { this.notaElementos = notaElementos; }

    public String getEtiquetaNota() { return etiquetaNota; }
    public void setEtiquetaNota(String etiquetaNota) { this.etiquetaNota = etiquetaNota; }

    public String getMedicion() { return medicion; }
    public void setMedicion(String medicion) { this.medicion = medicion; }

    public String getModoCombinacion() { return modoCombinacion; }
    public void setModoCombinacion(String modoCombinacion) { this.modoCombinacion = modoCombinacion; }

    public CorrectorIa getCorrectorIa() { return correctorIa; }
    public void setCorrectorIa(CorrectorIa correctorIa) { this.correctorIa = correctorIa; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CorrectorIa {
        private boolean usado;
        private Double nota;
        private Boolean cumpleEnunciado;
        private String comentario;
        private String modelo;
        private String error;
        private String omitido;

        public boolean isUsado() { return usado; }
        public void setUsado(boolean usado) { this.usado = usado; }
        public Double getNota() { return nota; }
        public void setNota(Double nota) { this.nota = nota; }
        public Boolean getCumpleEnunciado() { return cumpleEnunciado; }
        public void setCumpleEnunciado(Boolean cumpleEnunciado) { this.cumpleEnunciado = cumpleEnunciado; }
        public String getComentario() { return comentario; }
        public void setComentario(String comentario) { this.comentario = comentario; }
        public String getModelo() { return modelo; }
        public void setModelo(String modelo) { this.modelo = modelo; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getOmitido() { return omitido; }
        public void setOmitido(String omitido) { this.omitido = omitido; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetalleCriterio {
        private boolean cumplido;
        private String patron;
        private List<String> terminos;
        private String tipo;
        private int peso;
        private String descripcion;
        private String error;
        private String esperado;
        private String pista;

        public boolean isCumplido() { return cumplido; }
        public void setCumplido(boolean cumplido) { this.cumplido = cumplido; }
        /** Compatibilidad JSON {@code ok}. */
        public boolean isOk() { return cumplido; }
        public void setOk(boolean ok) { this.cumplido = ok; }

        public String getPatron() { return patron; }
        public void setPatron(String patron) { this.patron = patron; }
        public List<String> getTerminos() { return terminos; }
        public void setTerminos(List<String> terminos) { this.terminos = terminos; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public int getPeso() { return peso; }
        public void setPeso(int peso) { this.peso = peso; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getEsperado() { return esperado; }
        public void setEsperado(String esperado) { this.esperado = esperado; }
        public String getPista() { return pista; }
        public void setPista(String pista) { this.pista = pista; }
    }
}
