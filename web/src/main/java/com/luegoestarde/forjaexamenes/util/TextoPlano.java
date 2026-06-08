// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import java.util.regex.Pattern;

/** Quita markdown para PDF y vistas legibles (comandos listos para comparar). */
public final class TextoPlano {

    private static final Pattern BLOQUE_CODIGO = Pattern.compile("```[\\w.-]*\\n?([\\s\\S]*?)```");
    private static final Pattern CODIGO_INLINE = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern CABECERAS = Pattern.compile("(?m)^#{1,6}\\s+");
    private static final Pattern NEGRITA = Pattern.compile("\\*\\*([^*\\n]+)\\*\\*");
    private static final Pattern CURSIVA_AST = Pattern.compile("\\*([^*\\n]+)\\*");
    private static final Pattern NEGRITA_GUION = Pattern.compile("__([^_\\n]+)__");
    private static final Pattern CURSIVA_GUION = Pattern.compile("_([^_\\n]+)_");
    private static final Pattern ENLACES = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)");
    private static final Pattern CITA = Pattern.compile("(?m)^>\\s?");
    private static final Pattern HR = Pattern.compile("(?m)^[-*]{3,}\\s*$");
    private static final Pattern LISTA = Pattern.compile("(?m)^[\\*\\-\\+]\\s+");
    private static final Pattern LISTA_NUM = Pattern.compile("(?m)^\\d+\\.\\s+");
    private static final Pattern LINEAS_VACIAS = Pattern.compile("\\n{3,}");

    private TextoPlano() {}

    public static String sinMarkdown(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String t = texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        t = BLOQUE_CODIGO.matcher(t).replaceAll(m -> {
            String cuerpo = m.group(1).strip();
            return cuerpo.isEmpty() ? "" : cuerpo + "\n";
        });
        t = CODIGO_INLINE.matcher(t).replaceAll("$1");
        t = CABECERAS.matcher(t).replaceAll("");
        t = NEGRITA.matcher(t).replaceAll("$1");
        t = CURSIVA_AST.matcher(t).replaceAll("$1");
        t = NEGRITA_GUION.matcher(t).replaceAll("$1");
        t = CURSIVA_GUION.matcher(t).replaceAll("$1");
        t = ENLACES.matcher(t).replaceAll("$1");
        t = CITA.matcher(t).replaceAll("");
        t = HR.matcher(t).replaceAll("");
        t = LISTA.matcher(t).replaceAll("");
        t = LISTA_NUM.matcher(t).replaceAll("");
        t = LINEAS_VACIAS.matcher(t).replaceAll("\n\n");
        return t.strip();
    }
}
