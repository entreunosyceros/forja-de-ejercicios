// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import jakarta.annotation.PreDestroy;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mantiene un pequeño pool de ejercicios {@code docs_*} ya generados con Gemini,
 * para servirlos al instante y desacoplar la latencia de la API de la petición HTTP.
 *
 * <p>El relleno se hace en segundo plano con un executor acotado (limita las
 * llamadas paralelas a Gemini, lo que respeta mejor los límites de cuota). Solo
 * se pre-generan ejercicios «de todo el tema» (sin capítulo/sección concretos).
 */
@Service
public class ServicioPrecargaEjercicios {

    private static final Logger log = LoggerFactory.getLogger(ServicioPrecargaEjercicios.class);
    private static final long ENFRIAMIENTO_FALLO_MS = 60_000;

    private final ServicioGenerador generador;
    private final PropiedadesForjaExamenes propiedades;

    private final Map<String, Deque<Escenario>> pool = new ConcurrentHashMap<>();
    private final Set<String> enCurso = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> enfriamiento = new ConcurrentHashMap<>();
    private final ExecutorService ejecutor;

    public ServicioPrecargaEjercicios(ServicioGenerador generador, PropiedadesForjaExamenes propiedades) {
        this.generador = generador;
        this.propiedades = propiedades;
        int hilos = Math.max(1, propiedades.getPrecargaHilos());
        this.ejecutor = Executors.newFixedThreadPool(hilos, new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "precarga-ejercicios-" + n.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
    }

    public boolean activa() {
        return propiedades.isPrecargaEjerciciosActiva() && propiedades.getPrecargaPorClave() > 0;
    }

    /**
     * Devuelve un ejercicio pre-generado para {@code modulo}/{@code nivel} si lo hay,
     * y programa el relleno del pool en segundo plano. Solo aplica a módulos {@code docs_*}.
     */
    public Optional<Escenario> tomar(String modulo, int nivel) {
        if (!activa() || modulo == null || !modulo.startsWith("docs_")) {
            return Optional.empty();
        }
        String clave = clave(modulo, nivel);
        Deque<Escenario> cola = pool.get(clave);
        Escenario escenario = cola != null ? cola.pollFirst() : null;
        programarRelleno(clave, modulo, nivel);
        return Optional.ofNullable(escenario);
    }

    private void programarRelleno(String clave, String modulo, int nivel) {
        int objetivo = propiedades.getPrecargaPorClave();
        if (objetivo <= 0 || tamano(clave) >= objetivo) {
            return;
        }
        Long hasta = enfriamiento.get(clave);
        if (hasta != null && System.currentTimeMillis() < hasta) {
            return;
        }
        if (!enCurso.add(clave)) {
            return;
        }
        try {
            ejecutor.submit(() -> rellenar(clave, modulo, nivel, objetivo));
        } catch (RejectedExecutionException ex) {
            enCurso.remove(clave);
        }
    }

    private void rellenar(String clave, String modulo, int nivel, int objetivo) {
        try {
            while (tamano(clave) < objetivo) {
                Escenario e = generador.generar(
                        Optional.of(modulo), nivel, Optional.empty(), Optional.empty());
                pool.computeIfAbsent(clave, k -> new ConcurrentLinkedDeque<>()).addLast(e);
            }
            enfriamiento.remove(clave);
        } catch (Exception ex) {
            enfriamiento.put(clave, System.currentTimeMillis() + ENFRIAMIENTO_FALLO_MS);
            log.warn("No se pudo pre-generar ejercicio para {}: {}", clave, ex.getMessage());
        } finally {
            enCurso.remove(clave);
        }
    }

    private int tamano(String clave) {
        Deque<Escenario> cola = pool.get(clave);
        return cola == null ? 0 : cola.size();
    }

    private static String clave(String modulo, int nivel) {
        return modulo + "|" + nivel;
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdownNow();
    }
}
