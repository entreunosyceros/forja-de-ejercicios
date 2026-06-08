// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.ConfiguracionGeminiAlmacenada;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioConfiguracionGemini {

    @TempDir
    Path tempDir;

    private ServicioConfiguracionGemini servicio;

    @BeforeEach
    void preparar() throws Exception {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setRaiz(tempDir.toString());
        Path datosDir = tempDir.resolve("datos");
        Files.createDirectories(datosDir);
        // En producción directorio-datos se cablea a raiz/datos; aquí hay que fijarlo igual.
        props.setDirectorioDatos(datosDir.toString());

        ConfiguracionGeminiAlmacenada cfg = new ConfiguracionGeminiAlmacenada();
        cfg.setApiKey("AIzaSyClaveDePrueba123456789");
        cfg.setModel("gemini-2.5-flash");
        new ObjectMapper().writeValue(datosDir.resolve("gemini.json").toFile(), cfg);

        servicio = new ServicioConfiguracionGemini(props, null);
    }

    @Test
    void resuelveClaveDesdeDatos() {
        assertTrue(servicio.estaConfigurado());
        assertEquals("AIzaSyClaveDePrueba123456789", servicio.resolverApiKey());
        assertEquals("gemini-2.5-flash", servicio.resolverModelo());
    }

    @Test
    void enmascararClave() {
        assertEquals("AIza••••••6789", ServicioConfiguracionGemini.enmascarar("AIzaSy123456789"));
    }
}
