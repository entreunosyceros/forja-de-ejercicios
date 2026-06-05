package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioEntornoPractica;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/practica")
public class ControladorPractica {

    private final ServicioEntornoPractica servicioEntorno;

    public ControladorPractica(ServicioEntornoPractica servicioEntorno) {
        this.servicioEntorno = servicioEntorno;
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
}
