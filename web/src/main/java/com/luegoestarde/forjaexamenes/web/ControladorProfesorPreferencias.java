// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioPreferenciasProfesor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profesor/preferencias")
public class ControladorProfesorPreferencias {

    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioPreferenciasProfesor preferenciasProfesor;

    public ControladorProfesorPreferencias(
            ServicioAccesoProfesor accesoProfesor,
            ServicioPreferenciasProfesor preferenciasProfesor) {
        this.accesoProfesor = accesoProfesor;
        this.preferenciasProfesor = preferenciasProfesor;
    }

    @PostMapping("/gemini-guardar-pendientes")
    public String guardarGeminiPendientes(
            @RequestParam(defaultValue = "false") boolean activo,
            @RequestParam(defaultValue = "/profesor/revisar") String volver,
            RedirectAttributes flash) throws Exception {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            flash.addFlashAttribute("errorPerfil", "Acceso solo para cuentas de profesor.");
            return "redirect:/perfil";
        }
        preferenciasProfesor.setGeminiGuardarPendientes(activo);
        flash.addFlashAttribute(
                "mensajePerfilOk",
                activo
                        ? "Cola de revisión activada: los ejercicios docs_* se guardarán en banco/pendientes/."
                        : "Cola de revisión desactivada: los nuevos ejercicios docs_* no se acumularán en pendientes.");
        String destino = volver.startsWith("/profesor") ? volver : "/profesor/revisar";
        return "redirect:" + destino;
    }
}
