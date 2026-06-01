package com.luegoestarde.forjaexamenes.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forjaexamenes")
public class PropiedadesForjaExamenes {

    private String raiz = "..";
    private String directorioExamenes = "../examenes";
    private String scriptGenerador = "../generador.py";
    private String scriptEvaluador = "../evaluador.py";
    private boolean modoProfesor = false;

    public String getRaiz() { return raiz; }
    public void setRaiz(String raiz) { this.raiz = raiz; }

    public String getDirectorioExamenes() { return directorioExamenes; }
    public void setDirectorioExamenes(String directorioExamenes) { this.directorioExamenes = directorioExamenes; }

    public String getScriptGenerador() { return scriptGenerador; }
    public void setScriptGenerador(String scriptGenerador) { this.scriptGenerador = scriptGenerador; }

    public String getScriptEvaluador() { return scriptEvaluador; }
    public void setScriptEvaluador(String scriptEvaluador) { this.scriptEvaluador = scriptEvaluador; }

    public boolean isModoProfesor() { return modoProfesor; }
    public void setModoProfesor(boolean modoProfesor) { this.modoProfesor = modoProfesor; }
}
