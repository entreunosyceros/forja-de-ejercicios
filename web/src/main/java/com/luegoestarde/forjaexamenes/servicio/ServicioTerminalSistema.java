// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Abre una terminal del sistema (PowerShell, Terminal.app, gnome-terminal…)
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

    /** Etiqueta del botón según el sistema (PowerShell, Terminal…). */
    public String etiquetaBoton() {
        if (esWindows()) {
            return "Abrir PowerShell";
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

    /** Arranca el proceso en segundo plano, sin heredar la consola de la aplicación. */
    private void lanzarDesacoplado(List<String> comando) throws IOException {
        List<String> efectivo = comando;
        if (!esWindows() && ejecutableDisponible("setsid")) {
            efectivo = new java.util.ArrayList<>();
            efectivo.add("setsid");
            efectivo.addAll(comando);
        }
        ProcessBuilder pb = new ProcessBuilder(efectivo);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.redirectInput(ProcessBuilder.Redirect.DISCARD);
        pb.start();
    }

    private List<String> comandoSegunSistema(Path raiz) {
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

    private List<String> comandoWindows(Path raiz) {
        String ruta = raiz.toString().replace("'", "''");
        String script = scriptPowerShell(ruta);
        if (ejecutableDisponible("wt.exe")) {
            // Windows Terminal: pestaña nueva en ventana separada
            return List.of(
                    "wt.exe", "-w", "0", "new-tab", "--title", "Forja practica",
                    "-d", raiz.toString(),
                    "powershell.exe", "-NoExit", "-Command", script);
        }
        // cmd start abre SIEMPRE una consola nueva (no reutiliza la de la app)
        return List.of(
                "cmd.exe", "/c", "start", "\"Forja practica\"",
                "powershell.exe", "-NoExit", "-Command", script);
    }

    static String scriptPowerShell(String rutaLiteral) {
        String psDocker = SCRIPT_DOCKER.replace(
                "&&", "; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }; ");
        return String.format(
                "Set-Location -LiteralPath '%s'; "
                        + "Write-Host 'Levantando contenedor practica...' -ForegroundColor Cyan; "
                        + "%s",
                rutaLiteral, psDocker);
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
        return "cd '" + ruta + "' && echo 'Levantando contenedor practica...' && "
                + SCRIPT_DOCKER + "; exec bash";
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
