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
}
