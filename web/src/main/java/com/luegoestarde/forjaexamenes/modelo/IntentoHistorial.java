package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntentoHistorial {

    private String id = "";
    private String fecha = "";
    private String modulo = "";
    private String titulo = "";
    private double nota;
    private boolean aprobado;
    private long tiempoSegundos;
    private String ejercicioId = "";
    private String enunciado = "";
    private String respuesta = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public double getNota() { return nota; }
    public void setNota(double nota) { this.nota = nota; }

    public boolean isAprobado() { return aprobado; }
    public void setAprobado(boolean aprobado) { this.aprobado = aprobado; }

    public long getTiempoSegundos() { return tiempoSegundos; }
    public void setTiempoSegundos(long tiempoSegundos) { this.tiempoSegundos = tiempoSegundos; }

    public String getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(String ejercicioId) { this.ejercicioId = ejercicioId; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
}
