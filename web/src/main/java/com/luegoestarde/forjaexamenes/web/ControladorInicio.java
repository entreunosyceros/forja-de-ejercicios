package com.luegoestarde.forjaexamenes.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorInicio {

    @GetMapping("/")
    public String inicio(Model modelo) {
        modelo.addAttribute("titulo", "luego es tarde... para estudiar");
        return "inicio";
    }
}
