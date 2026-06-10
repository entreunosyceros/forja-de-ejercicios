// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.modelo;

/** Preferencias del profesor guardadas en disco (datos/preferencias-profesor.json). */
public class PreferenciasProfesor {

    /** Si true, cada ejercicio docs_* generado se guarda en banco/pendientes/. */
    private boolean geminiGuardarPendientes;

    public boolean isGeminiGuardarPendientes() {
        return geminiGuardarPendientes;
    }

    public void setGeminiGuardarPendientes(boolean geminiGuardarPendientes) {
        this.geminiGuardarPendientes = geminiGuardarPendientes;
    }
}
