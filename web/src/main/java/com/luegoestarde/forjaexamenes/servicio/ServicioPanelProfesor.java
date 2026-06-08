// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import com.luegoestarde.forjaexamenes.modelo.IntentoHistorial;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class ServicioPanelProfesor {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record MetricasPanel(
            int alumnosActivosSemana,
            String moduloMasPracticado,
            int intentosUltimos7Dias,
            int aprobadosUltimos7Dias,
            double tasaAprobacion7Dias) {}

    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioHistorialIntentos servicioHistorial;
    private final ServicioEntregasAlumno servicioEntregas;
    private final ServicioAccesoProfesor accesoProfesor;

    public ServicioPanelProfesor(
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioHistorialIntentos servicioHistorial,
            ServicioEntregasAlumno servicioEntregas,
            ServicioAccesoProfesor accesoProfesor) {
        this.servicioEstadisticas = servicioEstadisticas;
        this.servicioHistorial = servicioHistorial;
        this.servicioEntregas = servicioEntregas;
        this.accesoProfesor = accesoProfesor;
    }

    public MetricasPanel calcular(String profesorLogin) throws IOException {
        LocalDateTime limite = LocalDateTime.now(ZONA).minusDays(7);
        Predicate<String> esAlumno = login -> !accesoProfesor.esProfesor(login);

        Set<String> alumnosActivos = new HashSet<>();
        Map<String, Integer> conteoModulos = new HashMap<>();
        int intentos7d = 0;
        int aprobados7d = 0;

        for (var resumen : servicioEstadisticas.listarResumenesEnServidor(esAlumno)) {
            if (actividadReciente(resumen.ultimaActividad(), limite)) {
                alumnosActivos.add(resumen.login());
            }
        }

        for (var fila : servicioHistorial.listarFilas(esAlumno)) {
            if (fechaReciente(fila.fecha(), limite)) {
                alumnosActivos.add(fila.login());
                intentos7d++;
                if (fila.aprobado()) {
                    aprobados7d++;
                }
                if (fila.modulo() != null && !fila.modulo().isBlank()) {
                    conteoModulos.merge(fila.modulo(), 1, Integer::sum);
                }
            }
        }

        if (profesorLogin != null && !profesorLogin.isBlank()) {
            for (EntregaAlumno entrega : servicioEntregas.listarEntregasCompletas(profesorLogin)) {
                String clave = claveAlumno(entrega);
                EstadisticasUsuario stats = entrega.getEstadisticasServidor();
                if (stats != null && actividadReciente(stats.getUltimaActividad(), limite)) {
                    alumnosActivos.add(clave);
                }
                if (entrega.getHistorialIntentos() != null) {
                    for (IntentoHistorial i : entrega.getHistorialIntentos()) {
                        if (fechaReciente(i.getFecha(), limite)) {
                            alumnosActivos.add(clave);
                            intentos7d++;
                            if (i.isAprobado()) {
                                aprobados7d++;
                            }
                            if (i.getModulo() != null && !i.getModulo().isBlank()) {
                                conteoModulos.merge(i.getModulo(), 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        String moduloTop = conteoModulos.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");

        double tasa = intentos7d > 0
                ? Math.round(100.0 * aprobados7d / intentos7d * 10.0) / 10.0
                : 0.0;

        return new MetricasPanel(alumnosActivos.size(), moduloTop, intentos7d, aprobados7d, tasa);
    }

    private static String claveAlumno(EntregaAlumno entrega) {
        if (entrega.getAlumno() != null
                && entrega.getAlumno().getLogin() != null
                && !entrega.getAlumno().getLogin().isBlank()) {
            return entrega.getAlumno().getLogin();
        }
        if (entrega.getNombreEtiqueta() != null && !entrega.getNombreEtiqueta().isBlank()) {
            return entrega.getNombreEtiqueta();
        }
        return entrega.getIdImportacion();
    }

    private static boolean actividadReciente(String fechaTexto, LocalDateTime limite) {
        return fechaReciente(fechaTexto, limite);
    }

    private static boolean fechaReciente(String fechaTexto, LocalDateTime limite) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return false;
        }
        try {
            return LocalDateTime.parse(fechaTexto.strip(), FORMATO).isAfter(limite);
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }
}
