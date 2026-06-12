// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PruebaControladorEjercicioPdfYEditor {

    private static final Pattern ID_EJERCICIO =
            Pattern.compile("data-ejercicio-id=\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paginaEjercicioIncluyeAssetsDeResaltado() throws Exception {
        MockHttpSession sesion = sesion("alumno", "practica");
        mockMvc.perform(get("/ejercicio/nuevo").param("modulo", "poo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("editor-codigo.js?v=3")))
                .andExpect(content().string(containsString("highlight.min.js")))
                .andExpect(content().string(containsString("highlight.github.min.css")))
                .andExpect(content().string(containsString("id=\"textarea-respuesta\"")));
    }

    @Test
    void alumnoNoVeBotonPdfAntesDeCorregir() throws Exception {
        MockHttpSession sesion = sesion("alumno", "practica");
        mockMvc.perform(get("/ejercicio/nuevo").param("modulo", "poo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("PDF (profesor)"))))
                .andExpect(content().string(not(containsString("Descargar PDF"))));
    }

    @Test
    void profesorVePdfEnPantallaEjercicio() throws Exception {
        MockHttpSession sesion = sesion("profesor", "profesor");
        mockMvc.perform(get("/ejercicio/nuevo").param("modulo", "poo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PDF (profesor)")));
    }

    @Test
    void alumnoNoPuedeDescargarPdfSinCorregir() throws Exception {
        MockHttpSession sesion = sesion("alumno", "practica");
        String id = idDesdeNuevoEjercicio(sesion, "poo");
        mockMvc.perform(get("/ejercicio/" + id + "/pdf").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    void profesorPuedeDescargarPdfSinCorregir() throws Exception {
        MockHttpSession sesion = sesion("profesor", "profesor");
        String id = idDesdeNuevoEjercicio(sesion, "poo");
        mockMvc.perform(get("/ejercicio/" + id + "/pdf").session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("pdf")));
    }

    @Test
    void profesorPuedeGuardarSolucionReferenciaYEnBanco() throws Exception {
        MockHttpSession sesion = sesion("profesor", "profesor");
        String id = idDesdeNuevoEjercicio(sesion, "poo");
        mockMvc.perform(post("/ejercicio/" + id + "/solucion-referencia").session(sesion).with(csrf())
                        .param("solucionReferencia", "class Test { public static void main(String[] a) {} }")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value("true"));
        mockMvc.perform(post("/ejercicio/" + id + "/guardar-banco").session(sesion).with(csrf())
                        .param("nombreArchivo", "poo-prueba-banco")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.nombreArchivo").exists());
    }

    @Test
    void solucionReferenciaSinCsrfDevuelve403() throws Exception {
        MockHttpSession sesion = sesion("profesor", "profesor");
        String id = idDesdeNuevoEjercicio(sesion, "poo");
        mockMvc.perform(post("/ejercicio/" + id + "/solucion-referencia").session(sesion)
                        .param("solucionReferencia", "class Test {}")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isForbidden());
    }

    @Test
    void alumnoPuedeDescargarPdfTrasCorregir() throws Exception {
        MockHttpSession sesion = sesion("alumno", "practica");
        String id = idDesdeNuevoEjercicio(sesion, "poo");
        mockMvc.perform(post("/ejercicio/" + id + "/evaluar").session(sesion).with(csrf())
                        .param("respuesta", "class Hola { public static void main(String[] args) {} }")
                        .param("tiempoSegundos", "10"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/ejercicio/" + id + "/pdf").session(sesion))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("pdf")));
    }

    private String idDesdeNuevoEjercicio(MockHttpSession sesion, String modulo) throws Exception {
        MvcResult resultado = mockMvc.perform(
                        get("/ejercicio/nuevo").param("modulo", modulo).session(sesion))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = ID_EJERCICIO.matcher(resultado.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("No se encontró data-ejercicio-id en la página del ejercicio");
        }
        return matcher.group(1);
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
