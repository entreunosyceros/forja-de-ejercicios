package com.luegoestarde.forjaexamenes.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HistorialUsuario {

    private String login = "";
    private String nombreVisible = "";
    private List<IntentoHistorial> intentos = new ArrayList<>();

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getNombreVisible() { return nombreVisible; }
    public void setNombreVisible(String nombreVisible) { this.nombreVisible = nombreVisible; }

    public List<IntentoHistorial> getIntentos() { return intentos; }
    public void setIntentos(List<IntentoHistorial> intentos) {
        this.intentos = intentos != null ? intentos : new ArrayList<>();
    }
}
