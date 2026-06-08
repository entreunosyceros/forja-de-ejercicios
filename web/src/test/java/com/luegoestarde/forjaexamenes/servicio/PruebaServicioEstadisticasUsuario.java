// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioEstadisticasUsuario {

    @TempDir
    Path tempDir;

    private ServicioEstadisticasUsuario servicio;

    @BeforeEach
    void preparar() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.toString());
        servicio = new ServicioEstadisticasUsuario(props);
    }

    @Test
    void registrarAcumulaTotalesYModulo() throws Exception {
        servicio.registrar("alumno", "docker", 8.5, true, 120L);
        servicio.registrar("alumno", "docker", 5.0, false, 60L);

        EstadisticasUsuario stats = servicio.obtener("alumno");
        assertEquals(2, stats.getTotalIntentos());
        assertEquals(1, stats.getTotalAprobados());
        assertEquals(1, stats.getTotalSuspensos());
        assertEquals(6.8, stats.getNotaMedia(), 0.01);
        assertEquals(8.5, stats.getMejorNota(), 0.01);
        assertEquals(0, stats.getRachaActual());
        assertEquals(180L, stats.getTiempoTotalSegundos());
        assertTrue(stats.getUltimaActividad().length() > 5);

        var docker = stats.getPorModulo().get("docker");
        assertEquals(2, docker.getIntentos());
        assertEquals(1, docker.getAprobados());
        assertEquals(6.8, docker.getNotaMedia(), 0.01);
        assertEquals(8.5, docker.getMejorNota(), 0.01);

        Path fichero = tempDir.resolve("estadisticas").resolve("alumno.json");
        assertTrue(Files.isRegularFile(fichero));
    }

    @Test
    void registrarGuardaUltimosIntentos() throws Exception {
        servicio.registrar("alumno", "docs_forense", 8.0, true, 90L, "Ejercicio forense", "abc123");
        var intentos = servicio.obtener("alumno").getUltimosIntentos();
        assertEquals(1, intentos.size());
        assertEquals("docs_forense", intentos.get(0).getModulo());
        assertEquals("Ejercicio forense", intentos.get(0).getTitulo());
        assertEquals("abc123", intentos.get(0).getEjercicioId());
    }

    @Test
    void limpiarEliminaFicheroYDejaEstadisticasVacias() throws Exception {
        servicio.registrar("alumno", "poo", 7.0, true, 30L);
        assertTrue(servicio.limpiar("alumno"));
        assertEquals(0, servicio.obtener("alumno").getTotalIntentos());
        // Segunda limpieza: ya no hay fichero que borrar, así que devuelve false (idempotente).
        assertFalse(servicio.limpiar("alumno"));
    }

    @Test
    void formatearTiempoMuestraMinutosYHoras() {
        assertEquals("2:05", ServicioEstadisticasUsuario.formatearTiempo(125));
        assertEquals("1:01:05", ServicioEstadisticasUsuario.formatearTiempo(3665));
        assertEquals("0:00", ServicioEstadisticasUsuario.formatearTiempo(0));
    }
}
