// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Abre una terminal del sistema (CMD + .bat en Windows, Terminal.app en macOS, gnome-terminal…)
 * en la carpeta del proyecto con los comandos Docker de práctica.
 * Pensado para uso local; en servidores sin escritorio no habrá terminal gráfica.
 */
@Service
public class ServicioTerminalSistema {

    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    private static final String SCRIPT_DOCKER =
            "docker compose up -d --build practica && docker exec -it forjaexamenes-practica bash";

    /** Indica si parece haber terminal gráfica en este equipo. */
    public boolean disponible() {
        if (esWindows() || esMac()) {
            return true;
        }
        return System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null;
    }

    /** Etiqueta del botón según el sistema (consola, Terminal…). */
    public String etiquetaBoton() {
        if (esWindows()) {
            return "Abrir consola de práctica";
        }
        if (esMac()) {
            return "Abrir Terminal";
        }
        return "Abrir terminal";
    }

    /**
     * Lanza una terminal en {@code raizProyecto} con los comandos de práctica Docker.
     * @return mensaje descriptivo del resultado.
     */
    public String abrirTerminalPracticaDocker(Path raizProyecto) throws IOException {
        Path raiz = raizProyecto.toAbsolutePath().normalize();
        if (!Files.isDirectory(raiz)) {
            throw new IOException("No se encuentra la carpeta del proyecto: " + raiz);
        }
        Path compose = raiz.resolve("docker-compose.yml");
        if (!Files.isRegularFile(compose)) {
            throw new IOException(
                    "No se encuentra docker-compose.yml en " + raiz
                            + ". Arranca la terminal desde la carpeta examenforge.");
        }
        if (!disponible()) {
            throw new IllegalStateException(
                    "Este equipo no tiene terminal gráfica disponible (¿servidor sin escritorio?). "
                            + "Ejecuta manualmente en " + raiz + ":\n"
                            + SCRIPT_DOCKER);
        }

        List<String> comando = comandoSegunSistema(raiz);
        if (comando == null) {
            throw new IllegalStateException(
                    "No sé cómo abrir la terminal en este sistema. Ejecuta en " + raiz + ":\n"
                            + SCRIPT_DOCKER);
        }

        try {
            lanzarDesacoplado(comando);
        } catch (IOException e) {
            throw new IOException(
                    "No se pudo abrir la terminal. Ejecuta en " + raiz + ":\n" + SCRIPT_DOCKER, e);
        }
        return "Nueva ventana de " + etiquetaBoton().toLowerCase(Locale.ROOT)
                + " en " + raiz + " con los comandos de práctica Docker.";
    }

    /**
     * Arranca el proceso en segundo plano.
     * En Windows no se redirigen flujos (redirectInput(DISCARD) provoca
     * «Redirect invalid for reading: WRITE»); cmd /start ya abre ventana aparte.
     */
    private void lanzarDesacoplado(List<String> comando) throws IOException {
        if (esWindows()) {
            new ProcessBuilder(comando).start();
            return;
        }
        List<String> efectivo = comando;
        if (ejecutableDisponible("setsid")) {
            efectivo = new java.util.ArrayList<>();
            efectivo.add("setsid");
            efectivo.addAll(comando);
        }
        ProcessBuilder pb = new ProcessBuilder(efectivo);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.start();
    }

    private List<String> comandoSegunSistema(Path raiz) throws IOException {
        if (esWindows()) {
            return comandoWindows(raiz);
        }
        if (esMac()) {
            return comandoMac(raiz);
        }
        if (OS.contains("nux") || OS.contains("nix")) {
            return comandoLinux(raiz);
        }
        return null;
    }

    static final String SCRIPT_BAT = ".forja-terminal-practica.bat";

    private List<String> comandoWindows(Path raiz) throws IOException {
        Path bat = escribirScriptBat(raiz);
        // start "" "ruta.bat" abre ventana CMD nueva; el .bat usa %~dp0 (sin rutas C:\ embebidas)
        return List.of("cmd.exe", "/c", "start", "", bat.toString());
    }

    private Path escribirScriptBat(Path raiz) throws IOException {
        Path bat = raiz.resolve(SCRIPT_BAT);
        Files.writeString(bat, contenidoScriptBat(), StandardCharsets.UTF_8);
        return bat.toAbsolutePath().normalize();
    }

    static String contenidoScriptBat() {
        return """
                @echo off
                chcp 65001 >nul
                cd /d "%~dp0"
                title Forja - entorno practica
                echo Levantando contenedor practica...
                docker compose up -d --build practica
                if errorlevel 1 (
                    echo.
                    echo ERROR al levantar el contenedor.
                    echo Comprueba que Docker Desktop este en marcha.
                    goto fin
                )
                docker exec -it forjaexamenes-practica bash
                :fin
                echo.
                pause
                """;
    }

    private List<String> comandoMac(Path raiz) {
        String ruta = raiz.toString().replace("\\", "\\\\").replace("\"", "\\\"");
        String shell = String.format("cd \\\"%s\\\" && %s", ruta, SCRIPT_DOCKER);
        // make new window: ventana independiente de la que ejecuta la app
        String appleScript = String.format(
                "tell application \"Terminal\"%n"
                        + "    activate%n"
                        + "    do script \"%s\" in (make new window)%n"
                        + "end tell",
                shell);
        return List.of("osascript", "-e", appleScript);
    }

    private List<String> comandoLinux(Path raiz) {
        String bash = scriptBash(raiz);
        // --window / --separate fuerzan ventana nueva, no pestaña en terminal existente
        List<List<String>> candidatos = List.of(
                List.of("gnome-terminal", "--window", "--", "bash", "-lc", bash),
                List.of("konsole", "--separate", "-e", "bash", "-lc", bash),
                List.of("xfce4-terminal", "--window", "-e", "bash", "-lc", bash),
                List.of("xterm", "-e", "bash", "-lc", bash));
        for (List<String> comando : candidatos) {
            if (ejecutableDisponible(comando.get(0))) {
                return comando;
            }
        }
        return null;
    }

    static String scriptBash(Path raiz) {
        String ruta = escaparComillaSimple(raiz.toAbsolutePath().normalize().toString());
        return "cd '" + ruta + "' || exit 1; "
                + "echo 'Levantando contenedor practica...'; "
                + "if ! docker compose up -d --build practica; then "
                + "echo ''; echo 'ERROR al levantar el contenedor.'; "
                + "read -r -p 'Pulsa Enter para cerrar...' _; exit 1; fi; "
                + "docker exec -it forjaexamenes-practica bash || true; "
                + "echo ''; read -r -p 'Pulsa Enter para cerrar...' _";
    }

    private static String escaparComillaSimple(String texto) {
        return texto.replace("'", "'\\''");
    }

    private static boolean ejecutableDisponible(String nombre) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return false;
        }
        for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
            Path candidato = Path.of(dir, nombre);
            if (Files.isExecutable(candidato)) {
                return true;
            }
        }
        return false;
    }

    private boolean esWindows() {
        return OS.contains("win");
    }

    private boolean esMac() {
        return OS.contains("mac") || OS.contains("darwin");
    }
}
