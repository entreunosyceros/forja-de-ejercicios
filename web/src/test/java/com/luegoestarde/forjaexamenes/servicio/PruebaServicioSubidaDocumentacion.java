package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class PruebaServicioSubidaDocumentacion {

    @TempDir
    Path tempDir;

    private ServicioSubidaDocumentacion servicio;

    @BeforeEach
    void preparar() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDocumentacion(tempDir.resolve("documentacion").toString());
        props.setSubidaPdfMaxMb(5);
        servicio = new ServicioSubidaDocumentacion(props);
    }

    @Test
    void guardaPdfEnTemaExistente() throws Exception {
        Files.createDirectories(tempDir.resolve("documentacion/docker"));
        var pdf = new MockMultipartFile(
                "archivos",
                "apuntes.pdf",
                "application/pdf",
                "%PDF-1.4 test".getBytes());

        var resultado = servicio.guardarPdfs("docker", null, null, new MockMultipartFile[] {pdf});

        assertEquals(1, resultado.guardados());
        assertTrue(Files.exists(tempDir.resolve("documentacion/docker/apuntes.pdf")));
    }

    @Test
    void creaTemaNuevoYSubcarpeta() throws Exception {
        var pdf = new MockMultipartFile(
                "archivos",
                "intro.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes());

        var resultado = servicio.guardarPdfs("__nuevo__", "Ansible", "unidad1", new MockMultipartFile[] {pdf});

        assertEquals("ansible/unidad1", resultado.carpetaDestino());
        assertTrue(Files.exists(tempDir.resolve("documentacion/ansible/unidad1/intro.pdf")));
    }

    @Test
    void rechazaNoPdf() {
        var fichero = new MockMultipartFile("archivos", "texto.txt", "text/plain", "hola".getBytes());
        assertThrows(IllegalArgumentException.class, () ->
                servicio.guardarPdfs("docker", null, null, new MockMultipartFile[] {fichero}));
    }
}
