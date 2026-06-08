// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

/** El profesor debe confirmar si sobrescribe una entrega previa del mismo alumno. */
public class ConflictoImportacionEntregaException extends Exception {

    private final String loginAlumno;
    private final String nombreEtiquetaExistente;
    private final String importadoEn;
    private final String idExistente;

    public ConflictoImportacionEntregaException(
            String loginAlumno,
            String nombreEtiquetaExistente,
            String importadoEn,
            String idExistente) {
        super("Ya existe una entrega para este alumno.");
        this.loginAlumno = loginAlumno;
        this.nombreEtiquetaExistente = nombreEtiquetaExistente;
        this.importadoEn = importadoEn;
        this.idExistente = idExistente;
    }

    public String getLoginAlumno() { return loginAlumno; }

    public String getNombreEtiquetaExistente() { return nombreEtiquetaExistente; }

    public String getImportadoEn() { return importadoEn; }

    public String getIdExistente() { return idExistente; }
}
