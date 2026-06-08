// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import com.luegoestarde.forjaexamenes.servicio.ServicioPanelProfesor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profesor")
public class ControladorProfesor {

    private final ServicioPanelProfesor servicioPanel;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioMetadatosEjercicio metadatosEjercicio;

    public ControladorProfesor(
            ServicioPanelProfesor servicioPanel,
            ServicioAccesoProfesor accesoProfesor,
            ServicioMetadatosEjercicio metadatosEjercicio) {
        this.servicioPanel = servicioPanel;
        this.accesoProfesor = accesoProfesor;
        this.metadatosEjercicio = metadatosEjercicio;
    }

    @GetMapping
    public String panel(Model modelo) throws Exception {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            return "redirect:/";
        }
        String profesor = metadatosEjercicio.loginActual();
        modelo.addAttribute("tituloPagina", "Panel del profesor");
        modelo.addAttribute("metricas", servicioPanel.calcular(profesor));
        return "profesor-panel";
    }
}
