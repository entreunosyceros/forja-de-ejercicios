// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServicioSubidaDocumentacion {

    private static final Pattern SLUG_VALIDO = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,48}$");
    private static final String TEMA_NUEVO = "__nuevo__";

    public record CarpetaTema(String slug, String tituloVisible, String moduloDocs, int pdfsEnDisco) {}

    public record ResultadoSubida(
            int guardados,
            List<String> nombres,
            List<String> errores,
            String carpetaDestino) {}

    private final PropiedadesForjaExamenes propiedades;

    public ServicioSubidaDocumentacion(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public Path directorioDocumentacion() {
        return Path.of(propiedades.getDirectorioDocumentacion()).toAbsolutePath().normalize();
    }

    public List<CarpetaTema> listarTemasEnDisco() throws IOException {
        Path raiz = directorioDocumentacion();
        Files.createDirectories(raiz);
        List<CarpetaTema> temas = new ArrayList<>();
        try (var stream = Files.list(raiz)) {
            stream.filter(Files::isDirectory).forEach(carpeta -> {
                try {
                    temas.add(toCarpetaTema(carpeta));
                } catch (IOException ignored) {
                    // omitir carpetas inaccesibles
                }
            });
        }
        temas.sort(Comparator.comparing(CarpetaTema::slug));
        return temas;
    }

    public ResultadoSubida guardarPdfs(
            String temaSeleccionado,
            String temaNuevo,
            String capitulo,
            MultipartFile[] archivos) throws IOException {
        if (archivos == null || archivos.length == 0) {
            throw new IllegalArgumentException("Selecciona al menos un archivo PDF.");
        }

        String slugTema = resolverSlugTema(temaSeleccionado, temaNuevo);
        String slugCapitulo = normalizarSlugOpcional(capitulo);

        Path destino = directorioDocumentacion().resolve(slugTema);
        if (slugCapitulo != null) {
            destino = destino.resolve(slugCapitulo);
        }
        Files.createDirectories(destino);

        List<String> guardados = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            try {
                validarPdf(archivo);
                String nombre = nombreSeguroPdf(archivo.getOriginalFilename());
                Path fichero = destino.resolve(nombre);
                if (Files.exists(fichero)) {
                    String base = nombre.replace(".pdf", "");
                    nombre = base + "-" + System.currentTimeMillis() % 100000 + ".pdf";
                    fichero = destino.resolve(nombre);
                }
                archivo.transferTo(fichero);
                guardados.add(nombre);
            } catch (Exception ex) {
                String etiqueta = archivo.getOriginalFilename() != null
                        ? archivo.getOriginalFilename()
                        : "(sin nombre)";
                errores.add(etiqueta + ": " + ex.getMessage());
            }
        }

        if (guardados.isEmpty()) {
            throw new IllegalArgumentException(
                    errores.isEmpty()
                            ? "No se guardó ningún PDF."
                            : String.join("; ", errores));
        }

        String rutaRelativa = slugTema + (slugCapitulo != null ? "/" + slugCapitulo : "");
        return new ResultadoSubida(guardados.size(), guardados, errores, rutaRelativa);
    }

    private void validarPdf(MultipartFile archivo) {
        long maxBytes = propiedades.getSubidaPdfMaxBytes();
        if (archivo.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "El archivo supera el límite de " + (maxBytes / 1024 / 1024) + " MB.");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se permiten ficheros PDF.");
        }
        String tipo = archivo.getContentType();
        if (tipo != null && !tipo.equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("Tipo de archivo no válido (se esperaba PDF).");
        }
    }

    private String resolverSlugTema(String temaSeleccionado, String temaNuevo) {
        String raw;
        if (TEMA_NUEVO.equals(temaSeleccionado)) {
            raw = temaNuevo;
        } else {
            raw = temaSeleccionado;
        }
        String slug = aSlug(raw);
        if (!SLUG_VALIDO.matcher(slug).matches()) {
            throw new IllegalArgumentException(
                    "Nombre de tema no válido. Usa letras, números y guiones (p. ej. docker, java_poo).");
        }
        Path destino = directorioDocumentacion().resolve(slug).normalize();
        if (!destino.startsWith(directorioDocumentacion())) {
            throw new IllegalArgumentException("Ruta de tema no permitida.");
        }
        return slug;
    }

    private String normalizarSlugOpcional(String capitulo) {
        if (capitulo == null || capitulo.isBlank()) {
            return null;
        }
        String slug = aSlug(capitulo);
        if (!SLUG_VALIDO.matcher(slug).matches()) {
            throw new IllegalArgumentException(
                    "Nombre de capítulo no válido. Usa letras, números y guiones.");
        }
        return slug;
    }

    private static String aSlug(String texto) {
        if (texto == null) {
            return "";
        }
        String slug = texto.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug;
    }

    private static String nombreSeguroPdf(String nombreOriginal) {
        String base = Path.of(nombreOriginal != null ? nombreOriginal : "apuntes.pdf")
                .getFileName()
                .toString();
        base = base.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        if (!base.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            base = base + ".pdf";
        }
        if (base.equals(".pdf") || base.isBlank()) {
            base = "apuntes.pdf";
        }
        return base;
    }

    private CarpetaTema toCarpetaTema(Path carpeta) throws IOException {
        String slug = carpeta.getFileName().toString();
        int pdfs;
        try (var walk = Files.walk(carpeta)) {
            pdfs = (int) walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .count();
        }
        String modulo = "docs_" + slug;
        String titulo = slug.replace("_", " ").replace("-", " ");
        if (!titulo.isBlank()) {
            titulo = titulo.substring(0, 1).toUpperCase(Locale.ROOT) + titulo.substring(1);
        }
        return new CarpetaTema(slug, titulo, modulo, pdfs);
    }
}
