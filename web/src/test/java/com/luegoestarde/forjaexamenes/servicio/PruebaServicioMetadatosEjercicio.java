package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
class PruebaServicioMetadatosEjercicio {

    @Autowired
    private ServicioMetadatosEjercicio metadatos;

    @Test
    @WithMockUser(username = "maria")
    void marcaEscenarioConUsuarioYFecha() {
        Escenario escenario = new Escenario();
        escenario.setId("test-1");
        metadatos.marcarEscenario(escenario, "maria", "María García");
        assertEquals("maria", escenario.getUsuarioAcceso());
        assertEquals("María García", escenario.getUsuario());
        assertNotNull(escenario.getFechaHora());
    }

    @Test
    void enriqueceResultadoConMetadatos() {
        Escenario escenario = new Escenario();
        escenario.setFechaHora("01/06/2026 10:00");
        ResultadoEvaluacion resultado = new ResultadoEvaluacion();
        metadatos.enriquecerResultado(resultado, escenario, "pedro", 125L);
        assertEquals("pedro", resultado.getUsuario());
        assertEquals("01/06/2026 10:00", resultado.getFechaHoraInicio());
        assertNotNull(resultado.getFechaHoraEvaluacion());
        assertEquals(125L, resultado.getTiempoSegundos());
    }
}
