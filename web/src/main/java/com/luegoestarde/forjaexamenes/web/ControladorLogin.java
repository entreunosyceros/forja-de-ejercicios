// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorLogin {

    @GetMapping("/login")
    public String login(Model modelo) {
        modelo.addAttribute("tituloPagina", "Entrar");
        return "login";
    }
}
