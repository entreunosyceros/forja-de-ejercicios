// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PruebaParametrosEjercicio {

    private final ParametrosEjercicioBean bean = new ParametrosEjercicioBean();

    @Test
    void origenApuntesDevuelveMapa() {
        Map<String, Object> origen = Map.of("capitulo_slug", "redes", "pagina", 12);
        Map<String, Object> params = Map.of("origen", origen);
        assertEquals(origen, bean.origenApuntes(params));
    }

    @Test
    void origenEnteroNoSeTrataComoApuntes() {
        Map<String, Object> params = Map.of("origen", 4521, "destino", 8899);
        assertNull(bean.origenApuntes(params));
    }
}
