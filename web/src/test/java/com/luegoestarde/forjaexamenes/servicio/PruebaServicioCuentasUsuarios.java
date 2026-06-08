// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesLogin;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PruebaServicioCuentasUsuarios {

    @TempDir
    Path tempDir;

    private ServicioCuentasUsuarios servicio;

    @BeforeEach
    void preparar() throws Exception {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.toString());
        PropiedadesLogin login = new PropiedadesLogin();
        login.setUsuarios("alumno:practica");
        servicio = new ServicioCuentasUsuarios(props, login, new BCryptPasswordEncoder());
        servicio.inicializar();
    }

    @Test
    void creaFicheroUsuariosAlArrancar() {
        assertTrue(Files.exists(tempDir.resolve("usuarios.json")));
        assertEquals("Alumno", servicio.nombreVisible("alumno"));
    }

    @Test
    void actualizaNombreYContrasena() throws Exception {
        var resultado = servicio.actualizarPerfil(
                "alumno", "alumno", "María López", "practica", "nueva123", "nueva123");
        assertEquals("María López", servicio.nombreVisible("alumno"));
        assertTrue(resultado.mensaje().contains("Contraseña"));
    }

    @Test
    void rechazaContrasenaActualIncorrecta() {
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarPerfil(
                "alumno", "alumno", "María", "mala", null, null));
    }

    @Test
    void nivelGeminiPorDefectoYActualizable() throws Exception {
        assertEquals(2, servicio.obtenerNivelGemini("alumno"));
        servicio.actualizarNivelGemini("alumno", 3);
        assertEquals(3, servicio.obtenerNivelGemini("alumno"));
        servicio.actualizarNivelGemini("alumno", 9);
        assertEquals(3, servicio.obtenerNivelGemini("alumno"));
    }
}
