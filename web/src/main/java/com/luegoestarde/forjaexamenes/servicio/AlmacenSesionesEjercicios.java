package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AlmacenSesionesEjercicios {

    private final Map<String, Escenario> escenarios = new ConcurrentHashMap<>();
    private final Map<String, ResultadoEvaluacion> resultados = new ConcurrentHashMap<>();
    private final Map<String, Long> tiemposSegundos = new ConcurrentHashMap<>();

    public void guardarEscenario(Escenario escenario) {
        escenarios.put(escenario.getId(), escenario);
    }

    public Optional<Escenario> obtenerEscenario(String id, String usuarioAcceso) {
        Escenario escenario = escenarios.get(id);
        if (escenario == null) {
            return Optional.empty();
        }
        if (usuarioAcceso != null
                && escenario.getUsuarioAcceso() != null
                && !usuarioAcceso.equals(escenario.getUsuarioAcceso())) {
            throw new IllegalArgumentException("No tienes acceso a este ejercicio.");
        }
        return Optional.of(escenario);
    }

    public void guardarResultado(String id, ResultadoEvaluacion resultado, Long tiempoSegundos) {
        resultados.put(id, resultado);
        if (tiempoSegundos != null) {
            tiemposSegundos.put(id, tiempoSegundos);
        }
    }

    public Optional<ResultadoEvaluacion> obtenerResultado(String id) {
        return Optional.ofNullable(resultados.get(id));
    }

    public Optional<Long> obtenerTiempoSegundos(String id) {
        return Optional.ofNullable(tiemposSegundos.get(id));
    }
}
