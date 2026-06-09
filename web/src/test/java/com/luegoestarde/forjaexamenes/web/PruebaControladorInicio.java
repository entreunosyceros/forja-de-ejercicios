// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PruebaControladorInicio {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "alumno")
    void indiceDevuelveOk() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicio"));
    }

    @Test
    void alumnoVeEntregaParaProfesor() throws Exception {
        MockHttpSession sesion = sesion("alumno", "practica");
        mockMvc.perform(get("/").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Entrega para el profesor")))
                .andExpect(content().string(containsString("Descargar entrega para el profesor")));
    }

    @Test
    void profesorNoVeEntregaParaProfesor() throws Exception {
        MockHttpSession sesion = sesion("profesor", "profesor");
        mockMvc.perform(get("/").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Entrega para el profesor"))))
                .andExpect(content().string(not(containsString("Descargar entrega para el profesor"))));
    }

    private MockHttpSession sesion(String usuario, String clave) throws Exception {
        return (MockHttpSession) mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/login")
                                .with(csrf())
                                .param("username", usuario)
                                .param("password", clave))
                .andReturn()
                .getRequest()
                .getSession();
    }
}
