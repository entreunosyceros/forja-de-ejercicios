package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioCuentasUsuarios;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AsesorModelo {

    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioCuentasUsuarios cuentasUsuarios;

    public AsesorModelo(ServicioAccesoProfesor accesoProfesor,
                        ServicioMetadatosEjercicio metadatosEjercicio,
                        ServicioCuentasUsuarios cuentasUsuarios) {
        this.accesoProfesor = accesoProfesor;
        this.metadatosEjercicio = metadatosEjercicio;
        this.cuentasUsuarios = cuentasUsuarios;
    }

    @ModelAttribute("modoProfesor")
    public boolean modoProfesor() {
        return accesoProfesor.modoProfesorActivo();
    }

    @ModelAttribute("esProfesor")
    public boolean esProfesor() {
        return accesoProfesor.esProfesorActual();
    }

    @ModelAttribute("nombreUsuario")
    public String nombreUsuario() {
        return metadatosEjercicio.usuarioActual();
    }

    @ModelAttribute("loginUsuario")
    public String loginUsuario() {
        return metadatosEjercicio.loginActual();
    }

    @ModelAttribute("nivelGemini")
    public int nivelGemini() {
        String login = metadatosEjercicio.loginActual();
        if (login == null) {
            return 2;
        }
        return cuentasUsuarios.obtenerNivelGemini(login);
    }
}
