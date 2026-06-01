package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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
        Path archivoResultado = Files.createTempFile("resultado-", ".json");

        try {
            mapeador.writerWithDefaultPrettyPrinter().writeValue(archivoEscenario.toFile(), escenario);

            List<String> comando = new ArrayList<>();
            comando.add("python3");
            comando.add(script.toString());
            comando.add("--escenario");
            comando.add(archivoEscenario.toString());
            comando.add("--respuesta");
            comando.add(respuesta);
            comando.add("--salida");
            comando.add(archivoResultado.toString());

            ProcessBuilder constructorProceso = new ProcessBuilder(comando);
            constructorProceso.directory(script.getParent().toFile());
            constructorProceso.redirectErrorStream(true);
            Process proceso = constructorProceso.start();

            StringBuilder salida = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    salida.append(linea).append('\n');
                }
            }

            int codigo = proceso.waitFor();
            if (codigo != 0) {
                throw new IllegalStateException("evaluador.py falló: " + salida);
            }

            ResultadoEvaluacion resultado = mapeador.readValue(archivoResultado.toFile(), ResultadoEvaluacion.class);
            resultado.setRespuestaAlumno(respuesta);
            return resultado;
        } finally {
            Files.deleteIfExists(archivoEscenario);
            Files.deleteIfExists(archivoResultado);
        }
    }
}
