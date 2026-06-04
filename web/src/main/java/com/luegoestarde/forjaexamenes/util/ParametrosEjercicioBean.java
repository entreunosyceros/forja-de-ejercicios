package com.luegoestarde.forjaexamenes.util;

import java.util.Map;

import org.springframework.stereotype.Component;

/** Utilidades para plantillas Thymeleaf: {@code ${@parametros.origenApuntes(...)}} */
@Component("parametros")
public class ParametrosEjercicioBean {

    @SuppressWarnings("unchecked")
    public Map<String, Object> origenApuntes(Map<String, Object> parametros) {
        if (parametros == null) {
            return null;
        }
        Object valor = parametros.get("origen");
        if (valor instanceof Map<?, ?> mapa) {
            return (Map<String, Object>) mapa;
        }
        return null;
    }
}
