// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PruebaModeloGemini {

    @Test
    void corrigePuntoAntesDeFlash() {
        assertEquals("gemini-2.5-flash", ModeloGemini.normalizar("gemini-2.5.flash"));
    }

    @Test
    void mantieneNombreCorrecto() {
        assertEquals("gemini-2.5-flash", ModeloGemini.normalizar("gemini-2.5-flash"));
    }

    @Test
    void vacioUsaPorDefecto() {
        assertEquals(ModeloGemini.POR_DEFECTO, ModeloGemini.normalizar("  "));
    }
}
