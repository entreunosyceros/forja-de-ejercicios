package com.luegoestarde.forjaexamenes.evento;

import org.springframework.context.ApplicationEvent;

/** Se publica cuando cambian ficheros en disco (banco o índice de apuntes). */
public class RecursosActualizadosEvent extends ApplicationEvent {

    public enum Tipo {
        CATALOGO_BANCO,
        INDICE_DOCUMENTACION
    }

    private final Tipo tipo;

    public RecursosActualizadosEvent(Object origen, Tipo tipo) {
        super(origen);
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }
}
