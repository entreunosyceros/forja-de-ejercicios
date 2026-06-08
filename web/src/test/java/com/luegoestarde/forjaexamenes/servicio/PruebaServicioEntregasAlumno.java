package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioEntregasAlumno {

    @TempDir
    Path tempDir;

    private ServicioEntregasAlumno servicioEntregas;
    private ServicioEstadisticasUsuario servicioEstadisticas;
    private final ObjectMapper mapeador = new ObjectMapper();

    @BeforeEach
    void preparar() throws Exception {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.toString());
        servicioEstadisticas = new ServicioEstadisticasUsuario(props);
        servicioEntregas = new ServicioEntregasAlumno(props, servicioEstadisticas);
        servicioEstadisticas.registrar("alumno", "docker", 8.0, true, 60L, "Contenedor", "ex1");
    }

    @Test
    void construirExportacionIncluyeEstadisticas() {
        EntregaAlumno entrega = servicioEntregas.construirExportacion("alumno", "Alumno Demo");
        assertEquals(EntregaAlumno.FORMATO, entrega.getFormato());
        assertEquals("alumno", entrega.getAlumno().getLogin());
        assertEquals("Alumno Demo", entrega.getAlumno().getNombreVisible());
        assertEquals(1, entrega.getEstadisticasServidor().getTotalIntentos());
    }

    @Test
    void importarGuardaNombreEtiquetaYComparativa() throws Exception {
        EntregaAlumno original = servicioEntregas.construirExportacion("alumno", "Alumno Demo");
        byte[] json = mapeador.writeValueAsBytes(original);

        EntregaAlumno importada = servicioEntregas.importar("profesor", json, "María López");
        assertFalse(importada.getIdImportacion().isBlank());
        assertEquals("María López", importada.getNombreEtiqueta());
        assertEquals(1, importada.getEstadisticasServidor().getTotalIntentos());

        var lista = servicioEntregas.listarImportadas("profesor");
        assertEquals(1, lista.size());
        assertEquals("María López", lista.get(0).nombreEtiqueta());

        var comparativa = servicioEntregas.construirComparativa("profesor");
        assertEquals(1, comparativa.alumnos().size());
        assertEquals("María López", comparativa.alumnos().get(0).nombreEtiqueta());
        assertTrue(comparativa.modulos().contains("docker"));

        servicioEntregas.renombrar("profesor", importada.getIdImportacion(), "María L. — 1º DAM");
        EntregaAlumno renombrada = servicioEntregas.obtenerImportada("profesor", importada.getIdImportacion());
        assertEquals("María L. — 1º DAM", renombrada.getNombreEtiqueta());

        assertTrue(servicioEntregas.eliminarImportada("profesor", importada.getIdImportacion()));
        assertTrue(servicioEntregas.listarImportadas("profesor").isEmpty());
    }

    @Test
    void importarAceptaFicheroEstadisticasPlano() throws Exception {
        Path stats = tempDir.resolve("estadisticas").resolve("demo.json");
        Files.createDirectories(stats.getParent());
        Files.writeString(stats, """
                {"totalIntentos":3,"totalAprobados":2,"porModulo":{"poo":{"intentos":3}}}
                """);

        byte[] json = Files.readAllBytes(stats);
        EntregaAlumno importada = servicioEntregas.importar("profesor", json, "Pedro");
        assertEquals(3, importada.getEstadisticasServidor().getTotalIntentos());
        assertEquals("Pedro", importada.getNombreEtiqueta());
    }
}
