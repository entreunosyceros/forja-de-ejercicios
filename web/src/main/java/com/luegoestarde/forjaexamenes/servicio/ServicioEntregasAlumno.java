// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno.AlumnoInfo;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario.EstadisticasModulo;
import com.luegoestarde.forjaexamenes.util.FechasForja;
import com.luegoestarde.forjaexamenes.util.RutasUsuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ServicioEntregasAlumno {

    private static final int MAX_NOMBRE_ETIQUETA = 80;

    public record ResumenEntregaImportada(
            String id,
            String nombreEtiqueta,
            String loginAlumno,
            String nombreAlumno,
            int totalIntentos,
            int totalAprobados,
            double notaMedia,
            double mejorNota,
            int modulosDistintos,
            String ultimaActividad,
            String importadoEn,
            boolean tieneProgresoLocal) {}

    public record FilaComparativaAlumno(
            String id,
            String nombreEtiqueta,
            String loginAlumno,
            int totalIntentos,
            int totalAprobados,
            double notaMedia,
            double mejorNota,
            int modulosDistintos,
            Map<String, Double> notaMediaPorModulo,
            List<Double> ultimasNotas) {}

    public record DatosComparativaClase(
            List<FilaComparativaAlumno> alumnos,
            List<String> modulos) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioHistorialIntentos servicioHistorial;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioEntregasAlumno(
            PropiedadesForjaExamenes propiedades,
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioHistorialIntentos servicioHistorial) {
        this.propiedades = propiedades;
        this.servicioEstadisticas = servicioEstadisticas;
        this.servicioHistorial = servicioHistorial;
    }

    public EntregaAlumno construirExportacion(String login, String nombreVisible) {
        EntregaAlumno entrega = new EntregaAlumno();
        entrega.setGenerado(Instant.now().toString());
        AlumnoInfo alumno = new AlumnoInfo();
        alumno.setLogin(login != null ? login : "");
        alumno.setNombreVisible(nombreVisible != null ? nombreVisible : login);
        entrega.setAlumno(alumno);
        entrega.setEstadisticasServidor(servicioEstadisticas.obtener(login));
        entrega.setHistorialIntentos(servicioHistorial.listarIntentos(login));
        return entrega;
    }

    public enum TipoImportacion { NUEVA, ACTUALIZADA, IGNORADA }

    public record ResultadoImportacion(TipoImportacion tipo, EntregaAlumno entrega) {}

    public ResultadoImportacion importar(
            String profesorLogin, byte[] contenido, String nombreEtiqueta)
            throws IOException, ConflictoImportacionEntregaException {
        return importar(profesorLogin, contenido, nombreEtiqueta, false);
    }

    public synchronized ResultadoImportacion importar(
            String profesorLogin,
            byte[] contenido,
            String nombreEtiqueta,
            boolean sobrescribirSiExiste) throws IOException, ConflictoImportacionEntregaException {
        if (profesorLogin == null || profesorLogin.isBlank()) {
            throw new IOException("Sesión de profesor no válida.");
        }
        if (contenido == null || contenido.length == 0) {
            throw new IOException("El fichero está vacío.");
        }
        if (contenido.length > 2 * 1024 * 1024) {
            throw new IOException("El fichero supera el límite de 2 MB.");
        }

        JsonNode raiz;
        try {
            raiz = mapeador.readTree(contenido);
        } catch (JsonProcessingException ex) {
            throw new IOException("Archivo inválido: el JSON está mal formado o corrupto.");
        }
        EntregaAlumno entrega;
        try {
            entrega = normalizarEntrada(raiz);
        } catch (JsonProcessingException ex) {
            throw new IOException("Archivo inválido: revisa el formato de la entrega del alumno.");
        }

        String loginAlumno = entrega.getAlumno() != null ? entrega.getAlumno().getLogin() : "";
        EntregaAlumno existente = buscarPorLoginAlumno(profesorLogin, loginAlumno);

        TipoImportacion tipo;
        String id;
        if (existente != null) {
            if (!sobrescribirSiExiste) {
                throw new ConflictoImportacionEntregaException(
                        loginAlumno,
                        nombreEtiqueta(existente),
                        existente.getImportadoEn(),
                        existente.getIdImportacion());
            }
            id = existente.getIdImportacion();
            tipo = TipoImportacion.ACTUALIZADA;
        } else {
            id = generarId(loginAlumno);
            tipo = TipoImportacion.NUEVA;
        }

        entrega.setIdImportacion(id);
        entrega.setImportadoPor(profesorLogin);
        entrega.setImportadoEn(FechasForja.ahora());
        entrega.setNombreEtiqueta(normalizarNombreEtiqueta(nombreEtiqueta, entrega));

        Path fichero = ficheroImportada(profesorLogin, id);
        Files.createDirectories(fichero.getParent());
        mapeador.writerWithDefaultPrettyPrinter().writeValue(fichero.toFile(), entrega);
        return new ResultadoImportacion(tipo, entrega);
    }

    public EntregaAlumno buscarPorLoginAlumno(String profesorLogin, String loginAlumno) throws IOException {
        if (loginAlumno == null || loginAlumno.isBlank()) {
            return null;
        }
        String normalizado = loginAlumno.strip().toLowerCase(Locale.ROOT);
        return listarEntregasCompletas(profesorLogin).stream()
                .filter(e -> e.getAlumno() != null
                        && e.getAlumno().getLogin() != null
                        && normalizado.equals(e.getAlumno().getLogin().strip().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    public List<EntregaAlumno> listarEntregasCompletas(String profesorLogin) throws IOException {
        return listarEntregasCompletasInternas(profesorLogin);
    }

    public synchronized void renombrar(String profesorLogin, String id, String nombreEtiqueta) throws IOException {
        EntregaAlumno entrega = obtenerImportada(profesorLogin, id);
        entrega.setNombreEtiqueta(normalizarNombreEtiqueta(nombreEtiqueta, entrega));
        guardarImportada(profesorLogin, entrega);
    }

    public List<ResumenEntregaImportada> listarImportadas(String profesorLogin) throws IOException {
        return listarEntregasCompletas(profesorLogin).stream()
                .map(this::aResumen)
                .sorted(Comparator.comparing(ResumenEntregaImportada::nombreEtiqueta, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public DatosComparativaClase construirComparativa(String profesorLogin) throws IOException {
        List<EntregaAlumno> entregas = listarEntregasCompletas(profesorLogin);
        Set<String> modulos = new LinkedHashSet<>();
        List<FilaComparativaAlumno> filas = new ArrayList<>();

        for (EntregaAlumno entrega : entregas) {
            EstadisticasUsuario stats = entrega.getEstadisticasServidor();
            Map<String, Double> porModulo = new LinkedHashMap<>();
            if (stats.getPorModulo() != null) {
                stats.getPorModulo().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            modulos.add(entry.getKey());
                            EstadisticasModulo pm = entry.getValue();
                            porModulo.put(entry.getKey(), pm != null ? pm.getNotaMedia() : 0.0);
                        });
            }

            List<Double> ultimasNotas = new ArrayList<>();
            if (stats.getUltimosIntentos() != null && !stats.getUltimosIntentos().isEmpty()) {
                var copia = new ArrayList<>(stats.getUltimosIntentos());
                Collections.reverse(copia);
                copia.forEach(i -> ultimasNotas.add(i.getNota()));
            }

            filas.add(new FilaComparativaAlumno(
                    entrega.getIdImportacion(),
                    nombreEtiqueta(entrega),
                    entrega.getAlumno().getLogin(),
                    stats.getTotalIntentos(),
                    stats.getTotalAprobados(),
                    stats.getNotaMedia(),
                    stats.getMejorNota(),
                    stats.getPorModulo() != null ? stats.getPorModulo().size() : 0,
                    porModulo,
                    ultimasNotas));
        }

        return new DatosComparativaClase(filas, List.copyOf(modulos));
    }

    public EntregaAlumno obtenerImportada(String profesorLogin, String id) throws IOException {
        Path fichero = ficheroImportada(profesorLogin, id);
        if (!Files.isRegularFile(fichero)) {
            throw new IOException("Entrega no encontrada: " + id);
        }
        return mapeador.readValue(fichero.toFile(), EntregaAlumno.class);
    }

    public synchronized boolean eliminarImportada(String profesorLogin, String id) throws IOException {
        return Files.deleteIfExists(ficheroImportada(profesorLogin, id));
    }

    public synchronized int eliminarTodasImportadas(String profesorLogin) throws IOException {
        Path carpeta = carpetaImportadas(profesorLogin);
        if (!Files.isDirectory(carpeta)) {
            return 0;
        }
        int borradas = 0;
        try (var stream = Files.list(carpeta)) {
            for (Path fichero : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
                if (Files.deleteIfExists(fichero)) {
                    borradas++;
                }
            }
        }
        return borradas;
    }

    private List<EntregaAlumno> listarEntregasCompletasInternas(String profesorLogin) throws IOException {
        Path carpeta = carpetaImportadas(profesorLogin);
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        List<EntregaAlumno> lista = new ArrayList<>();
        try (var stream = Files.list(carpeta)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .forEach(fichero -> {
                        try {
                            lista.add(mapeador.readValue(fichero.toFile(), EntregaAlumno.class));
                        } catch (IOException ignored) {
                            // omitir ficheros corruptos
                        }
                    });
        }
        lista.sort(Comparator.comparing(EntregaAlumno::getImportadoEn).reversed());
        return lista;
    }

    private ResumenEntregaImportada aResumen(EntregaAlumno entrega) {
        EstadisticasUsuario stats = entrega.getEstadisticasServidor();
        return new ResumenEntregaImportada(
                entrega.getIdImportacion(),
                nombreEtiqueta(entrega),
                entrega.getAlumno().getLogin(),
                nombreAlumnoFichero(entrega),
                stats.getTotalIntentos(),
                stats.getTotalAprobados(),
                stats.getNotaMedia(),
                stats.getMejorNota(),
                stats.getPorModulo() != null ? stats.getPorModulo().size() : 0,
                stats.getUltimaActividad(),
                entrega.getImportadoEn(),
                entrega.getProgresoLocal() != null && !entrega.getProgresoLocal().isNull());
    }

    private void guardarImportada(String profesorLogin, EntregaAlumno entrega) throws IOException {
        Path fichero = ficheroImportada(profesorLogin, entrega.getIdImportacion());
        mapeador.writerWithDefaultPrettyPrinter().writeValue(fichero.toFile(), entrega);
    }

    private EntregaAlumno normalizarEntrada(JsonNode raiz) throws IOException {
        if (raiz == null || raiz.isNull()) {
            throw new IOException("JSON no válido.");
        }

        if (raiz.has("formato") && EntregaAlumno.FORMATO.equals(raiz.get("formato").asText())) {
            EntregaAlumno entrega = mapeador.treeToValue(raiz, EntregaAlumno.class);
            if (entrega.getAlumno() == null) {
                entrega.setAlumno(new AlumnoInfo());
            }
            if (entrega.getEstadisticasServidor() == null) {
                entrega.setEstadisticasServidor(new EstadisticasUsuario());
            }
            return entrega;
        }

        if (raiz.has("totalIntentos") || raiz.has("porModulo")) {
            EstadisticasUsuario stats = mapeador.treeToValue(raiz, EstadisticasUsuario.class);
            EntregaAlumno entrega = new EntregaAlumno();
            entrega.setEstadisticasServidor(stats);
            return entrega;
        }

        throw new IOException(
                "Formato no reconocido. Usa una entrega exportada desde la portada (forja-entrega-alumno) "
                        + "o un fichero de estadísticas del servidor.");
    }

    private static String normalizarNombreEtiqueta(String nombreEtiqueta, EntregaAlumno entrega) throws IOException {
        String limpio = nombreEtiqueta != null ? nombreEtiqueta.strip() : "";
        if (limpio.isEmpty()) {
            limpio = nombreAlumnoFichero(entrega);
        }
        if (limpio.length() > MAX_NOMBRE_ETIQUETA) {
            limpio = limpio.substring(0, MAX_NOMBRE_ETIQUETA);
        }
        if (limpio.isBlank()) {
            throw new IOException("Indica un nombre para identificar al alumno.");
        }
        return limpio;
    }

    private static String nombreEtiqueta(EntregaAlumno entrega) {
        if (entrega.getNombreEtiqueta() != null && !entrega.getNombreEtiqueta().isBlank()) {
            return entrega.getNombreEtiqueta();
        }
        return nombreAlumnoFichero(entrega);
    }

    private static String nombreAlumnoFichero(EntregaAlumno entrega) {
        AlumnoInfo alumno = entrega.getAlumno();
        if (alumno == null) {
            return "Alumno";
        }
        if (alumno.getNombreVisible() != null && !alumno.getNombreVisible().isBlank()) {
            return alumno.getNombreVisible();
        }
        if (alumno.getLogin() != null && !alumno.getLogin().isBlank()) {
            return alumno.getLogin();
        }
        return "Alumno";
    }

    private static String generarId(String loginAlumno) {
        String base = loginAlumno != null
                ? loginAlumno.replaceAll("[^a-z0-9_-]", "").toLowerCase(Locale.ROOT)
                : "alumno";
        if (base.isBlank()) {
            base = "alumno";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Path carpetaImportadas(String profesorLogin) {
        return Path.of(propiedades.getDirectorioDatos())
                .toAbsolutePath()
                .normalize()
                .resolve("entregas")
                .resolve(RutasUsuario.sanitizarLogin(profesorLogin));
    }

    private Path ficheroImportada(String profesorLogin, String id) {
        return carpetaImportadas(profesorLogin).resolve(RutasUsuario.sanitizarLogin(id) + ".json");
    }
}
