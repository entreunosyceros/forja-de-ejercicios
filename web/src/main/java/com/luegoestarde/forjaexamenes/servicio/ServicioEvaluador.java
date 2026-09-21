// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.InterpretePython;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import com.luegoestarde.forjaexamenes.util.MapeadorJson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ServicioEvaluador {

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador;

    public ServicioEvaluador(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
        this.mapeador = MapeadorJson.snakeCase();
    }

    public ResultadoEvaluacion evaluar(Escenario escenario, String respuesta) throws Exception {
        Path script = Path.of(propiedades.getScriptEvaluador()).toAbsolutePath().normalize();
        Path archivoEscenario = Files.createTempFile("escenario-", ".json");
        Path archivoRespuesta = Files.createTempFile("respuesta-", ".txt");
        Path archivoResultado = Files.createTempFile("resultado-", ".json");

        try {
            mapeador.writerWithDefaultPrettyPrinter().writeValue(archivoEscenario.toFile(), escenario);
            Files.writeString(archivoRespuesta, respuesta == null ? "" : respuesta, StandardCharsets.UTF_8);

            List<String> comando = new ArrayList<>();
            comando.add(InterpretePython.resolver(propiedades));
            comando.add(script.toString());
            comando.add("--escenario");
            comando.add(archivoEscenario.toString());
            comando.add("--archivo-respuesta");
            comando.add(archivoRespuesta.toString());
            comando.add("--salida");
            comando.add(archivoResultado.toString());

            ProcessBuilder constructorProceso = new ProcessBuilder(comando);
            constructorProceso.directory(script.getParent().toFile());
            constructorProceso.redirectErrorStream(true);
            Map<String, String> entorno = constructorProceso.environment();
            if (propiedades.isCorrectorIa()) {
                entorno.put("FORJAEXAMENES_CORRECTOR_IA", "true");
                String modo = propiedades.getCorrectorIaModo();
                if (modo != null && !modo.isBlank()) {
                    entorno.put("FORJAEXAMENES_CORRECTOR_IA_MODO", modo.trim());
                }
            } else {
                entorno.put("FORJAEXAMENES_CORRECTOR_IA", "false");
            }
            if (propiedades.getGeminiApiKey() != null && !propiedades.getGeminiApiKey().isBlank()) {
                entorno.put("GEMINI_API_KEY", propiedades.getGeminiApiKey());
                entorno.put("FORJAEXAMENES_GEMINI_API_KEY", propiedades.getGeminiApiKey());
            }
            if (propiedades.getGeminiModel() != null && !propiedades.getGeminiModel().isBlank()) {
                entorno.put("FORJAEXAMENES_GEMINI_MODEL", propiedades.getGeminiModel());
            }
            entorno.put(
                    "FORJAEXAMENES_GEMINI_TIMEOUT_MS",
                    String.valueOf(Math.max(1, propiedades.getGeminiTimeoutMs())));

            long timeout = propiedades.getTimeoutEvaluadorSegundos();
            if (propiedades.isCorrectorIa()) {
                timeout = Math.max(timeout, 90);
            }

            EjecutorProcesoPython.Resultado res = EjecutorProcesoPython.ejecutar(
                    constructorProceso, timeout);
            if (res.codigo() != 0) {
                throw new IllegalStateException("evaluador.py falló: " + res.salida());
            }

            ResultadoEvaluacion resultado = mapeador.readValue(archivoResultado.toFile(), ResultadoEvaluacion.class);
            resultado.setRespuestaAlumno(respuesta);
            return resultado;
        } finally {
            Files.deleteIfExists(archivoEscenario);
            Files.deleteIfExists(archivoRespuesta);
            Files.deleteIfExists(archivoResultado);
        }
    }
}
