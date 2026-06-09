// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PruebaInterpretePython {

    @Test
    void usaComandoConfiguradoSinRuta() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setPythonInterprete("python3");
        assertEquals("python3", InterpretePython.resolver(props));
    }

    @Test
    void porDefectoSegunSistemaSiVacio() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        String esperado = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "python"
                : "python3";
        assertEquals(esperado, InterpretePython.resolver(props));
    }

    @Test
    void rechazaRutaWindowsCorruptaPorProperties() {
        assertNull(InterpretePython.validarNormalizar("C:Program FilesPython313python.exe"));
    }

    @Test
    void aceptaRutaWindowsConBarrasNormales() {
        String python = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "C:/Windows/System32/cmd.exe"
                : "/bin/sh";
        assertEquals(python, InterpretePython.validarNormalizar(python));
    }
}
