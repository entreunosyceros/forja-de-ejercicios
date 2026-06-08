// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.luegoestarde.forjaexamenes.configuracion.InterpretePython;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServicioEvaluador {

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador;

    public ServicioEvaluador(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
        this.mapeador = new ObjectMapper();
        this.mapeador.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public ResultadoEvaluacion evaluar(Escenario escenario, String respuesta) throws Exception {
        Path script = Path.of(propiedades.getScriptEvaluador()).toAbsolutePath().normalize();
        Path archivoEscenario = Files.createTempFile("escenario-", ".json");
        Path archivoRespuesta = Files.createTempFile("respuesta-", ".txt");
        Path archivoResultado = Files.createTempFile("resultado-", ".json");

        try {
            mapeador.writerWithDefaultPrettyPrinter().writeValue(archivoEscenario.toFile(), escenario);
            // La respuesta va por archivo (no como argumento) para no chocar con el
            // límite de longitud de la línea de comandos ni problemas de escapado.
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

            EjecutorProcesoPython.Resultado res = EjecutorProcesoPython.ejecutar(
                    constructorProceso, propiedades.getTimeoutEvaluadorSegundos());
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
