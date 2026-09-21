// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Escenarios y resultados en memoria con expiración por acceso y tamaño máximo
 * (evita fugas en aulas con muchos alumnos).
 */
@Component
public class AlmacenSesionesEjercicios {

    private static final long EXPIRACION_MS = 2L * 60 * 60 * 1000;
    private static final int TAMANO_MAXIMO = 500;

    private final Map<String, EntradaEscenario> escenarios = new ConcurrentHashMap<>();
    private final Map<String, EntradaResultado> resultados = new ConcurrentHashMap<>();

    public void guardarEscenario(Escenario escenario) {
        purgarSiHaceFalta();
        escenarios.put(escenario.getId(), new EntradaEscenario(escenario, System.currentTimeMillis()));
    }

    public Optional<Escenario> obtenerEscenario(String id, String usuarioAcceso) {
        EntradaEscenario entrada = escenarios.get(id);
        if (entrada == null || entrada.expirada()) {
            escenarios.remove(id);
            return Optional.empty();
        }
        entrada.tocar();
        Escenario escenario = entrada.escenario();
        if (usuarioAcceso != null
                && escenario.getUsuarioAcceso() != null
                && !usuarioAcceso.equals(escenario.getUsuarioAcceso())) {
            throw new IllegalArgumentException("No tienes acceso a este ejercicio.");
        }
        return Optional.of(escenario);
    }

    public void guardarResultado(String id, ResultadoEvaluacion resultado, Long tiempoSegundos) {
        purgarSiHaceFalta();
        resultados.put(id, new EntradaResultado(resultado, tiempoSegundos, System.currentTimeMillis()));
    }

    public Optional<ResultadoEvaluacion> obtenerResultado(String id) {
        EntradaResultado entrada = resultados.get(id);
        if (entrada == null || entrada.expirada()) {
            resultados.remove(id);
            return Optional.empty();
        }
        entrada.tocar();
        return Optional.of(entrada.resultado());
    }

    public Optional<Long> obtenerTiempoSegundos(String id) {
        EntradaResultado entrada = resultados.get(id);
        if (entrada == null || entrada.expirada()) {
            resultados.remove(id);
            return Optional.empty();
        }
        entrada.tocar();
        return Optional.ofNullable(entrada.tiempoSegundos());
    }

    private void purgarSiHaceFalta() {
        long ahora = System.currentTimeMillis();
        escenarios.entrySet().removeIf(e -> e.getValue().expirada(ahora));
        resultados.entrySet().removeIf(e -> e.getValue().expirada(ahora));
        while (escenarios.size() > TAMANO_MAXIMO) {
            eliminarMasAntigua(escenarios);
        }
        while (resultados.size() > TAMANO_MAXIMO) {
            eliminarMasAntigua(resultados);
        }
    }

    private static <T extends EntradaBase> void eliminarMasAntigua(Map<String, T> mapa) {
        Iterator<Map.Entry<String, T>> it = mapa.entrySet().iterator();
        String claveMasAntigua = null;
        long accesoMin = Long.MAX_VALUE;
        while (it.hasNext()) {
            Map.Entry<String, T> e = it.next();
            if (e.getValue().ultimoAccesoMs() < accesoMin) {
                accesoMin = e.getValue().ultimoAccesoMs();
                claveMasAntigua = e.getKey();
            }
        }
        if (claveMasAntigua != null) {
            mapa.remove(claveMasAntigua);
        }
    }

    private abstract static class EntradaBase {
        private volatile long ultimoAccesoMs;

        EntradaBase(long ahora) {
            this.ultimoAccesoMs = ahora;
        }

        void tocar() {
            ultimoAccesoMs = System.currentTimeMillis();
        }

        long ultimoAccesoMs() {
            return ultimoAccesoMs;
        }

        boolean expirada() {
            return expirada(System.currentTimeMillis());
        }

        boolean expirada(long ahora) {
            return ahora - ultimoAccesoMs > EXPIRACION_MS;
        }
    }

    private static final class EntradaEscenario extends EntradaBase {
        private final Escenario escenario;

        EntradaEscenario(Escenario escenario, long ahora) {
            super(ahora);
            this.escenario = escenario;
        }

        Escenario escenario() {
            return escenario;
        }
    }

    private static final class EntradaResultado extends EntradaBase {
        private final ResultadoEvaluacion resultado;
        private final Long tiempoSegundos;

        EntradaResultado(ResultadoEvaluacion resultado, Long tiempoSegundos, long ahora) {
            super(ahora);
            this.resultado = resultado;
            this.tiempoSegundos = tiempoSegundos;
        }

        ResultadoEvaluacion resultado() {
            return resultado;
        }

        Long tiempoSegundos() {
            return tiempoSegundos;
        }
    }
}
