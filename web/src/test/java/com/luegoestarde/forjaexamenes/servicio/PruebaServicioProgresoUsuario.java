// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioProgresoUsuario {

    @TempDir
    Path tempDir;

    private ServicioProgresoUsuario servicioProgreso;
    private ServicioEstadisticasUsuario servicioEstadisticas;

    @BeforeEach
    void preparar() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.resolve("datos").toString());
        servicioEstadisticas = new ServicioEstadisticasUsuario(props);
        servicioProgreso = new ServicioProgresoUsuario(servicioEstadisticas);
    }

    @Test
    void snapshotReflejaEstadisticasDelServidor() throws Exception {
        servicioEstadisticas.registrar("alumno", "poo", 8.0, true, 30L, "Herencia", "ej-1");
        Map<String, Object> snap = servicioProgreso.snapshot("alumno");
        assertEquals("servidor", snap.get("fuente"));
        @SuppressWarnings("unchecked")
        var historial = (java.util.List<Map<String, Object>>) snap.get("historial");
        assertEquals(1, historial.size());
        assertEquals("poo", historial.get(0).get("modulo"));
        @SuppressWarnings("unchecked")
        var stats = (Map<String, Object>) snap.get("stats");
        assertEquals(1, stats.get("totalAprobados"));
        assertTrue(((Map<?, ?>) stats.get("porModulo")).containsKey("poo"));
    }

    @Test
    void snapshotVacioSinDatos() {
        Map<String, Object> snap = servicioProgreso.desdeEstadisticas(new EstadisticasUsuario());
        @SuppressWarnings("unchecked")
        var historial = (java.util.List<?>) snap.get("historial");
        assertTrue(historial.isEmpty());
    }
}
