// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ManejadorExcepcionesGlobal {

    @ExceptionHandler(Exception.class)
    public String manejar(Exception excepcion, HttpServletRequest peticion, Model modelo) {
        modelo.addAttribute("error", excepcion.getMessage());
        // Para peticiones GET (p. ej. generar ejercicio), ofrecer "Reintentar" con la
        // misma URL. Útil cuando Gemini devuelve un 503 temporal: el usuario reintenta
        // sin tener que volver a la portada y reconstruir la petición.
        if ("GET".equalsIgnoreCase(peticion.getMethod())) {
            String uri = peticion.getRequestURI();
            String query = peticion.getQueryString();
            String urlReintento = (query != null && !query.isBlank()) ? uri + "?" + query : uri;
            modelo.addAttribute("urlReintento", urlReintento);
        }
        return "error";
    }
}
