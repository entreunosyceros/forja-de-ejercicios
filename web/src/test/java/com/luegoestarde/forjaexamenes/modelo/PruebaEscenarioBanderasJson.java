// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PruebaEscenarioBanderasJson {

    private final ObjectMapper mapeador = new ObjectMapper();

    @Test
    void leeFlagsLegacyComoBanderasYSoloEscribeBanderas() throws Exception {
        String json = """
                {
                  "titulo": "t",
                  "enunciado": "e",
                  "solucion_referencia": "s",
                  "criterios": [
                    {"tipo": "regex", "patron": "x", "peso": 1, "flags": "i"}
                  ]
                }
                """;
        Escenario esc = mapeador.readValue(json, Escenario.class);
        assertEquals("i", esc.getCriterios().get(0).getBanderas());

        String out = mapeador.writeValueAsString(esc);
        assertFalse(out.contains("\"flags\""), out);
        org.junit.jupiter.api.Assertions.assertTrue(out.contains("\"banderas\":\"i\""), out);
    }
}
