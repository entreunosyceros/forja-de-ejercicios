package com.luegoestarde.forjaexamenes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PruebaLenguajeResaltado {

    @Test
    void moduloSql() {
        assertEquals("sql", LenguajeResaltado.porModulo("bd_sql"));
        assertEquals("sql", LenguajeResaltado.porModulo("bd"));
    }

    @Test
    void moduloJava() {
        assertEquals("java", LenguajeResaltado.porModulo("poo"));
    }

    @Test
    void moduloBash() {
        assertEquals("bash", LenguajeResaltado.porModulo("docker"));
        assertEquals("bash", LenguajeResaltado.porModulo("git"));
    }

    @Test
    void moduloDesconocido() {
        assertEquals("plaintext", LenguajeResaltado.porModulo("otro"));
        assertEquals("plaintext", LenguajeResaltado.porModulo(null));
    }
}
