// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resolución común de ficheros por usuario dentro del directorio de datos.
 */
public final class RutasUsuario {

    private static final Pattern LOGIN_SEGURO = Pattern.compile("^[a-z0-9_\\-]{1,64}$");

    private RutasUsuario() {}

    /**
     * Normaliza el login a un nombre de fichero seguro.
     * Si el login original no es válido, usa un hash corto (no colapsa logins distintos).
     */
    public static String sanitizarLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login vacío.");
        }
        String normalizado = login.trim().toLowerCase(Locale.ROOT);
        if (LOGIN_SEGURO.matcher(normalizado).matches()) {
            return normalizado;
        }
        return "u_" + hashCorto(normalizado);
    }

    private static String hashCorto(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(texto.hashCode());
        }
    }

    public static Path ficheroJson(String directorioDatos, String subcarpeta, String login) {
        return Path.of(directorioDatos)
                .toAbsolutePath()
                .normalize()
                .resolve(subcarpeta)
                .resolve(sanitizarLogin(login) + ".json");
    }
}
