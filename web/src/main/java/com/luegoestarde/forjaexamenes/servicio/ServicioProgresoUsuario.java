// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario.EstadisticasModulo;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario.IntentoReciente;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Vista de progreso para la UI: el servidor es la fuente de verdad;
 * el navegador solo cachea este snapshot.
 */
@Service
public class ServicioProgresoUsuario {

    private final ServicioEstadisticasUsuario servicioEstadisticas;

    public ServicioProgresoUsuario(ServicioEstadisticasUsuario servicioEstadisticas) {
        this.servicioEstadisticas = servicioEstadisticas;
    }

    public Map<String, Object> snapshot(String login) {
        EstadisticasUsuario stats = servicioEstadisticas.obtener(login);
        return desdeEstadisticas(stats);
    }

    public Map<String, Object> desdeEstadisticas(EstadisticasUsuario stats) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("historial", mapearHistorial(stats.getUltimosIntentos()));
        out.put("stats", mapearStats(stats));
        out.put("fuente", "servidor");
        return out;
    }

    private static List<Map<String, Object>> mapearHistorial(List<IntentoReciente> intentos) {
        List<Map<String, Object>> lista = new ArrayList<>();
        if (intentos == null) {
            return lista;
        }
        for (IntentoReciente i : intentos) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("modulo", i.getModulo() != null ? i.getModulo() : "");
            fila.put("titulo", i.getTitulo() != null ? i.getTitulo() : "");
            fila.put("enunciado", i.getTitulo() != null ? i.getTitulo() : "");
            fila.put("nota", i.getNota());
            fila.put("aprobado", i.isAprobado());
            fila.put("tiempoSegundos", i.getTiempoSegundos());
            fila.put("ejercicioId", i.getEjercicioId() != null ? i.getEjercicioId() : "");
            fila.put("fecha", i.getFecha() != null ? i.getFecha() : "");
            fila.put("dificultad", i.getDificultad() != null ? i.getDificultad() : 2);
            lista.add(fila);
        }
        return lista;
    }

    private static Map<String, Object> mapearStats(EstadisticasUsuario stats) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> porModulo = new LinkedHashMap<>();
        Map<String, Boolean> modulosAprobados = new LinkedHashMap<>();
        if (stats.getPorModulo() != null) {
            for (Map.Entry<String, EstadisticasModulo> e : stats.getPorModulo().entrySet()) {
                EstadisticasModulo m = e.getValue();
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("intentos", m.getIntentos());
                pm.put("aprobados", m.getAprobados());
                pm.put("mejorNota", m.getMejorNota());
                pm.put("mediaNota", m.getNotaMedia());
                pm.put("sumaNotas", Math.round(m.getNotaMedia() * m.getIntentos() * 10.0) / 10.0);
                porModulo.put(e.getKey(), pm);
                if (m.getAprobados() > 0) {
                    modulosAprobados.put(e.getKey(), true);
                }
            }
        }
        out.put("porModulo", porModulo);
        out.put("totalAprobados", stats.getTotalAprobados());
        out.put("rachaActual", stats.getRachaActual());
        out.put("modulosAprobados", modulosAprobados);
        out.put("modulosDistintos", porModulo.size());
        return out;
    }
}
