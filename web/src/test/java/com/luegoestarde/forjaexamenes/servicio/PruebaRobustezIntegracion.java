// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests de robustez: mono rompe-teclados, JSON manipulado y contenedor sucio.
 */
class PruebaRobustezIntegracion {

    @TempDir
    Path tempDir;

    private ServicioEvaluador servicioEvaluador;
    private ServicioEntregasAlumno servicioEntregas;
    private ServicioEntornoPractica servicioEntorno;
    private final ObjectMapper mapeador = new ObjectMapper();

    @BeforeEach
    void preparar() throws Exception {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.resolve("datos").toString());
        props.setRaiz(tempDir.toString());
        Path evaluador = Path.of("..", "evaluador.py").toAbsolutePath().normalize();
        if (!Files.isRegularFile(evaluador)) {
            evaluador = Path.of(System.getProperty("user.dir")).getParent().resolve("evaluador.py");
        }
        props.setScriptEvaluador(evaluador.toString());

        servicioEvaluador = new ServicioEvaluador(props);
        servicioEntregas = new ServicioEntregasAlumno(
                props,
                new ServicioEstadisticasUsuario(props),
                new ServicioHistorialIntentos(props));
        servicioEntorno = new ServicioEntornoPractica(props);
    }

    @Test
    void evaluadorRespondeCeroConBasuraSinPetardazo() throws Exception {
        Escenario escenario = escenarioDockerMinimo();
        String[] basura = {"", "$;--", "\u0000\u00ff", "%%%@@@###", "a".repeat(50_000)};
        for (String respuesta : basura) {
            ResultadoEvaluacion resultado = servicioEvaluador.evaluar(escenario, respuesta);
            assertNotNull(resultado);
            assertTrue(resultado.getNota() >= 0.0 && resultado.getNota() <= 10.0);
            assertFalse(resultado.isAprobado());
        }
        // Alias mal estructurado: no debe petardear; puede sacar nota parcial por sinónimos
        ResultadoEvaluacion aliasMal = servicioEvaluador.evaluar(escenario, "docker container run nginx -p mal");
        assertNotNull(aliasMal);
        assertTrue(aliasMal.getNota() >= 0.0 && aliasMal.getNota() < 10.0);
    }

    @Test
    void importarJsonCorruptoDevuelveMensajeControlado() {
        byte[] comaBorrada = """
                {"formato":"forja-entrega-alumno","alumno":{"login":"a"},"estadisticasServidor":{"totalIntentos" 1}}
                """.strip().getBytes(StandardCharsets.UTF_8);
        IOException ex1 = assertThrows(IOException.class,
                () -> servicioEntregas.importar("profesor", comaBorrada, "Test"));
        assertTrue(ex1.getMessage().contains("Archivo inválido"), ex1.getMessage());

        byte[] notaTexto = """
                {"formato":"forja-entrega-alumno","alumno":{"login":"a"},
                 "estadisticasServidor":{"totalIntentos":1,"notaMedia":"un siete"}}
                """.strip().getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> servicioEntregas.importar("profesor", notaTexto, "Test"));

        IOException ex3 = assertThrows(IOException.class,
                () -> servicioEntregas.importar("profesor", "{no json".getBytes(StandardCharsets.UTF_8), "Test"));
        assertFalse(ex3.getMessage().contains("at com.fasterxml"),
                "No debe filtrar stacktrace: " + ex3.getMessage());

        IOException ex4 = assertThrows(IOException.class,
                () -> servicioEntregas.importar("profesor", new byte[0], "Test"));
        assertTrue(ex4.getMessage().contains("vacío"));
    }

    @Test
    void limpiezaDatosPracticaEliminaBasura() throws Exception {
        Path practica = servicioEntorno.carpetaDatosPractica();
        Files.createDirectories(practica);
        Files.writeString(practica.resolve("basura.txt"), "xxx");
        Path sub = practica.resolve("subdir");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("otro.dat"), "yyy");
        Files.writeString(practica.resolve(".gitkeep"), "");

        var resultado = servicioEntorno.limpiar();
        assertTrue(resultado.elementosEliminados() >= 2);
        assertFalse(Files.exists(practica.resolve("basura.txt")));
        assertFalse(Files.exists(sub));
        assertTrue(Files.exists(practica.resolve(".gitkeep")));
        assertTrue(servicioEntorno.debeLimpiarAlNuevoEjercicio("docker"));
    }

    private static Escenario escenarioDockerMinimo() {
        Escenario escenario = new Escenario();
        escenario.setId("test-docker");
        escenario.setModulo("docker");
        escenario.setTitulo("Nginx");
        escenario.setEnunciado("Levanta nginx en 8080");
        escenario.setCriterios(java.util.List.of(
                criterio("contiene_todos", java.util.List.of("docker run"), 5),
                criterio("contiene_alguno", java.util.List.of("-p 8080:80"), 5)));
        return escenario;
    }

    private static Escenario.Criterio criterio(String tipo, java.util.List<String> terminos, int peso) {
        Escenario.Criterio c = new Escenario.Criterio();
        c.setTipo(tipo);
        c.setTerminos(terminos);
        c.setPeso(peso);
        c.setEsperado("test");
        return c;
    }
}
