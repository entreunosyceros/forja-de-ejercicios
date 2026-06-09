// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.Path;

class PruebaServicioDificultadAdaptativa {

    @TempDir
    Path tempDir;

    private ServicioDificultadAdaptativa adaptativa;
    private ServicioEstadisticasUsuario estadisticas;
    private ServicioCuentasUsuarios cuentas;

    @BeforeEach
    void preparar() throws Exception {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setDirectorioDatos(tempDir.toString());
        PropiedadesLogin login = new PropiedadesLogin();
        login.setUsuarios("alumno:practica,profesor:profesor");
        cuentas = new ServicioCuentasUsuarios(props, login, new BCryptPasswordEncoder());
        cuentas.inicializar();
        estadisticas = new ServicioEstadisticasUsuario(props);
        ServicioAccesoProfesor acceso = new ServicioAccesoProfesor(props, login, cuentas);
        adaptativa = new ServicioDificultadAdaptativa(cuentas, estadisticas, acceso);
    }

    @Test
    void cincoAprobadosSubenNivel() throws Exception {
        for (int i = 0; i < 5; i++) {
            estadisticas.registrar("alumno", "poo", 8.0, true, 60L);
        }

        var ajuste = adaptativa.evaluarTrasIntento("alumno");
        assertTrue(ajuste.isPresent());
        assertEquals(2, ajuste.get().nivelAnterior());
        assertEquals(3, ajuste.get().nivelNuevo());
        assertEquals(3, cuentas.obtenerNivelGemini("alumno"));
        assertEquals(0, estadisticas.obtener("alumno").getRachaActual());
    }

    @Test
    void tresSuspensosBajanNivel() throws Exception {
        cuentas.actualizarNivelGemini("alumno", 3);
        for (int i = 0; i < 3; i++) {
            estadisticas.registrar("alumno", "poo", 4.0, false, 60L);
        }

        var ajuste = adaptativa.evaluarTrasIntento("alumno");
        assertTrue(ajuste.isPresent());
        assertEquals(3, ajuste.get().nivelAnterior());
        assertEquals(2, ajuste.get().nivelNuevo());
        assertEquals(2, cuentas.obtenerNivelGemini("alumno"));
        assertEquals(0, estadisticas.obtener("alumno").getRachaSuspensos());
    }

    @Test
    void noAjustaProfesor() throws Exception {
        for (int i = 0; i < 5; i++) {
            estadisticas.registrar("profesor", "poo", 8.0, true, 60L);
        }
        assertTrue(adaptativa.evaluarTrasIntento("profesor").isEmpty());
        assertEquals(2, cuentas.obtenerNivelGemini("profesor"));
    }

    @Test
    void noBajaPorDebajoDeUno() throws Exception {
        cuentas.actualizarNivelGemini("alumno", 1);
        for (int i = 0; i < 3; i++) {
            estadisticas.registrar("alumno", "poo", 4.0, false, 60L);
        }
        assertTrue(adaptativa.evaluarTrasIntento("alumno").isEmpty());
        assertEquals(1, cuentas.obtenerNivelGemini("alumno"));
        assertEquals(0, estadisticas.obtener("alumno").getRachaSuspensos());
    }
}
