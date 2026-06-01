package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class ServicioPdf {

    private static final float MARGEN = 50;
    private static final float ALTURA_LINEA = 14;

    private final PropiedadesForjaExamenes propiedades;

    public ServicioPdf(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public Path generarPdf(Escenario escenario, ResultadoEvaluacion resultado) throws IOException {
        Path directorio = Path.of(propiedades.getDirectorioExamenes()).toAbsolutePath().normalize();
        Files.createDirectories(directorio);

        String marcaTiempo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path rutaPdf = directorio.resolve("ejercicio-" + escenario.getId() + "-" + marcaTiempo + ".pdf");

        try (PDDocument documento = new PDDocument()) {
            agregarPaginaTexto(documento, "EJERCICIO — Forja de ejercicios / luego es tarde para estudiar",
                    "[" + escenario.getModulo().toUpperCase() + "] " + escenario.getTitulo(),
                    escenario.getEnunciado() + "\n\n[ID: " + escenario.getId() + "]");

            if (resultado != null) {
                String titulo = "Nota: " + resultado.getNota() + "/10 — "
                        + (resultado.isAprobado() ? "APROBADO" : "SUSPENSO");
                agregarPaginaTexto(documento, "CORRECCION — Forja de ejercicios", titulo,
                        construirCuerpoCorreccion(resultado));
            } else {
                agregarPaginaTexto(documento, "SOLUCION — Forja de ejercicios (solo profesor)",
                        escenario.getTitulo(),
                        escenario.getSolucionReferencia());
            }

            documento.save(rutaPdf.toFile());
        }
        return rutaPdf;
    }

    private String construirCuerpoCorreccion(ResultadoEvaluacion resultado) {
        StringBuilder texto = new StringBuilder();
        texto.append(resultado.getRetroalimentacion()).append("\n\n");
        texto.append("Respuesta del alumno:\n").append(seguroNulo(resultado.getRespuestaAlumno())).append("\n\n");
        if (resultado.getDetalles() != null) {
            texto.append("Criterios:\n");
            for (var detalle : resultado.getDetalles()) {
                texto.append(detalle.isCumplido() ? "[OK] " : "[X] ");
                texto.append(detalle.getPatron()).append('\n');
                if (detalle.getEsperado() != null) {
                    texto.append("  Esperado: ").append(detalle.getEsperado()).append('\n');
                }
                if (!detalle.isCumplido() && detalle.getPista() != null) {
                    texto.append("  Pista: ").append(detalle.getPista()).append('\n');
                }
            }
        }
        texto.append("\nSolucion de referencia:\n").append(seguroNulo(resultado.getSolucionReferencia()));
        return texto.toString();
    }

    private void agregarPaginaTexto(PDDocument documento, String cabecera, String titulo, String cuerpo)
            throws IOException {
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        float y = pagina.getMediaBox().getHeight() - MARGEN;

        try (PDPageContentStream flujo = new PDPageContentStream(documento, pagina)) {
            y = escribirLinea(flujo, negrita, 10, MARGEN, y, sanitizar(cabecera));
            y -= ALTURA_LINEA;
            for (String linea : dividirLineas(sanitizar(titulo), 72)) {
                y = escribirLinea(flujo, negrita, 13, MARGEN, y, linea);
            }
            y -= ALTURA_LINEA / 2;
            for (String linea : dividirLineas(sanitizar(cuerpo), 88)) {
                if (y < MARGEN) break;
                y = escribirLinea(flujo, regular, 10, MARGEN, y, linea);
            }
        }
    }

    private float escribirLinea(PDPageContentStream flujo, PDType1Font fuente, float tamano,
                                float x, float y, String texto) throws IOException {
        flujo.beginText();
        flujo.setFont(fuente, tamano);
        flujo.newLineAtOffset(x, y);
        flujo.showText(texto);
        flujo.endText();
        return y - ALTURA_LINEA;
    }

    private static String seguroNulo(String cadena) {
        return cadena == null ? "" : cadena;
    }

    private static String sanitizar(String texto) {
        if (texto == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == '\n' || c == '\r' || (c >= 32 && c <= 126) || c >= 160) {
                sb.append(c == '\r' ? ' ' : c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static List<String> dividirLineas(String texto, int ancho) {
        List<String> lineas = new ArrayList<>();
        for (String parrafo : texto.split("\n", -1)) {
            String restante = parrafo;
            while (restante.length() > ancho) {
                int corte = restante.lastIndexOf(' ', ancho);
                if (corte <= 0) corte = ancho;
                lineas.add(restante.substring(0, corte).trim());
                restante = restante.substring(corte).trim();
            }
            lineas.add(restante);
        }
        return lineas;
    }
}
