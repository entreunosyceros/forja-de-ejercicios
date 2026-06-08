// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forjaexamenes.login")
public class PropiedadesLogin {

    /**
     * Usuarios en formato {@code usuario:contraseña,otro:clave}.
     * Ejemplo: {@code alumno:practica,maria:maria123}
     */
    private String usuarios = "alumno:practica,demo:demo,profesor:profesor";

    /** Logins con rol profesor (además del campo {@code rol} en usuarios.json). */
    private String profesores = "profesor";

    public String getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(String usuarios) {
        this.usuarios = usuarios;
    }

    public String getProfesores() {
        return profesores;
    }

    public void setProfesores(String profesores) {
        this.profesores = profesores;
    }
}
