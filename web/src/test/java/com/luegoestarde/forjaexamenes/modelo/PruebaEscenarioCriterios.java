package com.luegoestarde.forjaexamenes.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

class PruebaEscenarioCriterios {

    @Test
    void conservaTerminosAlDeserializarYSerializar() throws Exception {
        String json = """
                {
                  "id": "banco-docker-01",
                  "modulo": "banco_docker",
                  "titulo": "nginx",
                  "enunciado": "Levanta nginx",
                  "criterios": [
                    {
                      "tipo": "contiene_alguno",
                      "terminos": ["-p 8080:80", "-p8080:80"],
                      "peso": 3,
                      "esperado": "Mapear puerto 8080:80"
                    }
                  ],
                  "solucion_referencia": "docker run -d -p 8080:80 nginx"
                }
                """;

        ObjectMapper mapeador = new ObjectMapper();
        mapeador.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        Escenario escenario = mapeador.readValue(json, Escenario.class);
        assertNotNull(escenario.getCriterios());
        assertEquals(2, escenario.getCriterios().getFirst().getTerminos().size());
        assertEquals("-p 8080:80", escenario.getCriterios().getFirst().getTerminos().getFirst());

        String otraVez = mapeador.writeValueAsString(escenario);
        Escenario reimportado = mapeador.readValue(otraVez, Escenario.class);
        assertEquals(
                escenario.getCriterios().getFirst().getTerminos(),
                reimportado.getCriterios().getFirst().getTerminos());
    }
}
