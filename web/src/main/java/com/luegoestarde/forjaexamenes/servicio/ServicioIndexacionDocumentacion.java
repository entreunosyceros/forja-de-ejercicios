// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.InterpretePython;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.evento.RecursosActualizadosEvent;
import com.luegoestarde.forjaexamenes.evento.RecursosActualizadosEvent.Tipo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ServicioIndexacionDocumentacion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ServicioIndexacionDocumentacion.class);

    public record ResultadoIndexacion(
            boolean exito,
            String mensaje,
            int colecciones,
            Instant instante) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ApplicationEventPublisher eventos;
    private final AtomicReference<ResultadoIndexacion> ultimoResultado = new AtomicReference<>(
            new ResultadoIndexacion(false, "Aún no se ha indexado la documentación.", 0, null));
    private final AtomicBoolean indexacionEnCurso = new AtomicBoolean(false);

    public ServicioIndexacionDocumentacion(
            PropiedadesForjaExamenes propiedades,
            ApplicationEventPublisher eventos) {
        this.propiedades = propiedades;
        this.eventos = eventos;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!propiedades.isAutoIndexarDocumentacion()) {
            log.info("Auto-indexación de documentacion/ desactivada (forjaexamenes.auto-indexar-documentacion=false)");
            return;
        }
        if (!hayPdfsEnDocumentacion()) {
            ultimoResultado.set(new ResultadoIndexacion(
                    true,
                    "Sin PDFs en documentacion/ — nada que indexar.",
                    0,
                    Instant.now()));
            log.info("documentacion/ sin PDFs; se omite la indexación al arranque.");
            return;
        }
        log.info("Auto-indexación de apuntes al arranque…");
        ResultadoIndexacion resultado = reindexar();
        if (resultado.exito()) {
            log.info("Indexación completada: {} colección(es). {}", resultado.colecciones(), resultado.mensaje());
        } else {
            log.warn("Indexación falló: {}", resultado.mensaje());
        }
    }

    public ResultadoIndexacion obtenerUltimoResultado() {
        return ultimoResultado.get();
    }

    public boolean indexacionEnCurso() {
        return indexacionEnCurso.get();
    }

    /**
     * Lanza la indexación en segundo plano (no bloquea la petición HTTP ni la sesión).
     *
     * @return {@code true} si se ha encolado; {@code false} si ya había una en curso.
     */
    public boolean solicitarReindexacionAsincrona() {
        if (!indexacionEnCurso.compareAndSet(false, true)) {
            return false;
        }
        reindexarEnSegundoPlano();
        return true;
    }

    @Async
    void reindexarEnSegundoPlano() {
        try {
            reindexar();
        } finally {
            indexacionEnCurso.set(false);
        }
    }

    /**
     * Ejecuta indexador_docs.py (idempotente; seguro llamar varias veces).
     */
    public synchronized ResultadoIndexacion reindexar() {
        Path script = Path.of(propiedades.getScriptIndexador()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(script)) {
            ResultadoIndexacion r = new ResultadoIndexacion(
                    false,
                    "No se encuentra indexador_docs.py en: " + script,
                    0,
                    Instant.now());
            ultimoResultado.set(r);
            return r;
        }

        if (!hayPdfsEnDocumentacion()) {
            ResultadoIndexacion r = new ResultadoIndexacion(
                    true,
                    "No hay PDFs en documentacion/. Añade carpetas con apuntes y vuelve a indexar.",
                    0,
                    Instant.now());
            ultimoResultado.set(r);
            eventos.publishEvent(new RecursosActualizadosEvent(this, Tipo.INDICE_DOCUMENTACION));
            return r;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    InterpretePython.resolver(propiedades),
                    script.toString());
            pb.directory(script.getParent().toFile());
            pb.redirectErrorStream(true);

            EjecutorProcesoPython.Resultado res = EjecutorProcesoPython.ejecutar(
                    pb, propiedades.getTimeoutIndexadorSegundos());
            int codigo = res.codigo();
            int colecciones = contarIndicesGenerados();
            Instant ahora = Instant.now();

            if (codigo != 0) {
                ResultadoIndexacion r = new ResultadoIndexacion(
                        false,
                        "indexador_docs.py falló (código " + codigo + "): " + resumirSalida(res.salida()),
                        colecciones,
                        ahora);
                ultimoResultado.set(r);
                return r;
            }

            String mensaje = colecciones > 0
                    ? "Indexados " + colecciones + " tema(s) desde documentacion/."
                    : "Indexación terminada sin colecciones (revisa los PDFs).";
            ResultadoIndexacion r = new ResultadoIndexacion(true, mensaje, colecciones, ahora);
            ultimoResultado.set(r);
            eventos.publishEvent(new RecursosActualizadosEvent(this, Tipo.INDICE_DOCUMENTACION));
            return r;
        } catch (Exception e) {
            ResultadoIndexacion r = new ResultadoIndexacion(
                    false,
                    "Error al indexar: " + e.getMessage(),
                    0,
                    Instant.now());
            ultimoResultado.set(r);
            return r;
        }
    }

    private int contarIndicesGenerados() throws Exception {
        Path indice = Path.of(propiedades.getDirectorioIndice()).toAbsolutePath().normalize();
        if (!Files.isDirectory(indice)) {
            return 0;
        }
        try (Stream<Path> archivos = Files.list(indice)) {
            return (int) archivos
                    .filter(p -> p.getFileName().toString().startsWith("docs_")
                            && p.getFileName().toString().endsWith(".json"))
                    .count();
        }
    }

    private boolean hayPdfsEnDocumentacion() {
        Path doc = Path.of(propiedades.getDirectorioDocumentacion()).toAbsolutePath().normalize();
        if (!Files.isDirectory(doc)) {
            return false;
        }
        try (Stream<Path> pdfs = Files.walk(doc)) {
            return pdfs.anyMatch(p -> Files.isRegularFile(p)
                    && p.getFileName().toString().toLowerCase().endsWith(".pdf"));
        } catch (Exception e) {
            return false;
        }
    }

    private static String resumirSalida(String salida) {
        if (salida == null || salida.isBlank()) {
            return "(sin salida)";
        }
        String limpio = salida.strip();
        return limpio.length() > 1200 ? limpio.substring(0, 1200) + "…" : limpio;
    }
}
