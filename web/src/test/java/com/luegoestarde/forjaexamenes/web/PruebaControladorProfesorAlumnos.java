// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno;
import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PruebaControladorProfesorAlumnos {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioEntregasAlumno servicioEntregas;

    @Autowired
    private ServicioEstadisticasUsuario servicioEstadisticas;

    private final ObjectMapper mapeador = new ObjectMapper();

    @Test
    void listadoTrasImportarEntrega() throws Exception {
        servicioEstadisticas.registrar("alumno", "bd", 7.5, true, 45L, "Índices", "ex1");
        servicioEstadisticas.registrar("alumno", "docker", 6.0, true, 30L, "Logs", "ex2");

        EntregaAlumno original = servicioEntregas.construirExportacion("alumno", "Ana García");
        byte[] json = mapeador.writeValueAsBytes(original);

        MockHttpSession sesion = sesionProfesor();

        mockMvc.perform(multipart("/profesor/alumnos/importar")
                        .file(new MockMultipartFile("archivo", "entrega.json", "application/json", json))
                        .param("nombreEtiqueta", "Ana García — 1º DAM")
                        .with(csrf())
                        .session(sesion))
                .andExpect(status().is3xxRedirection());

        var comparativa = servicioEntregas.construirComparativa("profesor");
        org.junit.jupiter.api.Assertions.assertFalse(comparativa.modulos().isEmpty());

        mockMvc.perform(get("/profesor/alumnos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(view().name("profesor-alumnos-lista"))
                .andExpect(content().string(containsString("Nota media por módulo")))
                .andExpect(content().string(containsString("tabla-modulos-clase")));
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
