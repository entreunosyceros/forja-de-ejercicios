package com.luegoestarde.forjaexamenes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PruebaNombresBanco {

    @Test
    void sanitizaTituloConAcentos() {
        assertEquals("docker-nginx-puerto-8080", NombresBanco.sanitizarNombreArchivo("Docker: nginx puerto 8080"));
    }

    @Test
    void priorizaNombreSolicitado() {
        assertEquals(
                "mi-ejercicio-voluntades",
                NombresBanco.resolverNombreArchivo("Mi ejercicio voluntades", "Título ignorado", "abc123"));
    }

    @Test
    void usaTituloSiFaltaNombre() {
        assertEquals("sql-select-basico", NombresBanco.resolverNombreArchivo(null, "SQL SELECT básico", "x"));
    }
}
