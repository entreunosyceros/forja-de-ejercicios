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

    public void guardarEscenario(Escenario escenario) {
        escenarios.put(escenario.getId(), escenario);
    }

    public Optional<Escenario> obtenerEscenario(String id) {
        return Optional.ofNullable(escenarios.get(id));
    }

    public void guardarResultado(String id, ResultadoEvaluacion resultado) {
        resultados.put(id, resultado);
    }

    public Optional<ResultadoEvaluacion> obtenerResultado(String id) {
        return Optional.ofNullable(resultados.get(id));
    }
}
