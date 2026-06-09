// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import java.nio.file.Path;

/**
 * Resolución común de ficheros por usuario dentro del directorio de datos.
 * Centraliza la sanitización del login y el cálculo de la ruta, que antes
 * estaba repetido en varios servicios (estadísticas, historial, entregas).
 */
public final class RutasUsuario {

    private RutasUsuario() {}

    /** Deja solo caracteres seguros para usar el login como nombre de fichero. */
    public static String sanitizarLogin(String login) {
        if (login == null) {
            return "";
        }
        return login.replaceAll("[^a-z0-9_\\-]", "");
    }

    /**
     * Devuelve {@code <directorioDatos>/<subcarpeta>/<login>.json} con el login
     * sanitizado y la ruta normalizada (independiente del separador del SO).
     */
    public static Path ficheroJson(String directorioDatos, String subcarpeta, String login) {
        return Path.of(directorioDatos)
                .toAbsolutePath()
                .normalize()
                .resolve(subcarpeta)
                .resolve(sanitizarLogin(login) + ".json");
    }
}
