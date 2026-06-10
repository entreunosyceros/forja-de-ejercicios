// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoEjercicios;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoPortable;
import com.luegoestarde.forjaexamenes.servicio.ServicioExploradorArchivos;
import com.luegoestarde.forjaexamenes.servicio.ServicioPreferenciasProfesor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profesor/banco")
public class ControladorBancoProfesor {

    private final ServicioBancoEjercicios servicioBanco;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioBancoPortable servicioBancoPortable;
    private final ServicioPreferenciasProfesor preferenciasProfesor;
    private final ServicioExploradorArchivos exploradorArchivos;

    public ControladorBancoProfesor(
            ServicioBancoEjercicios servicioBanco,
            ServicioAccesoProfesor accesoProfesor,
            ServicioBancoPortable servicioBancoPortable,
            ServicioPreferenciasProfesor preferenciasProfesor,
            ServicioExploradorArchivos exploradorArchivos) {
        this.servicioBanco = servicioBanco;
        this.accesoProfesor = accesoProfesor;
        this.servicioBancoPortable = servicioBancoPortable;
        this.preferenciasProfesor = preferenciasProfesor;
        this.exploradorArchivos = exploradorArchivos;
    }

    @GetMapping
    public String listar(Model modelo, RedirectAttributes flash) throws Exception {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            flash.addFlashAttribute("errorPerfil", "Acceso solo para cuentas de profesor.");
            return "redirect:/perfil";
        }
        modelo.addAttribute("tituloPagina", "Revisión del banco");
        modelo.addAttribute("pendientes", servicioBanco.listarPendientes());
        modelo.addAttribute("geminiGuardarPendientes", preferenciasProfesor.isGeminiGuardarPendientes());
        modelo.addAttribute("totalBancoAprobados", servicioBancoPortable.contarAprobados());
        modelo.addAttribute("exploradorDisponible", exploradorArchivos.disponible());
        return "profesor-banco";
    }

    @PostMapping("/abrir-carpeta")
    public String abrirCarpeta(
            @RequestParam(defaultValue = "aprobados") String cual,
            RedirectAttributes flash) {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            flash.addFlashAttribute("errorPerfil", "Acceso solo para cuentas de profesor.");
            return "redirect:/perfil";
        }
        java.nio.file.Path carpeta = "pendientes".equals(cual)
                ? servicioBanco.carpetaPendientes()
                : servicioBanco.carpetaAprobados();
        try {
            String mensaje = exploradorArchivos.abrirCarpeta(carpeta);
            flash.addFlashAttribute("mensajePerfilOk", mensaje);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
        }
        return "redirect:/profesor/banco";
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
