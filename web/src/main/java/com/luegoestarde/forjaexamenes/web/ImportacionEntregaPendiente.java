// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import java.io.Serializable;

/** Datos de una importación en espera de confirmación de sobrescritura. */
public class ImportacionEntregaPendiente implements Serializable {

    private final byte[] contenido;
    private final String nombreEtiqueta;
    private final String loginAlumno;
    private final String nombreEtiquetaExistente;
    private final String importadoEn;
    private final String idExistente;

    public ImportacionEntregaPendiente(
            byte[] contenido,
            String nombreEtiqueta,
            String loginAlumno,
            String nombreEtiquetaExistente,
            String importadoEn,
            String idExistente) {
        this.contenido = contenido;
        this.nombreEtiqueta = nombreEtiqueta;
        this.loginAlumno = loginAlumno;
        this.nombreEtiquetaExistente = nombreEtiquetaExistente;
        this.importadoEn = importadoEn;
        this.idExistente = idExistente;
    }

    public byte[] getContenido() { return contenido; }

    public String getNombreEtiqueta() { return nombreEtiqueta; }

    public String getLoginAlumno() { return loginAlumno; }

    public String getNombreEtiquetaExistente() { return nombreEtiquetaExistente; }

    public String getImportadoEn() { return importadoEn; }

    public String getIdExistente() { return idExistente; }
}
