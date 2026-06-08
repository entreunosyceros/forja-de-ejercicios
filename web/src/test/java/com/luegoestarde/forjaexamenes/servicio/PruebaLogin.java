package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PruebaLogin {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder codificador;

    @Autowired
    private ServicioCuentasUsuarios cuentasUsuarios;

    @Test
    void hashDelFicheroCoincideConPractica() {
        var detalle = cuentasUsuarios.loadUserByUsername("alumno");
        assertTrue(codificador.matches("practica", detalle.getPassword()));
    }

    @Test
    void loginAlumnoRedirigeAlInicio() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "alumno")
                        .param("password", "practica"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void cuentaProfesorTieneRolProfesor() {
        var detalle = cuentasUsuarios.loadUserByUsername("profesor");
        assertTrue(
                detalle.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_PROFESOR".equals(a.getAuthority())));
        assertTrue(codificador.matches("profesor", detalle.getPassword()));
    }

    @Test
    void loginProfesorRedirigeAlInicio() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "profesor")
                        .param("password", "profesor"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void alumnoNoAccedeZonaProfesor() throws Exception {
        MockHttpSession sesion = (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "alumno")
                        .param("password", "practica"))
                .andReturn()
                .getRequest()
                .getSession();
        // Alumno autenticado (con su sesión) accediendo a zona de profesor: 403, no 302.
        mockMvc.perform(get("/profesor/revisar").session(sesion))
                .andExpect(status().isForbidden());
    }
}
