package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AsesorModelo {

    private final PropiedadesForjaExamenes propiedades;

    public AsesorModelo(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    @ModelAttribute("modoProfesor")
    public boolean modoProfesor() {
        return propiedades.isModoProfesor();
    }
}
