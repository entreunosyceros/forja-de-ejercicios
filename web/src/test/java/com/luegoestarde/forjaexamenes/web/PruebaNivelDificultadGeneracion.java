// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.luegoestarde.forjaexamenes.servicio.ServicioCuentasUsuarios;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica que el nivel de dificultad fijado en el perfil se respeta al generar
 * ejercicios, tanto para alumnos como para profesores (que también practican).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PruebaNivelDificultadGeneracion {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioCuentasUsuarios cuentasUsuarios;

    @AfterEach
    void restaurarNiveles() throws Exception {
        cuentasUsuarios.actualizarNivelGemini("alumno", 2);
        cuentasUsuarios.actualizarNivelGemini("profesor", 2);
    }

    @Test
    void alumnoRespetaNivelDelPerfil() throws Exception {
        comprobarNivel("alumno", "practica", 1);
    }

    @Test
    void profesorRespetaNivelDelPerfil() throws Exception {
        comprobarNivel("profesor", "profesor", 3);
    }

    private void comprobarNivel(String usuario, String clave, int nivel) throws Exception {
        MockHttpSession sesion = sesion(usuario, clave);

        mockMvc.perform(post("/perfil/nivel-gemini").session(sesion).with(csrf())
                        .param("nivelGemini", String.valueOf(nivel)))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/ejercicio/nuevo").param("modulo", "bd").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nivel " + nivel)));
    }

    private MockHttpSession sesion(String usuario, String clave) throws Exception {
        return (MockHttpSession) mockMvc.perform(
                        post("/login").with(csrf())
                                .param("username", usuario)
                                .param("password", clave))
                .andReturn()
                .getRequest()
                .getSession();
    }
}
