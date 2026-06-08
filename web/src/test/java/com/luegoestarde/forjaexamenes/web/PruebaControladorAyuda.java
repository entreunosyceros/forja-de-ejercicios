// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PruebaControladorAyuda {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void comoFuncionaEsPublico() throws Exception {
        mockMvc.perform(get("/como-funciona"))
                .andExpect(status().isOk())
                .andExpect(view().name("como-funciona"));
    }
}
