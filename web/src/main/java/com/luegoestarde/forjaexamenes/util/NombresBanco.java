package com.luegoestarde.forjaexamenes.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * Nombres de fichero seguros para ejercicios en {@code banco/aprobados/} y {@code banco/pendientes/}.
 */
public final class NombresBanco {

    private static final int LONGITUD_MAXIMA = 80;

    private NombresBanco() {
    }

    public static String sanitizarNombreArchivo(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(nombre.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > LONGITUD_MAXIMA) {
            slug = slug.substring(0, LONGITUD_MAXIMA).replaceAll("-+$", "");
        }
        return slug;
    }

    public static String resolverNombreArchivo(String nombreSolicitado, String titulo, String idSesion) {
        String slug = sanitizarNombreArchivo(nombreSolicitado);
        if (slug.isBlank()) {
            slug = sanitizarNombreArchivo(titulo);
        }
        if (slug.isBlank()) {
            slug = sanitizarNombreArchivo(idSesion);
        }
        if (slug.isBlank()) {
            slug = "ejercicio-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }
}
