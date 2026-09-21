// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PruebaControladorEstadisticasProgreso {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "alumno")
    void progresoJsonDevuelveSnapshotServidor() throws Exception {
        mockMvc.perform(get("/estadisticas/progreso.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuente").value("servidor"))
                .andExpect(jsonPath("$.historial").isArray())
                .andExpect(jsonPath("$.stats").exists());
    }

    @Test
    @WithMockUser(username = "alumno")
    void portadaEmbebeProgresoServidor() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-progreso-servidor")))
                .andExpect(content().string(containsString("Recargar desde el servidor")));
    }
}
