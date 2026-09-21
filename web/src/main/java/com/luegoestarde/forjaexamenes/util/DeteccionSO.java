// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import java.util.Locale;

/** Detección básica del sistema operativo (escritorio / scripts). */
public final class DeteccionSO {

    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    private DeteccionSO() {}

    public static String nombre() {
        return OS;
    }

    public static boolean esWindows() {
        return OS.contains("win");
    }

    public static boolean esMac() {
        return OS.contains("mac");
    }

    public static boolean pareceEscritorioGrafico() {
        if (esWindows() || esMac()) {
            return true;
        }
        return System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null;
    }
}
