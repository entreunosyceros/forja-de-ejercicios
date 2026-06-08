// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estadisticas")
public class ControladorEstadisticas {

    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioMetadatosEjercicio metadatosEjercicio;

    public ControladorEstadisticas(
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioMetadatosEjercicio metadatosEjercicio) {
        this.servicioEstadisticas = servicioEstadisticas;
        this.metadatosEjercicio = metadatosEjercicio;
    }

    @PostMapping("/limpiar")
    public String limpiar(RedirectAttributes flash) {
        String login = metadatosEjercicio.loginActual();
        if (login == null || login.isBlank()) {
            return "redirect:/login";
        }
        try {
            boolean eliminado = servicioEstadisticas.limpiar(login);
            if (eliminado) {
                flash.addFlashAttribute(
                        "mensajeEstadisticas",
                        "Estadísticas de uso eliminadas. Empezarás de cero en el resumen del servidor.");
            } else {
                flash.addFlashAttribute(
                        "mensajeEstadisticas",
                        "No había estadísticas guardadas en el servidor.");
            }
        } catch (Exception ex) {
            flash.addFlashAttribute(
                    "errorEstadisticas",
                    "No se pudieron eliminar las estadísticas: " + ex.getMessage());
        }
        return "redirect:/#estadisticas-uso";
    }
}
