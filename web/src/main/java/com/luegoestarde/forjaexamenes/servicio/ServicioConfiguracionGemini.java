// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.CargadorEnvFichero;
import com.luegoestarde.forjaexamenes.configuracion.ModeloGemini;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.ConfiguracionGeminiAlmacenada;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ServicioConfiguracionGemini {

    public record EstadoGemini(boolean configurado, String mascara, String modelo) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ServicioCuentasUsuarios cuentasUsuarios;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioConfiguracionGemini(
            PropiedadesForjaExamenes propiedades,
            ServicioCuentasUsuarios cuentasUsuarios) {
        this.propiedades = propiedades;
        this.cuentasUsuarios = cuentasUsuarios;
    }

    public String resolverApiKey() {
        return CargadorEnvFichero.resolverGeminiApiKey(
                propiedades.getGeminiApiKey(),
                raizProyecto(),
                directorioDatos());
    }

    public String resolverModelo() {
        String desdeDatos = cargarDesdeDisco().map(ConfiguracionGeminiAlmacenada::getModel).orElse("");
        if (desdeDatos != null && !desdeDatos.isBlank()) {
            return ModeloGemini.normalizar(desdeDatos);
        }
        String desdeEnv = CargadorEnvFichero.resolverGeminiModelo(
                propiedades.getGeminiModel(), raizProyecto());
        return ModeloGemini.normalizar(desdeEnv);
    }

    public boolean estaConfigurado() {
        return !resolverApiKey().isBlank();
    }

    public EstadoGemini obtenerEstado() {
        String clave = resolverApiKey();
        return new EstadoGemini(
                !clave.isBlank(),
                enmascarar(clave),
                resolverModelo());
    }

    /**
     * Cambia solo el modelo (sin pedir contraseña). Requiere sesión y clave ya configurada.
     */
    public void actualizarSoloModelo(String modeloNuevo) throws IOException {
        if (modeloNuevo == null || modeloNuevo.isBlank()) {
            throw new IllegalArgumentException("Indica el nombre del modelo (p. ej. gemini-2.5-flash).");
        }
        if (!estaConfigurado()) {
            throw new IllegalArgumentException(
                    "Primero guarda la clave API de Gemini (formulario de arriba).");
        }
        ConfiguracionGeminiAlmacenada datos = cargarDesdeDisco()
                .orElseThrow(() -> new IllegalStateException("No se encontró datos/gemini.json."));
        datos.setModel(ModeloGemini.normalizar(modeloNuevo));
        guardarEnDisco(datos);
    }

    public void guardarDesdeInterfaz(
            String login,
            String contrasenaActual,
            String apiKeyNueva,
            String modeloNuevo) throws IOException {
        verificarContrasena(login, contrasenaActual);

        ConfiguracionGeminiAlmacenada datos = cargarDesdeDisco().orElse(new ConfiguracionGeminiAlmacenada());
        if (apiKeyNueva != null && !apiKeyNueva.isBlank()) {
            validarFormatoClave(apiKeyNueva);
            datos.setApiKey(apiKeyNueva.strip());
        } else if (datos.getApiKey() == null || datos.getApiKey().isBlank()) {
            throw new IllegalArgumentException("Indica la clave API de Gemini (empieza por AIza…).");
        }

        if (modeloNuevo != null && !modeloNuevo.isBlank()) {
            datos.setModel(ModeloGemini.normalizar(modeloNuevo));
        } else if (datos.getModel() == null || datos.getModel().isBlank()) {
            datos.setModel(ModeloGemini.POR_DEFECTO);
        }

        datos.setActualizadoPor(login);
        datos.setActualizadoEn(Instant.now().toString());
        guardarEnDisco(datos);
    }

    public void quitarDesdeInterfaz(String login, String contrasenaActual) throws IOException {
        verificarContrasena(login, contrasenaActual);
        Path fichero = ficheroConfiguracion();
        Files.deleteIfExists(fichero);
    }

    private void verificarContrasena(String login, String contrasenaActual) {
        if (contrasenaActual == null || contrasenaActual.isBlank()) {
            throw new IllegalArgumentException("Indica tu contraseña actual para confirmar el cambio.");
        }
        var cuenta = cuentasUsuarios.obtenerCuenta(login)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró tu cuenta."));
        if (!cuentasUsuarios.verificarContrasena(contrasenaActual, cuenta.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "La contraseña no es correcta para el usuario «" + login + "». "
                            + "Usa la contraseña con la que entraste (no la clave API de Gemini). "
                            + "Si no la cambiaste: alumno/practica, demo/demo o profesor/profesor.");
        }
    }

    private void validarFormatoClave(String clave) {
        String limpia = clave.strip();
        if (limpia.length() < 20) {
            throw new IllegalArgumentException("La clave API parece demasiado corta.");
        }
        if (!limpia.startsWith("AIza")) {
            throw new IllegalArgumentException("La clave de Gemini suele empezar por AIza…");
        }
    }

    static String enmascarar(String clave) {
        if (clave == null || clave.length() < 8) {
            return "—";
        }
        return clave.substring(0, 4) + "••••••" + clave.substring(clave.length() - 4);
    }

    private Optional<ConfiguracionGeminiAlmacenada> cargarDesdeDisco() {
        Path fichero = ficheroConfiguracion();
        if (!Files.isRegularFile(fichero)) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapeador.readValue(fichero.toFile(), ConfiguracionGeminiAlmacenada.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void guardarEnDisco(ConfiguracionGeminiAlmacenada datos) throws IOException {
        Path fichero = ficheroConfiguracion();
        Files.createDirectories(fichero.getParent());
        mapeador.writerWithDefaultPrettyPrinter().writeValue(fichero.toFile(), datos);
    }

    private Path ficheroConfiguracion() {
        return directorioDatos().resolve("gemini.json");
    }

    private Path directorioDatos() {
        return Path.of(propiedades.getDirectorioDatos()).toAbsolutePath().normalize();
    }

    private String raizProyecto() {
        return Path.of(propiedades.getRaiz()).toAbsolutePath().normalize().toString();
    }
}
