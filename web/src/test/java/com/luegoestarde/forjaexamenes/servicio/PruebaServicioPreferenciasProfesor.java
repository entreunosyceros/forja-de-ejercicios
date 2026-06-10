// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioPreferenciasProfesor {

    @TempDir
    Path tempDir;

    private ServicioPreferenciasProfesor servicio;
    private PropiedadesForjaExamenes props;

    @BeforeEach
    void preparar() {
        props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.toString());
        props.setGeminiGuardarPendientes(false);
        servicio = new ServicioPreferenciasProfesor(props);
    }

    @Test
    void usaValorPorDefectoDePropiedades() {
        props.setGeminiGuardarPendientes(true);
        assertTrue(servicio.isGeminiGuardarPendientes());
    }

    @Test
    void guardarPreferenciaSobreescribePropiedades() throws Exception {
        props.setGeminiGuardarPendientes(true);
        servicio.setGeminiGuardarPendientes(false);
        assertFalse(servicio.isGeminiGuardarPendientes());
        assertTrue(Files.isRegularFile(tempDir.resolve("preferencias-profesor.json")));
    }

    @Test
    void activarPreferenciaDesdeInterfaz() throws Exception {
        servicio.setGeminiGuardarPendientes(true);
        assertTrue(servicio.isGeminiGuardarPendientes());
        servicio.setGeminiGuardarPendientes(false);
        assertFalse(servicio.isGeminiGuardarPendientes());
    }
}
