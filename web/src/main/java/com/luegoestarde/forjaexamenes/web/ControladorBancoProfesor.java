package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoEjercicios;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profesor/banco")
public class ControladorBancoProfesor {

    private final ServicioBancoEjercicios servicioBanco;
    private final PropiedadesForjaExamenes propiedades;
    private final ServicioAccesoProfesor accesoProfesor;

    public ControladorBancoProfesor(
            ServicioBancoEjercicios servicioBanco,
            PropiedadesForjaExamenes propiedades,
            ServicioAccesoProfesor accesoProfesor) {
        this.servicioBanco = servicioBanco;
        this.propiedades = propiedades;
        this.accesoProfesor = accesoProfesor;
    }

    @GetMapping
    public String listar(Model modelo, RedirectAttributes flash) throws Exception {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            flash.addFlashAttribute("errorPerfil", "Acceso solo para cuentas de profesor.");
            return "redirect:/perfil";
        }
        modelo.addAttribute("tituloPagina", "Revisión del banco");
        modelo.addAttribute("pendientes", servicioBanco.listarPendientes());
        modelo.addAttribute("geminiGuardarPendientes", propiedades.isGeminiGuardarPendientes());
        return "profesor-banco";
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable String id, RedirectAttributes flash) throws Exception {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            flash.addFlashAttribute("errorPerfil", "Acceso solo para profesor.");
            return "redirect:/perfil";
        }
        try {
            servicioBanco.aprobar(id);
            flash.addFlashAttribute("mensajePerfilOk", "Ejercicio " + id + " aprobado y publicado en el banco.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
        }
        return "redirect:/profesor/banco";
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable String id, RedirectAttributes flash) throws Exception {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            flash.addFlashAttribute("errorPerfil", "Acceso solo para profesor.");
            return "redirect:/perfil";
        }
        servicioBanco.rechazar(id);
        flash.addFlashAttribute("mensajePerfilOk", "Propuesta " + id + " rechazada.");
        return "redirect:/profesor/banco";
    }
}
