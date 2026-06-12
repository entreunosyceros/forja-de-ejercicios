// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PruebaTextoPlano {

    @Test
    void quitaMarkdownDeSolucion() {
        String entrada = """
                **Solución**
                ```bash
                docker compose up -d
                docker ps
                ```
                """;
        String salida = TextoPlano.sinMarkdown(entrada);
        assertTrue(salida.contains("docker compose up -d"));
        assertTrue(salida.contains("docker ps"));
        assertFalse(salida.contains("```"));
        assertFalse(salida.contains("**"));
    }

    @Test
    void quitaDecoracionNivelDuplicada() {
        String entrada = """
                [Nivel intermedio]

                [Nivel intermedio]

                Enunciado real del ejercicio.
                """;
        String salida = TextoPlano.quitarDecoracionNivel(entrada);
        assertFalse(salida.contains("[Nivel intermedio]"));
        assertTrue(salida.contains("Enunciado real del ejercicio."));
    }
}
