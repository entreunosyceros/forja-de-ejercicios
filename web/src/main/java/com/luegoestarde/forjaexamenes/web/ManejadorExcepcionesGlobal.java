// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ManejadorExcepcionesGlobal {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorExcepcionesGlobal.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarArgumento(
            IllegalArgumentException excepcion, HttpServletRequest peticion, Model modelo) {
        return preparar(excepcion, peticion, modelo, HttpStatus.BAD_REQUEST, false);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String manejar(Exception excepcion, HttpServletRequest peticion, Model modelo) {
        return preparar(excepcion, peticion, modelo, HttpStatus.INTERNAL_SERVER_ERROR, true);
    }

    private String preparar(
            Exception excepcion,
            HttpServletRequest peticion,
            Model modelo,
            HttpStatus estado,
            boolean registrarStack) {
        String id = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        if (registrarStack) {
            LOG.error("Error {} en {} {}: {}", id, peticion.getMethod(), peticion.getRequestURI(),
                    excepcion.getMessage(), excepcion);
        } else {
            LOG.warn("Error {} en {} {}: {}", id, peticion.getMethod(), peticion.getRequestURI(),
                    excepcion.getMessage());
        }
        modelo.addAttribute("errorId", id);
        modelo.addAttribute(
                "error",
                "Error " + id + ". Díselo al profesor si el problema continúa.");
        if ("GET".equalsIgnoreCase(peticion.getMethod())) {
            String uri = peticion.getRequestURI();
            String query = peticion.getQueryString();
            String urlReintento = (query != null && !query.isBlank()) ? uri + "?" + query : uri;
            modelo.addAttribute("urlReintento", urlReintento);
        }
        modelo.addAttribute("httpStatus", estado.value());
        return "error";
    }
}
