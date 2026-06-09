// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import com.luegoestarde.forjaexamenes.util.FechasForja;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ServicioMetadatosEjercicio {

    private final ServicioCuentasUsuarios cuentasUsuarios;

    public ServicioMetadatosEjercicio(ServicioCuentasUsuarios cuentasUsuarios) {
        this.cuentasUsuarios = cuentasUsuarios;
    }

    public String loginActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String login = auth.getName();
        if (login == null || login.isBlank() || "anonymousUser".equals(login)) {
            return null;
        }
        return login;
    }

    /** Nombre visible en ejercicios y PDF. */
    public String nombreVisibleActual() {
        String login = loginActual();
        if (login == null) {
            return null;
        }
        return cuentasUsuarios.nombreVisible(login);
    }

    /** Alias de {@link #nombreVisibleActual()} para plantillas. */
    public String usuarioActual() {
        return nombreVisibleActual();
    }

    public String ahoraFormateado() {
        return FechasForja.ahora();
    }

    public void marcarEscenario(Escenario escenario, String login, String nombreVisible) {
        escenario.setUsuarioAcceso(login);
        escenario.setUsuario(nombreVisible);
        escenario.setFechaHora(ahoraFormateado());
    }

    public void enriquecerResultado(
            ResultadoEvaluacion resultado,
            Escenario escenario,
            String nombreVisible,
            Long tiempoSegundos) {
        resultado.setUsuario(nombreVisible);
        resultado.setFechaHoraInicio(escenario.getFechaHora());
        resultado.setFechaHoraEvaluacion(ahoraFormateado());
        if (tiempoSegundos != null) {
            resultado.setTiempoSegundos(tiempoSegundos);
        }
    }
}
