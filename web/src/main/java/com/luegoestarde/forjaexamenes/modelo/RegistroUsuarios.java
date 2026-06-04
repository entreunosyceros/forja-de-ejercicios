package com.luegoestarde.forjaexamenes.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegistroUsuarios {

    private Map<String, UsuarioAlmacenado> usuarios = new LinkedHashMap<>();

    public Map<String, UsuarioAlmacenado> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(Map<String, UsuarioAlmacenado> usuarios) {
        this.usuarios = usuarios != null ? usuarios : new LinkedHashMap<>();
    }
}
