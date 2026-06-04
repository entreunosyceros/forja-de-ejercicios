package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PruebaMensajesErrorGenerador {

    @Test
    void extraeRuntimeErrorSinTraceback() {
        String salida = """
                Traceback (most recent call last):
                  File "generador.py", line 1
                RuntimeError: Cuota agotada para gemini-2.0-flash
                  • Usa gemini-2.5-flash
                The above exception was the direct cause
                """;
        String resumen = MensajesErrorGenerador.resumir(salida);
        assertTrue(resumen.contains("Cuota agotada"));
        assertTrue(resumen.contains("gemini-2.5-flash"));
        assertTrue(!resumen.contains("Traceback"));
    }
}
