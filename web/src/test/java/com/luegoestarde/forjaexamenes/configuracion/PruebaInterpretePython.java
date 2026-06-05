package com.luegoestarde.forjaexamenes.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PruebaInterpretePython {

    @Test
    void usaValorConfigurado() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        props.setPythonInterprete("/usr/bin/python3.12");
        assertEquals("/usr/bin/python3.12", InterpretePython.resolver(props));
    }

    @Test
    void porDefectoSegunSistemaSiVacio() {
        PropiedadesForjaExamenes props = new PropiedadesForjaExamenes();
        String esperado = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "python"
                : "python3";
        assertEquals(esperado, InterpretePython.resolver(props));
    }
}
