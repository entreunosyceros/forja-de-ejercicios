// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
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
        servicioBanco = new ServicioBancoEjercicios(props, noop, null);
        servicioPortable = new ServicioBancoPortable(servicioBanco);
    }

    @Test
    void importarNormalizaModuloInvalido() throws Exception {
        String json = """
                {
                  "id": "mal-modulo",
                  "modulo": "banco_aprobados",
                  "titulo": "Docker mal etiquetado",
                  "enunciado": "docker ps",
                  "criterios": [{"tipo": "contiene_todos", "terminos": ["docker"], "peso": 5}]
                }
                """;
        var resultado = servicioPortable.importar(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(1, resultado.importados());

        Path fichero = tempDir.resolve("banco").resolve("aprobados").resolve("general").resolve("mal-modulo.json");
        assertTrue(Files.isRegularFile(fichero));
        String modulo = new ObjectMapper().readTree(fichero.toFile()).path("modulo").asText();
        assertEquals("banco_general", modulo);

        var catalogo = servicioBanco.listarParaPortada();
        assertEquals(1, catalogo.size());
        assertEquals("banco_general", catalogo.get(0).modulo());
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
        ServicioBancoEjercicios servicioAlumno = new ServicioBancoEjercicios(props2, event -> {}, null);
        ServicioBancoPortable portableAlumno = new ServicioBancoPortable(servicioAlumno);

        var resultado = portableAlumno.importar(paquete);
        assertEquals(1, resultado.importados());
        assertTrue(Files.isRegularFile(
                dirBancoAlumno.resolve("aprobados").resolve("docker").resolve("banco-test-01.json")));
        assertTrue(Files.isRegularFile(dirBancoAlumno.resolve("catalogo.json")));
    }

    @Test
    void guardarEjercicioLocalEnAprobadosYPendientes() throws Exception {
        Escenario valido = new ObjectMapper().readValue("""
                {
                  "id": "banco-guardar-ok",
                  "modulo": "docker",
                  "titulo": "Test",
                  "enunciado": "docker ps",
                  "criterios": [{"tipo": "contiene_todos", "terminos": ["docker"], "peso": 5}],
                  "solucion_referencia": "docker ps"
                }
                """, Escenario.class);
        ResultadoEvaluacion evalOk = new ResultadoEvaluacion();
        evalOk.setPesoObtenido(5);
        evalOk.setPesoTotal(5);
        evalOk.setNota(10.0);
        var ok = servicioPortable.guardarEjercicioLocal(valido, evalOk, "docker-ps-ejemplo");
        assertEquals("aprobados", ok.destino());
        assertEquals("docker-ps-ejemplo.json", ok.nombreArchivo());
        assertTrue(Files.isRegularFile(
                tempDir.resolve("banco").resolve("aprobados").resolve("docker").resolve("docker-ps-ejemplo.json")));

        Escenario invalido = new ObjectMapper().readValue("""
                {
                  "id": "banco-guardar-pend",
                  "modulo": "docs_docker",
                  "titulo": "Pendiente",
                  "enunciado": "Levanta nginx",
                  "criterios": [
                    {"tipo": "contiene_todos", "terminos": ["docker run"], "peso": 3},
                    {"tipo": "contiene_todos", "terminos": ["nginx"], "peso": 2}
                  ],
                  "solucion_referencia": "incompleta"
                }
                """, Escenario.class);
        ResultadoEvaluacion evalMal = new ResultadoEvaluacion();
        evalMal.setPesoObtenido(0);
        evalMal.setPesoTotal(5);
        evalMal.setNota(0.0);
        var pend = servicioPortable.guardarEjercicioLocal(invalido, evalMal, "nginx-incompleto");
        assertEquals("pendientes", pend.destino());
        assertEquals("nginx-incompleto.json", pend.nombreArchivo());
        assertTrue(Files.isRegularFile(tempDir.resolve("banco").resolve("pendientes").resolve("nginx-incompleto.json")));
    }

    @Test
    void guardarEnBancoQuitaPrefijoNivelDelEnunciado() throws Exception {
        Escenario conPrefijo = new ObjectMapper().readValue("""
                {
                  "id": "banco-prefijo",
                  "modulo": "poo",
                  "titulo": "Interfaces",
                  "dificultad": 2,
                  "enunciado": "[Nivel intermedio]\\n\\nImplementa Imprimible.",
                  "criterios": [{"tipo": "contiene_todos", "terminos": ["implements"], "peso": 5}],
                  "solucion_referencia": "class Pedido implements Imprimible {}"
                }
                """, Escenario.class);
        ResultadoEvaluacion evalOk = new ResultadoEvaluacion();
        evalOk.setPesoObtenido(5);
        evalOk.setPesoTotal(5);
        evalOk.setNota(10.0);
        servicioPortable.guardarEjercicioLocal(conPrefijo, evalOk, "poo-interfaces");
        String guardado = Files.readString(
                tempDir.resolve("banco").resolve("aprobados").resolve("poo").resolve("poo-interfaces.json"));
        assertFalse(guardado.contains("[Nivel intermedio]"));
        assertTrue(guardado.contains("Implementa Imprimible."));
        assertTrue(guardado.contains("\"dificultad\" : 2"));
    }
}
