package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EstadisticasUsuario {

    private int totalIntentos;
    private int totalAprobados;
    private int totalSuspensos;
    private double notaMedia;
    private double mejorNota;
    private int rachaActual;
    private long tiempoTotalSegundos;
    private String ultimaActividad = "";
    private Map<String, EstadisticasModulo> porModulo = new LinkedHashMap<>();

    public int getTotalIntentos() { return totalIntentos; }
    public void setTotalIntentos(int totalIntentos) { this.totalIntentos = totalIntentos; }

    public int getTotalAprobados() { return totalAprobados; }
    public void setTotalAprobados(int totalAprobados) { this.totalAprobados = totalAprobados; }

    public int getTotalSuspensos() { return totalSuspensos; }
    public void setTotalSuspensos(int totalSuspensos) { this.totalSuspensos = totalSuspensos; }

    public double getNotaMedia() { return notaMedia; }
    public void setNotaMedia(double notaMedia) { this.notaMedia = notaMedia; }

    public double getMejorNota() { return mejorNota; }
    public void setMejorNota(double mejorNota) { this.mejorNota = mejorNota; }

    public int getRachaActual() { return rachaActual; }
    public void setRachaActual(int rachaActual) { this.rachaActual = rachaActual; }

    public long getTiempoTotalSegundos() { return tiempoTotalSegundos; }
    public void setTiempoTotalSegundos(long tiempoTotalSegundos) { this.tiempoTotalSegundos = tiempoTotalSegundos; }

    public String getUltimaActividad() { return ultimaActividad; }
    public void setUltimaActividad(String ultimaActividad) { this.ultimaActividad = ultimaActividad; }

    public Map<String, EstadisticasModulo> getPorModulo() { return porModulo; }
    public void setPorModulo(Map<String, EstadisticasModulo> porModulo) {
        this.porModulo = porModulo != null ? porModulo : new LinkedHashMap<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EstadisticasModulo {
        private int intentos;
        private int aprobados;
        private double notaMedia;
        private double mejorNota;

        public int getIntentos() { return intentos; }
        public void setIntentos(int intentos) { this.intentos = intentos; }

        public int getAprobados() { return aprobados; }
        public void setAprobados(int aprobados) { this.aprobados = aprobados; }

        public double getNotaMedia() { return notaMedia; }
        public void setNotaMedia(double notaMedia) { this.notaMedia = notaMedia; }

        public double getMejorNota() { return mejorNota; }
        public void setMejorNota(double mejorNota) { this.mejorNota = mejorNota; }
    }
}
