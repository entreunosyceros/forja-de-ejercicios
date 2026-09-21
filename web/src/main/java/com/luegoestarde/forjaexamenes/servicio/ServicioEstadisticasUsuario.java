// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario.EstadisticasModulo;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario.IntentoReciente;
import com.luegoestarde.forjaexamenes.util.EscrituraAtomica;
import com.luegoestarde.forjaexamenes.util.FechasForja;
import com.luegoestarde.forjaexamenes.util.RutasUsuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ServicioEstadisticasUsuario {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioEstadisticasUsuario.class);

    public record ResumenAlumnoServidor(
            String login,
            int totalIntentos,
            int totalAprobados,
            double notaMedia,
            String ultimaActividad,
            int modulosDistintos) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioEstadisticasUsuario(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public EstadisticasUsuario obtener(String login) {
        if (login == null || login.isBlank()) {
            return vacias();
        }
        Path fichero = ficheroEstadisticas(login);
        if (!Files.isRegularFile(fichero)) {
            return vacias();
        }
        try {
            return mapeador.readValue(fichero.toFile(), EstadisticasUsuario.class);
        } catch (IOException e) {
            LOG.warn("No se pudieron leer estadísticas de {}: {}", login, e.toString());
            return vacias();
        }
    }

    public List<ResumenAlumnoServidor> listarResumenesEnServidor(Predicate<String> incluirLogin)
            throws IOException {
        Path carpeta = Path.of(propiedades.getDirectorioDatos())
                .toAbsolutePath()
                .normalize()
                .resolve("estadisticas");
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        List<ResumenAlumnoServidor> lista = new ArrayList<>();
        try (var stream = Files.list(carpeta)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .forEach(fichero -> {
                        String login = fichero.getFileName().toString().replace(".json", "");
                        if (incluirLogin != null && !incluirLogin.test(login)) {
                            return;
                        }
                        try {
                            EstadisticasUsuario stats = mapeador.readValue(fichero.toFile(), EstadisticasUsuario.class);
                            lista.add(new ResumenAlumnoServidor(
                                    login,
                                    stats.getTotalIntentos(),
                                    stats.getTotalAprobados(),
                                    stats.getNotaMedia(),
                                    stats.getUltimaActividad(),
                                    stats.getPorModulo().size()));
                        } catch (IOException ex) {
                            LOG.warn("Estadísticas corruptas u omitidas: {} ({})",
                                    fichero.getFileName(), ex.toString());
                        }
                    });
        }
        lista.sort(Comparator
                .comparing(ResumenAlumnoServidor::ultimaActividad, Comparator.nullsLast(String::compareTo))
                .reversed()
                .thenComparing(r -> r.login().toLowerCase(Locale.ROOT)));
        return lista;
    }

    public synchronized void reiniciarRachaAprobados(String login) throws IOException {
        if (login == null || login.isBlank()) {
            return;
        }
        EstadisticasUsuario stats = obtener(login);
        stats.setRachaActual(0);
        guardar(login, stats);
    }

    public synchronized void reiniciarRachaSuspensos(String login) throws IOException {
        if (login == null || login.isBlank()) {
            return;
        }
        EstadisticasUsuario stats = obtener(login);
        stats.setRachaSuspensos(0);
        guardar(login, stats);
    }

    public synchronized boolean limpiar(String login) throws IOException {
        if (login == null || login.isBlank()) {
            return false;
        }
        Path fichero = ficheroEstadisticas(login);
        if (!Files.isRegularFile(fichero)) {
            return false;
        }
        Files.deleteIfExists(fichero);
        return true;
    }

    private static final int MAX_ULTIMOS_INTENTOS = 5;

    /**
     * Nivel efectivo 1–3. Sin dato (historial antiguo) se asume 2 (referencia).
     */
    public static int normalizarNivel(Integer dificultad) {
        if (dificultad == null) {
            return 2;
        }
        return Math.max(1, Math.min(3, dificultad));
    }

    /**
     * Factor de ponderación: nivel 2 = 1.0 (referencia), nivel 1 = 0.7, nivel 3 = 1.3.
     * Así un 10 fácil no pesa igual que un 10 exigente en la media.
     */
    public static double factorNivel(Integer dificultad) {
        return switch (normalizarNivel(dificultad)) {
            case 1 -> 0.7;
            case 3 -> 1.3;
            default -> 1.0;
        };
    }

    /** Nota comparable 0–10 tras aplicar el factor de nivel. */
    public static double notaPonderada(double nota, Integer dificultad) {
        double ponderada = nota * factorNivel(dificultad);
        return redondear(Math.min(10.0, Math.max(0.0, ponderada)));
    }

    public synchronized void registrar(
            String login, String modulo, double nota, boolean aprobado, Long tiempoSegundos)
            throws IOException {
        registrar(login, modulo, nota, aprobado, tiempoSegundos, null, null, null, null);
    }

    public synchronized void registrar(
            String login,
            String modulo,
            double nota,
            boolean aprobado,
            Long tiempoSegundos,
            String titulo,
            String ejercicioId) throws IOException {
        registrar(login, modulo, nota, aprobado, tiempoSegundos, titulo, ejercicioId, null, null);
    }

    /**
     * @param dificultad      nivel 1–3 del ejercicio corregido (puede ser null)
     * @param nivelUsuario    nivel actual del alumno (para rachas adaptativas; null = usar el del intento)
     */
    public synchronized void registrar(
            String login,
            String modulo,
            double nota,
            boolean aprobado,
            Long tiempoSegundos,
            String titulo,
            String ejercicioId,
            Integer dificultad,
            Integer nivelUsuario) throws IOException {
        if (login == null || login.isBlank() || modulo == null || modulo.isBlank()) {
            return;
        }

        int nivelEjercicio = normalizarNivel(dificultad);
        int nivelRef = nivelUsuario != null ? normalizarNivel(nivelUsuario) : nivelEjercicio;
        double notaComp = notaPonderada(nota, dificultad);

        EstadisticasUsuario stats = obtener(login);
        stats.setTotalIntentos(stats.getTotalIntentos() + 1);
        if (aprobado) {
            stats.setTotalAprobados(stats.getTotalAprobados() + 1);
            stats.setRachaSuspensos(0);
            if (nivelEjercicio >= nivelRef) {
                stats.setRachaActual(stats.getRachaActual() + 1);
            } else {
                // Aprobado en nivel más fácil: no infla la racha de subida.
                stats.setRachaActual(0);
            }
        } else {
            stats.setTotalSuspensos(stats.getTotalSuspensos() + 1);
            stats.setRachaActual(0);
            if (nivelEjercicio <= nivelRef) {
                stats.setRachaSuspensos(stats.getRachaSuspensos() + 1);
            } else {
                stats.setRachaSuspensos(0);
            }
        }

        double sumaPrev = stats.getNotaMedia() * (stats.getTotalIntentos() - 1);
        stats.setNotaMedia(redondear((sumaPrev + notaComp) / stats.getTotalIntentos()));
        if (notaComp > stats.getMejorNota()) {
            stats.setMejorNota(notaComp);
        }

        if (tiempoSegundos != null && tiempoSegundos > 0) {
            stats.setTiempoTotalSegundos(stats.getTiempoTotalSegundos() + tiempoSegundos);
        }
        stats.setUltimaActividad(FechasForja.ahora());

        EstadisticasModulo pm = stats.getPorModulo().computeIfAbsent(modulo, m -> new EstadisticasModulo());
        pm.setIntentos(pm.getIntentos() + 1);
        double sumaMod = pm.getNotaMedia() * (pm.getIntentos() - 1);
        pm.setNotaMedia(redondear((sumaMod + notaComp) / pm.getIntentos()));
        if (notaComp > pm.getMejorNota()) {
            pm.setMejorNota(notaComp);
        }
        if (aprobado) {
            pm.setAprobados(pm.getAprobados() + 1);
        }

        registrarIntentoReciente(
                stats, modulo, titulo, nota, aprobado, tiempoSegundos, ejercicioId, nivelEjercicio);
        guardar(login, stats);
    }

    private static void registrarIntentoReciente(
            EstadisticasUsuario stats,
            String modulo,
            String titulo,
            double nota,
            boolean aprobado,
            Long tiempoSegundos,
            String ejercicioId,
            int dificultad) {
        var intento = new IntentoReciente();
        intento.setModulo(modulo);
        intento.setTitulo(titulo != null && !titulo.isBlank() ? titulo.strip() : modulo);
        intento.setNota(nota);
        intento.setAprobado(aprobado);
        intento.setTiempoSegundos(tiempoSegundos != null ? tiempoSegundos : 0L);
        intento.setFecha(Instant.now().toString());
        intento.setEjercicioId(ejercicioId);
        intento.setDificultad(dificultad);

        var lista = stats.getUltimosIntentos() != null
                ? new ArrayList<>(stats.getUltimosIntentos())
                : new ArrayList<IntentoReciente>();
        if (ejercicioId != null && !ejercicioId.isBlank()) {
            lista.removeIf(i -> ejercicioId.equals(i.getEjercicioId()));
        }
        lista.add(0, intento);
        if (lista.size() > MAX_ULTIMOS_INTENTOS) {
            lista = new ArrayList<>(lista.subList(0, MAX_ULTIMOS_INTENTOS));
        }
        stats.setUltimosIntentos(lista);
    }

    public static String formatearTiempo(long segundos) {
        if (segundos <= 0) {
            return "0:00";
        }
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long resto = segundos % 60;
        if (horas > 0) {
            return String.format("%d:%02d:%02d", horas, minutos, resto);
        }
        return String.format("%d:%02d", minutos, resto);
    }

    private void guardar(String login, EstadisticasUsuario stats) throws IOException {
        Path fichero = ficheroEstadisticas(login);
        Files.createDirectories(fichero.getParent());
        EscrituraAtomica.json(mapeador, fichero, stats);
    }

    private Path ficheroEstadisticas(String login) {
        return RutasUsuario.ficheroJson(propiedades.getDirectorioDatos(), "estadisticas", login);
    }

    private static EstadisticasUsuario vacias() {
        return new EstadisticasUsuario();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
