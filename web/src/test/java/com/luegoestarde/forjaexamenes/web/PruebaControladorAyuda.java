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
                .andExpect(view().name("como-funciona"))
                .andExpect(content().string(containsString("Entrar")))
                .andExpect(content().string(containsString("Ir al login")));
    }

    @Test
    void comoFuncionaConSesionMuestraInicio() throws Exception {
        MockHttpSession sesion = sesionAlumno();
        mockMvc.perform(get("/como-funciona").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inicio")))
                .andExpect(content().string(containsString("Ir al inicio")))
                .andExpect(content().string(not(containsString("Ir al login"))));
    }

    @Test
    void comoFuncionaProfesorMuestraInicioNoLogin() throws Exception {
        MockHttpSession sesion = sesionProfesor();
        mockMvc.perform(get("/como-funciona").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inicio")))
                .andExpect(content().string(not(containsString(">Entrar<"))));
    }

    private MockHttpSession sesionAlumno() throws Exception {
        return (MockHttpSession) mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/login")
                                .with(csrf())
                                .param("username", "alumno")
                                .param("password", "practica"))
                .andReturn()
                .getRequest()
                .getSession();
    }

    private MockHttpSession sesionProfesor() throws Exception {
        return (MockHttpSession) mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/login")
                                .with(csrf())
                                .param("username", "profesor")
                                .param("password", "profesor"))
                .andReturn()
                .getRequest()
                .getSession();
    }
}
