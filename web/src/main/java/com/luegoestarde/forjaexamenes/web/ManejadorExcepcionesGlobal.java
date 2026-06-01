package com.luegoestarde.forjaexamenes.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ManejadorExcepcionesGlobal {

    @ExceptionHandler(Exception.class)
    public String manejar(Exception excepcion, Model modelo) {
        modelo.addAttribute("error", excepcion.getMessage());
        return "error";
    }
}
