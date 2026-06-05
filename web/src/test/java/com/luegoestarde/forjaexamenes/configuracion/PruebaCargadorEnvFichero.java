package com.luegoestarde.forjaexamenes.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaCargadorEnvFichero {

    @TempDir
    Path tempDir;

    @Test
    void prefiereFicheroEnvSobrePropiedadesVacias() throws Exception {
        Files.writeString(
                tempDir.resolve(".env"),
                "GEMINI_API_KEY=AIzaSyClaveDePrueba123456789\n");
        String clave = CargadorEnvFichero.resolverGeminiApiKey("", tempDir.toString());
        assertEquals("AIzaSyClaveDePrueba123456789", clave);
    }

    @Test
    void propiedadesTienenPrioridadSobreFichero() throws Exception {
        Files.writeString(tempDir.resolve(".env"), "GEMINI_API_KEY=desde_fichero\n");
        String clave = CargadorEnvFichero.resolverGeminiApiKey("desde_propiedades", tempDir.toString());
        assertEquals("desde_propiedades", clave);
    }

    @Test
    void datosTienenPrioridadSobreEnv() throws Exception {
        Path datos = tempDir.resolve("datos");
        Files.createDirectories(datos);
        Files.writeString(
                datos.resolve("gemini.json"),
                "{\"apiKey\":\"AIzaSyDesdeInterfaz\"}\n");
        Files.writeString(tempDir.resolve(".env"), "GEMINI_API_KEY=desde_env\n");
        String clave = CargadorEnvFichero.resolverGeminiApiKey("", tempDir.toString(), datos);
        assertEquals("AIzaSyDesdeInterfaz", clave);
    }
}
