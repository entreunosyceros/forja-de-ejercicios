// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import com.luegoestarde.forjaexamenes.servicio.ServicioProgresoUsuario;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estadisticas")
public class ControladorEstadisticas {

    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioProgresoUsuario servicioProgreso;
    private final ServicioMetadatosEjercicio metadatosEjercicio;

    public ControladorEstadisticas(
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioProgresoUsuario servicioProgreso,
            ServicioMetadatosEjercicio metadatosEjercicio) {
        this.servicioEstadisticas = servicioEstadisticas;
        this.servicioProgreso = servicioProgreso;
        this.metadatosEjercicio = metadatosEjercicio;
    }

    /** Snapshot de progreso (fuente de verdad del servidor) para sincronizar el navegador. */
    @GetMapping(value = "/progreso.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> progresoJson() {
        String login = metadatosEjercicio.loginActual();
        if (login == null || login.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(servicioProgreso.snapshot(login));
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
                        "Estadísticas eliminadas. El panel de progreso (historial, ranking y medallas) queda vacío al recargar.");
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
