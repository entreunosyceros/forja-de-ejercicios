package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioHistorialIntentos {

    @TempDir
    Path tempDir;

    private ServicioHistorialIntentos servicio;

    @BeforeEach
    void preparar() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.toString());
        servicio = new ServicioHistorialIntentos(props);
    }

    @Test
    void registrarPersisteEjercicioRespuestaYNota() throws Exception {
        Escenario escenario = new Escenario();
        escenario.setId("ex-1");
        escenario.setModulo("docker");
        escenario.setTitulo("Nginx");
        escenario.setEnunciado("Levanta nginx en 8080");

        ResultadoEvaluacion resultado = new ResultadoEvaluacion();
        resultado.setNota(8.5);
        resultado.setAprobado(true);

        servicio.registrar("alumno", "Ana", escenario, resultado, "docker run -p 8080:80 nginx", 95L);

        var intentos = servicio.listarIntentos("alumno");
        assertEquals(1, intentos.size());
        assertEquals("docker", intentos.get(0).getModulo());
        assertEquals(8.5, intentos.get(0).getNota(), 0.01);
        assertTrue(intentos.get(0).getEnunciado().contains("nginx"));
        assertTrue(intentos.get(0).getRespuesta().contains("docker run"));

        var filas = servicio.listarFilas(login -> true);
        assertEquals(1, filas.size());
        assertEquals("Ana", filas.get(0).alumno());

        byte[] csv = servicio.exportarCsv(login -> true);
        String texto = new String(csv, StandardCharsets.UTF_8);
        assertTrue(texto.contains("Alumno"));
        assertTrue(texto.contains("Ana"));
        assertTrue(texto.contains("docker"));

        assertTrue(Files.isRegularFile(tempDir.resolve("historial").resolve("alumno.json")));
    }
}
