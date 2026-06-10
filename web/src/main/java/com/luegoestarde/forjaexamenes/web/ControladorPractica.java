// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntornoPractica;
import com.luegoestarde.forjaexamenes.servicio.ServicioTerminalSistema;
import java.nio.file.Path;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/practica")
public class ControladorPractica {

    private final ServicioEntornoPractica servicioEntorno;
    private final ServicioTerminalSistema servicioTerminal;
    private final PropiedadesForjaExamenes propiedades;

    public ControladorPractica(
            ServicioEntornoPractica servicioEntorno,
            ServicioTerminalSistema servicioTerminal,
            PropiedadesForjaExamenes propiedades) {
        this.servicioEntorno = servicioEntorno;
        this.servicioTerminal = servicioTerminal;
        this.propiedades = propiedades;
    }

    @PostMapping("/limpiar")
    public String limpiar(RedirectAttributes flash) {
        try {
            var resultado = servicioEntorno.limpiar();
            flash.addFlashAttribute("mensajePractica", resultado.mensaje());
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPractica", "No se pudo limpiar: " + ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/abrir-terminal")
    public String abrirTerminal(RedirectAttributes flash) {
        try {
            Path raiz = Path.of(propiedades.getRaiz()).toAbsolutePath().normalize();
            String mensaje = servicioTerminal.abrirTerminalPracticaDocker(raiz);
            flash.addFlashAttribute("mensajePractica", mensaje);
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPractica", ex.getMessage());
        }
        return "redirect:/";
    }
}
