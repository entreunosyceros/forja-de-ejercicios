package com.luegoestarde.forjaexamenes.util;

import org.springframework.stereotype.Component;

/** Expone {@link TextoPlano} a plantillas Thymeleaf: {@code ${@texto.plano(...)}} */
@Component("texto")
public class TextoPlanoBean {

    public String plano(String texto) {
        return TextoPlano.sinMarkdown(texto);
    }
}
