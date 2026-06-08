package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntregaAlumno {

    public static final String FORMATO = "forja-entrega-alumno";
    public static final int VERSION = 1;

    private String formato = FORMATO;
    private int version = VERSION;
    private String generado = "";
    private AlumnoInfo alumno = new AlumnoInfo();
    private EstadisticasUsuario estadisticasServidor = new EstadisticasUsuario();
    private JsonNode progresoLocal;
    private String idImportacion = "";
    private String importadoPor = "";
    private String importadoEn = "";
    /** Nombre asignado por el profesor al importar (para distinguir alumnos). */
    private String nombreEtiqueta = "";

    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getGenerado() { return generado; }
    public void setGenerado(String generado) { this.generado = generado; }

    public AlumnoInfo getAlumno() { return alumno; }
    public void setAlumno(AlumnoInfo alumno) { this.alumno = alumno != null ? alumno : new AlumnoInfo(); }

    public EstadisticasUsuario getEstadisticasServidor() { return estadisticasServidor; }
    public void setEstadisticasServidor(EstadisticasUsuario estadisticasServidor) {
        this.estadisticasServidor = estadisticasServidor != null ? estadisticasServidor : new EstadisticasUsuario();
    }

    public JsonNode getProgresoLocal() { return progresoLocal; }
    public void setProgresoLocal(JsonNode progresoLocal) { this.progresoLocal = progresoLocal; }

    public String getIdImportacion() { return idImportacion; }
    public void setIdImportacion(String idImportacion) { this.idImportacion = idImportacion; }

    public String getImportadoPor() { return importadoPor; }
    public void setImportadoPor(String importadoPor) { this.importadoPor = importadoPor; }

    public String getImportadoEn() { return importadoEn; }
    public void setImportadoEn(String importadoEn) { this.importadoEn = importadoEn; }

    public String getNombreEtiqueta() { return nombreEtiqueta; }
    public void setNombreEtiqueta(String nombreEtiqueta) { this.nombreEtiqueta = nombreEtiqueta; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlumnoInfo {
        private String login = "";
        private String nombreVisible = "";

        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }

        public String getNombreVisible() { return nombreVisible; }
        public void setNombreVisible(String nombreVisible) { this.nombreVisible = nombreVisible; }
    }
}
