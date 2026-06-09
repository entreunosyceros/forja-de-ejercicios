// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * Fábrica de {@link ObjectMapper} con la convención snake_case que comparten los
 * scripts Python (campos como {@code solucion_referencia}). Evita repetir la
 * configuración en cada servicio.
 */
public final class MapeadorJson {

    private MapeadorJson() {}

    /** Nuevo mapeador configurado para nombres snake_case. */
    public static ObjectMapper snakeCase() {
        ObjectMapper mapeador = new ObjectMapper();
        mapeador.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapeador;
    }
}
