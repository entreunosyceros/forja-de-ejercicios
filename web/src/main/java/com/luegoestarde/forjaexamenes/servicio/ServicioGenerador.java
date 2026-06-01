package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ServicioGenerador {

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador;

    public ServicioGenerador(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
        this.mapeador = new ObjectMapper();
        this.mapeador.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public Escenario generar(Optional<String> modulo) throws Exception {
        return generar(modulo, 2);
    }

    public Escenario generar(Optional<String> modulo, int nivel) throws Exception {
        Path script = Path.of(propiedades.getScriptGenerador()).toAbsolutePath().normalize();
        if (!Files.exists(script)) {
            throw new IllegalStateException("No se encuentra generador.py en: " + script);
        }

        List<String> comando = new ArrayList<>();
        comando.add("python3");
        comando.add(script.toString());
        comando.add("--formateado");
        comando.add("--nivel");
        comando.add(String.valueOf(nivel));
        modulo.ifPresent(m -> {
            comando.add("--modulo");
            comando.add(m);
        });

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
            throw new IllegalStateException("generador.py falló: " + salida);
        }

        return mapeador.readValue(salida.toString().trim(), Escenario.class);
    }
}
