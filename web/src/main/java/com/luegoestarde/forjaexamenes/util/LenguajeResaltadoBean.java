package com.luegoestarde.forjaexamenes.util;

import org.springframework.stereotype.Component;

@Component("lenguajeCodigo")
public class LenguajeResaltadoBean {

    public String porModulo(String modulo) {
        return LenguajeResaltado.porModulo(modulo);
    }
}
