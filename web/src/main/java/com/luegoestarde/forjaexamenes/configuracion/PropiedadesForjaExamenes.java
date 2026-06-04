package com.luegoestarde.forjaexamenes.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forjaexamenes")
public class PropiedadesForjaExamenes {

    private String raiz = "..";
    private String directorioExamenes = "../examenes";
    private String scriptGenerador = "../generador.py";
    private String scriptEvaluador = "../evaluador.py";
    private String directorioDocumentacion = "../documentacion";
    private String directorioIndice = "../indice";
    private String geminiApiKey = "";
    private String geminiModel = "gemini-2.5-flash";
    private String scriptIndexador = "../indexador_docs.py";
    private String directorioDatos = "../datos";
    private String directorioBanco = "../banco";
    private boolean autoIndexarDocumentacion = true;
    private boolean modoProfesor = false;
    private boolean geminiGuardarPendientes = false;
    private boolean geminiSoloAprobados = false;
    private int subidaPdfMaxMb = 30;

    public String getRaiz() { return raiz; }
    public void setRaiz(String raiz) { this.raiz = raiz; }

    public String getDirectorioExamenes() { return directorioExamenes; }
    public void setDirectorioExamenes(String directorioExamenes) { this.directorioExamenes = directorioExamenes; }

    public String getScriptGenerador() { return scriptGenerador; }
    public void setScriptGenerador(String scriptGenerador) { this.scriptGenerador = scriptGenerador; }

    public String getScriptEvaluador() { return scriptEvaluador; }
    public void setScriptEvaluador(String scriptEvaluador) { this.scriptEvaluador = scriptEvaluador; }

    public String getDirectorioDocumentacion() { return directorioDocumentacion; }
    public void setDirectorioDocumentacion(String directorioDocumentacion) {
        this.directorioDocumentacion = directorioDocumentacion;
    }

    public String getDirectorioIndice() { return directorioIndice; }
    public void setDirectorioIndice(String directorioIndice) { this.directorioIndice = directorioIndice; }

    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }

    public String getGeminiModel() { return geminiModel; }
    public void setGeminiModel(String geminiModel) { this.geminiModel = geminiModel; }

    public String getScriptIndexador() { return scriptIndexador; }
    public void setScriptIndexador(String scriptIndexador) { this.scriptIndexador = scriptIndexador; }

    public String getDirectorioDatos() { return directorioDatos; }
    public void setDirectorioDatos(String directorioDatos) { this.directorioDatos = directorioDatos; }

    public boolean isAutoIndexarDocumentacion() { return autoIndexarDocumentacion; }
    public void setAutoIndexarDocumentacion(boolean autoIndexarDocumentacion) {
        this.autoIndexarDocumentacion = autoIndexarDocumentacion;
    }

    public boolean isModoProfesor() { return modoProfesor; }
    public void setModoProfesor(boolean modoProfesor) { this.modoProfesor = modoProfesor; }

    public String getDirectorioBanco() { return directorioBanco; }
    public void setDirectorioBanco(String directorioBanco) { this.directorioBanco = directorioBanco; }

    public boolean isGeminiGuardarPendientes() { return geminiGuardarPendientes; }
    public void setGeminiGuardarPendientes(boolean geminiGuardarPendientes) {
        this.geminiGuardarPendientes = geminiGuardarPendientes;
    }

    public boolean isGeminiSoloAprobados() { return geminiSoloAprobados; }
    public void setGeminiSoloAprobados(boolean geminiSoloAprobados) {
        this.geminiSoloAprobados = geminiSoloAprobados;
    }

    public int getSubidaPdfMaxMb() { return subidaPdfMaxMb; }
    public void setSubidaPdfMaxMb(int subidaPdfMaxMb) { this.subidaPdfMaxMb = subidaPdfMaxMb; }

    public long getSubidaPdfMaxBytes() {
        return (long) subidaPdfMaxMb * 1024 * 1024;
    }
}
