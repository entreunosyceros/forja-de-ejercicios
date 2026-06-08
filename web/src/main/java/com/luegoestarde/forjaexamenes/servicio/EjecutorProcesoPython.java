package com.luegoestarde.forjaexamenes.servicio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta procesos Python drenando la salida en un hilo aparte (para que no se
 * bloquee si llena el buffer de la tubería) y aplicando un timeout máximo.
 */
final class EjecutorProcesoPython {

    record Resultado(int codigo, String salida) {}

    private EjecutorProcesoPython() {}

    /**
     * Lanza el proceso descrito por {@code constructor}, recoge su salida combinada
     * (requiere {@code redirectErrorStream(true)}) y espera como máximo
     * {@code timeoutSegundos}. Si se supera, mata el proceso y lanza excepción.
     */
    static Resultado ejecutar(ProcessBuilder constructor, long timeoutSegundos)
            throws IOException, InterruptedException {
        Process proceso = constructor.start();
        StringBuilder salida = new StringBuilder();

        Thread lector = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = r.readLine()) != null) {
                    salida.append(linea).append('\n');
                }
            } catch (IOException ignorado) {
                // El proceso se cerró o fue destruido; la salida parcial basta.
            }
        }, "lector-python");
        lector.setDaemon(true);
        lector.start();

        boolean termino = timeoutSegundos > 0
                ? proceso.waitFor(timeoutSegundos, TimeUnit.SECONDS)
                : esperarSinLimite(proceso);

        if (!termino) {
            proceso.destroyForcibly();
            lector.interrupt();
            throw new IllegalStateException(
                    "El proceso Python superó el tiempo máximo (" + timeoutSegundos
                            + " s) y se ha cancelado. Reintenta o revisa la configuración.");
        }

        // Garantiza visibilidad de la salida acumulada por el hilo lector.
        lector.join(TimeUnit.SECONDS.toMillis(5));
        return new Resultado(proceso.exitValue(), salida.toString());
    }

    private static boolean esperarSinLimite(Process proceso) throws InterruptedException {
        proceso.waitFor();
        return true;
    }
}
