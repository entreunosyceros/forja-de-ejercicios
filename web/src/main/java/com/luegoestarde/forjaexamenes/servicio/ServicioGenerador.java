package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.luegoestarde.forjaexamenes.configuracion.CargadorEnvFichero;
import com.luegoestarde.forjaexamenes.configuracion.InterpretePython;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ServicioGenerador {

    private final PropiedadesForjaExamenes propiedades;
    private final ServicioConfiguracionGemini configuracionGemini;
    private final ObjectMapper mapeador;

    public ServicioGenerador(
            PropiedadesForjaExamenes propiedades,
            ServicioConfiguracionGemini configuracionGemini) {
        this.propiedades = propiedades;
        this.configuracionGemini = configuracionGemini;
        this.mapeador = new ObjectMapper();
        this.mapeador.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public Escenario generar(Optional<String> modulo) throws Exception {
        return generar(modulo, 2);
    }

    public Escenario generar(Optional<String> modulo, int nivel) throws Exception {
        return generar(modulo, nivel, Optional.empty(), Optional.empty());
    }

    public Escenario generar(
            Optional<String> modulo,
            int nivel,
            Optional<String> capitulo,
            Optional<String> seccion) throws Exception {
        Path script = Path.of(propiedades.getScriptGenerador()).toAbsolutePath().normalize();
        if (!Files.exists(script)) {
            throw new IllegalStateException("No se encuentra generador.py en: " + script);
        }

        List<String> comando = new ArrayList<>();
        comando.add(InterpretePython.resolver(propiedades));
        comando.add(script.toString());
        comando.add("--formateado");
        comando.add("--nivel");
        comando.add(String.valueOf(nivel));
        modulo.ifPresent(m -> {
            comando.add("--modulo");
            comando.add(m);
        });
        capitulo.filter(c -> !c.isBlank()).ifPresent(c -> {
            comando.add("--capitulo");
            comando.add(c);
        });
        seccion.filter(s -> !s.isBlank()).ifPresent(s -> {
            comando.add("--seccion");
            comando.add(s);
        });

        ProcessBuilder constructorProceso = new ProcessBuilder(comando);
        constructorProceso.directory(script.getParent().toFile());
        constructorProceso.redirectErrorStream(true);
        configurarEntornoGemini(constructorProceso);

        EjecutorProcesoPython.Resultado resultado = EjecutorProcesoPython.ejecutar(
                constructorProceso, propiedades.getTimeoutGeneradorSegundos());
        if (resultado.codigo() != 0) {
            throw new IllegalStateException(MensajesErrorGenerador.resumir(resultado.salida()));
        }

        return mapeador.readValue(resultado.salida().trim(), Escenario.class);
    }

    private void configurarEntornoGemini(ProcessBuilder constructorProceso) {
        var entorno = constructorProceso.environment();
        // Quitar claves heredadas del shell/IDE (suelen ser inválidas y bloquean la lectura de .env en Python)
        entorno.remove("GEMINI_API_KEY");
        entorno.remove("FORJAEXAMENES_GEMINI_API_KEY");
        entorno.remove("FORJAEXAMENES_GEMINI_MODEL");

        String clave = configuracionGemini.resolverApiKey();
        if (!clave.isBlank()) {
            entorno.put("GEMINI_API_KEY", clave);
            entorno.put("FORJAEXAMENES_GEMINI_API_KEY", clave);
        }
        String modelo = configuracionGemini.resolverModelo();
        if (modelo.isBlank()) {
            modelo = CargadorEnvFichero.resolverGeminiModelo(
                    propiedades.getGeminiModel(),
                    Path.of(propiedades.getRaiz()).toAbsolutePath().normalize().toString());
        }
        if (!modelo.isBlank()) {
            entorno.put("FORJAEXAMENES_GEMINI_MODEL", modelo);
        }
        if (propiedades.isGeminiGuardarPendientes()) {
            entorno.put("FORJAEXAMENES_GEMINI_GUARDAR_PENDIENTES", "true");
        }
        if (propiedades.isGeminiSoloAprobados()) {
            entorno.put("FORJAEXAMENES_GEMINI_SOLO_APROBADOS", "true");
        }
        if (propiedades.getGeminiTimeoutMs() > 0) {
            entorno.put("FORJAEXAMENES_GEMINI_TIMEOUT_MS",
                    String.valueOf(propiedades.getGeminiTimeoutMs()));
        }
    }
}
