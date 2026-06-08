// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;

class PruebaServicioBancoPortable {

    @TempDir
    Path tempDir;

    private ServicioBancoPortable servicioPortable;
    private ServicioBancoEjercicios servicioBanco;

    @BeforeEach
    void preparar() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioBanco(tempDir.resolve("banco").toString());
        ApplicationEventPublisher noop = event -> {};
        servicioBanco = new ServicioBancoEjercicios(props, noop);
        servicioPortable = new ServicioBancoPortable(servicioBanco);
    }

    @Test
    void exportarEImportarPaqueteEnOtroEquipo() throws Exception {
        Path origen = tempDir.resolve("banco").resolve("aprobados").resolve("docker");
        Files.createDirectories(origen);
        Files.writeString(origen.resolve("test01.json"), """
                {
                  "id": "banco-test-01",
                  "modulo": "banco_docker",
                  "titulo": "Test nginx",
                  "enunciado": "Levanta nginx en 8080",
                  "criterios": [{"tipo": "contiene_todos", "terminos": ["docker run"], "peso": 5}],
                  "solucion_referencia": "docker run -p 8080:80 nginx"
                }
                """);

        byte[] paquete = servicioPortable.exportarPaqueteCompleto();
        assertTrue(paquete.length > 50);

        PropiedadesForjaExamenes props2 = new PropiedadesForjaExamenes();
        Path dirBancoAlumno = tempDir.resolve("alumno-banco");
        props2.setDirectorioBanco(dirBancoAlumno.toString());
        ServicioBancoEjercicios servicioAlumno = new ServicioBancoEjercicios(props2, event -> {});
        ServicioBancoPortable portableAlumno = new ServicioBancoPortable(servicioAlumno);

        var resultado = portableAlumno.importar(paquete);
        assertEquals(1, resultado.importados());
        assertTrue(Files.isRegularFile(
                dirBancoAlumno.resolve("aprobados").resolve("docker").resolve("banco-test-01.json")));
        assertTrue(Files.isRegularFile(dirBancoAlumno.resolve("catalogo.json")));
    }
}
