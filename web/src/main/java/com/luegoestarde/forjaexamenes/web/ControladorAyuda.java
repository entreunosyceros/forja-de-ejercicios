package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioDocumentacion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorAyuda {

    private final ServicioDocumentacion servicioDocumentacion;

    public ControladorAyuda(ServicioDocumentacion servicioDocumentacion) {
        this.servicioDocumentacion = servicioDocumentacion;
    }

    @GetMapping("/como-funciona")
    public String comoFunciona(Model modelo) {
        modelo.addAttribute("tituloPagina", "Cómo funciona");
        modelo.addAttribute("geminiConfigurado", servicioDocumentacion.geminiConfigurado());
        return "como-funciona";
    }
}
