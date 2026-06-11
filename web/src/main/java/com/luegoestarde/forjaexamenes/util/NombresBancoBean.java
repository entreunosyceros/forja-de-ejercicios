package com.luegoestarde.forjaexamenes.util;

import org.springframework.stereotype.Component;

@Component("nombresBanco")
public class NombresBancoBean {

    public String sugerirNombreArchivo(String titulo, String idSesion) {
        return NombresBanco.resolverNombreArchivo(null, titulo, idSesion);
    }
}
