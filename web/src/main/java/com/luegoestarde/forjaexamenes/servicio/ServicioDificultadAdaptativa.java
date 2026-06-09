// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import java.io.IOException;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Ajusta automáticamente el nivel de dificultad del alumno según rachas de resultados.
 */
@Service
public class ServicioDificultadAdaptativa {

    public static final int APROBADOS_PARA_SUBIR = 5;
    public static final int SUSPENSOS_PARA_BAJAR = 3;

    public record AjusteDificultad(int nivelAnterior, int nivelNuevo, String mensaje) {}

    private final ServicioCuentasUsuarios cuentasUsuarios;
    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioAccesoProfesor accesoProfesor;

    public ServicioDificultadAdaptativa(
            ServicioCuentasUsuarios cuentasUsuarios,
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioAccesoProfesor accesoProfesor) {
        this.cuentasUsuarios = cuentasUsuarios;
        this.servicioEstadisticas = servicioEstadisticas;
        this.accesoProfesor = accesoProfesor;
    }

    /**
     * Tras registrar un intento, comprueba rachas y sube/baja el nivel si corresponde.
     */
    public Optional<AjusteDificultad> evaluarTrasIntento(String login) throws IOException {
        if (login == null || login.isBlank() || accesoProfesor.esProfesor(login)) {
            return Optional.empty();
        }

        EstadisticasUsuario stats = servicioEstadisticas.obtener(login);
        int nivelActual = cuentasUsuarios.obtenerNivelGemini(login);

        if (stats.getRachaActual() >= APROBADOS_PARA_SUBIR) {
            int nuevo = Math.min(3, nivelActual + 1);
            if (nuevo > nivelActual) {
                cuentasUsuarios.actualizarNivelGemini(login, nuevo);
                servicioEstadisticas.reiniciarRachaAprobados(login);
                return Optional.of(new AjusteDificultad(
                        nivelActual,
                        nuevo,
                        "¡" + APROBADOS_PARA_SUBIR + " aprobados seguidos! Dificultad subida a nivel "
                                + nuevo + "."));
            }
            servicioEstadisticas.reiniciarRachaAprobados(login);
        }

        if (stats.getRachaSuspensos() >= SUSPENSOS_PARA_BAJAR) {
            int nuevo = Math.max(1, nivelActual - 1);
            if (nuevo < nivelActual) {
                cuentasUsuarios.actualizarNivelGemini(login, nuevo);
                servicioEstadisticas.reiniciarRachaSuspensos(login);
                return Optional.of(new AjusteDificultad(
                        nivelActual,
                        nuevo,
                        SUSPENSOS_PARA_BAJAR + " suspensos seguidos. Dificultad bajada a nivel "
                                + nuevo + " para ayudarte a repasar."));
            }
            servicioEstadisticas.reiniciarRachaSuspensos(login);
        }

        return Optional.empty();
    }
}
