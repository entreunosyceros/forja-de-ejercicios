// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioApuntesPdf {

    @TempDir
    Path tempDir;

    @Test
    void resuelvePdfDentroDeDocumentacion() throws Exception {
        Path doc = tempDir.resolve("documentacion/docker");
        Files.createDirectories(doc);
        Path pdf = doc.resolve("apuntes.pdf");
        Files.writeString(pdf, "%PDF-1.4 mock");

        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setRaiz(tempDir.toString());
        props.setDirectorioDocumentacion(tempDir.resolve("documentacion").toString());

        ServicioApuntesPdf servicio = new ServicioApuntesPdf(props);
        Path resuelto = servicio.resolverPdf("documentacion/docker/apuntes.pdf");
        assertTrue(resuelto.toString().endsWith("apuntes.pdf"));
    }

    @Test
    void rechazaSalidaDeDocumentacion() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setRaiz(tempDir.toString());
        props.setDirectorioDocumentacion(tempDir.resolve("documentacion").toString());

        ServicioApuntesPdf servicio = new ServicioApuntesPdf(props);
        assertThrows(IllegalArgumentException.class,
                () -> servicio.resolverPdf("../evaluador.py"));
    }
}
