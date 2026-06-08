package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UsuarioAlmacenado {

    private String passwordHash = "";
    private String nombreVisible = "";
    /** {@code alumno} o {@code profesor} */
    private String rol = "alumno";
    /** Nivel 1–3 para ejercicios generados con IA desde PDF (docs_*). */
    private Integer nivelGemini = 2;

    public UsuarioAlmacenado() {}

    public UsuarioAlmacenado(String passwordHash, String nombreVisible) {
        this(passwordHash, nombreVisible, "alumno");
    }

    public UsuarioAlmacenado(String passwordHash, String nombreVisible, String rol) {
        this.passwordHash = passwordHash;
        this.nombreVisible = nombreVisible;
        this.rol = rol == null || rol.isBlank() ? "alumno" : rol.strip().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public void setNombreVisible(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean esProfesor() {
        return "profesor".equalsIgnoreCase(rol);
    }

    public Integer getNivelGemini() {
        return nivelGemini;
    }

    public void setNivelGemini(Integer nivelGemini) {
        this.nivelGemini = nivelGemini;
    }
}
