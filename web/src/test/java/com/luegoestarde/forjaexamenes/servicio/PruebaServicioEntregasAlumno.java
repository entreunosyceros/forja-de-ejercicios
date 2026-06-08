// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno.TipoImportacion;
import com.luegoestarde.forjaexamenes.servicio.ConflictoImportacionEntregaException;

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
        ServicioHistorialIntentos servicioHistorial = new ServicioHistorialIntentos(props);
        servicioEntregas = new ServicioEntregasAlumno(props, servicioEstadisticas, servicioHistorial);
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

        var resultado = servicioEntregas.importar("profesor", json, "María López");
        assertEquals(TipoImportacion.NUEVA, resultado.tipo());
        EntregaAlumno importada = resultado.entrega();
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
        EntregaAlumno importada = servicioEntregas.importar("profesor", json, "Pedro").entrega();
        assertEquals(3, importada.getEstadisticasServidor().getTotalIntentos());
        assertEquals("Pedro", importada.getNombreEtiqueta());
    }

    @Test
    void importarSobrescribeEntregaMismoLogin() throws Exception {
        EntregaAlumno original = servicioEntregas.construirExportacion("alumno", "Alumno Demo");
        byte[] json = mapeador.writeValueAsBytes(original);

        var primera = servicioEntregas.importar("profesor", json, "Primera");
        String idInicial = primera.entrega().getIdImportacion();

        servicioEstadisticas.registrar("alumno", "poo", 9.0, true, 30L, "Clases", "ex2");
        byte[] json2 = mapeador.writeValueAsBytes(servicioEntregas.construirExportacion("alumno", "Alumno Demo"));

        assertThrows(ConflictoImportacionEntregaException.class,
                () -> servicioEntregas.importar("profesor", json2, "Segunda", false));

        var segunda = servicioEntregas.importar("profesor", json2, "Segunda", true);
        assertEquals(TipoImportacion.ACTUALIZADA, segunda.tipo());
        assertEquals(idInicial, segunda.entrega().getIdImportacion());
        assertEquals("Segunda", segunda.entrega().getNombreEtiqueta());
        assertEquals(2, segunda.entrega().getEstadisticasServidor().getTotalIntentos());
        assertEquals(1, servicioEntregas.listarImportadas("profesor").size());
    }
}
