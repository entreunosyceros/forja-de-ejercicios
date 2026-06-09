// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Zona horaria y formato de fecha/hora comunes a toda la aplicación.
 * Centraliza lo que antes estaba repetido en varios servicios.
 */
public final class FechasForja {

    public static final ZoneId ZONA = ZoneId.of("Europe/Madrid");
    public static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private FechasForja() {}

    /** Fecha y hora actuales en la zona de la aplicación, ya formateadas. */
    public static String ahora() {
        return LocalDateTime.now(ZONA).format(FORMATO_FECHA_HORA);
    }

    /** Momento actual en la zona de la aplicación (sin formatear). */
    public static LocalDateTime ahoraLocal() {
        return LocalDateTime.now(ZONA);
    }

    /** Interpreta una fecha en formato {@code dd/MM/yyyy HH:mm}. */
    public static LocalDateTime parsear(String fechaTexto) {
        return LocalDateTime.parse(fechaTexto.strip(), FORMATO_FECHA_HORA);
    }
}
