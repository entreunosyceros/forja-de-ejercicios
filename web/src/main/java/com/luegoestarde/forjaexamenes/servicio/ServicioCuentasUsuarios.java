package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesLogin;
import com.luegoestarde.forjaexamenes.modelo.RegistroUsuarios;
import com.luegoestarde.forjaexamenes.modelo.UsuarioAlmacenado;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ServicioCuentasUsuarios implements UserDetailsService {

    public record ResultadoActualizacionPerfil(
            boolean usuarioAccesoCambiado,
            String nuevoUsuarioAcceso,
            String mensaje) {}

    private final PropiedadesForjaExamenes propiedades;
    private final PropiedadesLogin propiedadesLogin;
    private final PasswordEncoder codificador;
    private final ObjectMapper mapeador = new ObjectMapper();
    private final Map<String, UsuarioAlmacenado> usuarios = new ConcurrentHashMap<>();

    public ServicioCuentasUsuarios(
            PropiedadesForjaExamenes propiedades,
            PropiedadesLogin propiedadesLogin,
            PasswordEncoder codificador) {
        this.propiedades = propiedades;
        this.propiedadesLogin = propiedadesLogin;
        this.codificador = codificador;
    }

    @PostConstruct
    void inicializar() throws IOException {
        Path fichero = ficheroUsuarios();
        Files.createDirectories(fichero.getParent());
        if (Files.isRegularFile(fichero)) {
            cargarDesdeFichero(fichero);
            anadirUsuariosFaltantesDesdePropiedades();
            guardarEnFichero(fichero);
        } else {
            sembrarDesdePropiedades();
            guardarEnFichero(fichero);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String login = normalizarUsuarioAcceso(username);
        UsuarioAlmacenado cuenta = usuarios.get(login);
        if (cuenta == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + login);
        }
        String rolSpring = rolEfectivo(cuenta, login).equals("profesor") ? "PROFESOR" : "ALUMNO";
        return User.builder()
                .username(login)
                .password(cuenta.getPasswordHash())
                .roles(rolSpring)
                .build();
    }

    public Optional<UsuarioAlmacenado> obtenerCuenta(String usuarioAcceso) {
        return Optional.ofNullable(usuarios.get(usuarioAcceso));
    }

    public boolean verificarContrasena(String contrasenaPlana, String hashAlmacenado) {
        return codificador.matches(contrasenaPlana, hashAlmacenado);
    }

    public String nombreVisible(String usuarioAcceso) {
        UsuarioAlmacenado cuenta = usuarios.get(usuarioAcceso);
        if (cuenta == null) {
            return usuarioAcceso;
        }
        String nombre = cuenta.getNombreVisible();
        if (nombre == null || nombre.isBlank()) {
            return usuarioAcceso;
        }
        return nombre.strip();
    }

    public synchronized ResultadoActualizacionPerfil actualizarPerfil(
            String usuarioAccesoActual,
            String nuevoUsuarioAcceso,
            String nombreVisible,
            String contrasenaActual,
            String contrasenaNueva,
            String contrasenaNuevaRepetida) throws IOException {

        UsuarioAlmacenado cuenta = usuarios.get(usuarioAccesoActual);
        if (cuenta == null) {
            throw new IllegalArgumentException("No se encontró tu cuenta.");
        }
        if (contrasenaActual == null || contrasenaActual.isBlank()) {
            throw new IllegalArgumentException("Indica tu contraseña actual.");
        }
        if (!codificador.matches(contrasenaActual, cuenta.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        String loginNuevo = normalizarUsuarioAcceso(nuevoUsuarioAcceso);
        if (!loginNuevo.matches("[a-z0-9_]{2,32}")) {
            throw new IllegalArgumentException(
                    "Usuario de acceso: 2-32 caracteres, solo minúsculas, números o guion bajo.");
        }
        if (!loginNuevo.equals(usuarioAccesoActual) && usuarios.containsKey(loginNuevo)) {
            throw new IllegalArgumentException("Ese usuario de acceso ya existe.");
        }

        String nombre = nombreVisible == null ? "" : nombreVisible.strip();
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        if (nombre.length() > 80) {
            throw new IllegalArgumentException("El nombre es demasiado largo (máx. 80 caracteres).");
        }

        boolean cambiarContrasena = contrasenaNueva != null && !contrasenaNueva.isBlank();
        if (cambiarContrasena) {
            if (contrasenaNueva.length() < 4) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 4 caracteres.");
            }
            if (!contrasenaNueva.equals(contrasenaNuevaRepetida)) {
                throw new IllegalArgumentException("La nueva contraseña y su repetición no coinciden.");
            }
        }

        UsuarioAlmacenado actualizado = new UsuarioAlmacenado(
                cambiarContrasena ? codificador.encode(contrasenaNueva) : cuenta.getPasswordHash(),
                nombre);

        boolean loginCambiado = !loginNuevo.equals(usuarioAccesoActual);
        if (loginCambiado) {
            usuarios.remove(usuarioAccesoActual);
        }
        usuarios.put(loginNuevo, actualizado);
        guardarEnFichero(ficheroUsuarios());

        String mensaje = cambiarContrasena
                ? "Perfil actualizado. Contraseña cambiada."
                : "Perfil actualizado.";
        if (loginCambiado) {
            mensaje += " Vuelve a entrar con tu nuevo usuario de acceso.";
        }
        return new ResultadoActualizacionPerfil(loginCambiado, loginNuevo, mensaje);
    }

    private void cargarDesdeFichero(Path fichero) throws IOException {
        RegistroUsuarios registro = mapeador.readValue(fichero.toFile(), RegistroUsuarios.class);
        usuarios.clear();
        usuarios.putAll(registro.getUsuarios());
    }

    private void guardarEnFichero(Path fichero) throws IOException {
        RegistroUsuarios registro = new RegistroUsuarios();
        registro.setUsuarios(new LinkedHashMap<>(usuarios));
        mapeador.writerWithDefaultPrettyPrinter().writeValue(fichero.toFile(), registro);
    }

    private void anadirUsuariosFaltantesDesdePropiedades() {
        String definicion = propiedadesLogin.getUsuarios();
        if (definicion == null || definicion.isBlank()) {
            return;
        }
        boolean cambio = false;
        for (String par : definicion.split(",")) {
            String entrada = par.trim();
            if (entrada.isEmpty() || !entrada.contains(":")) {
                continue;
            }
            int separador = entrada.indexOf(':');
            String usuario = normalizarUsuarioAcceso(entrada.substring(0, separador));
            String clave = entrada.substring(separador + 1).trim();
            if (usuario.isEmpty() || clave.isEmpty() || usuarios.containsKey(usuario)) {
                continue;
            }
            String rol = esLoginProfesorConfig(usuario) ? "profesor" : "alumno";
            usuarios.put(
                    usuario,
                    new UsuarioAlmacenado(codificador.encode(clave), capitalizarUsuario(usuario), rol));
            cambio = true;
        }
        if (!cambio) {
            return;
        }
    }

    private void sembrarDesdePropiedades() {
        usuarios.clear();
        String definicion = propiedadesLogin.getUsuarios();
        if (definicion == null || definicion.isBlank()) {
            definicion = "alumno:practica";
        }
        for (String par : definicion.split(",")) {
            String entrada = par.trim();
            if (entrada.isEmpty() || !entrada.contains(":")) {
                continue;
            }
            int separador = entrada.indexOf(':');
            String usuario = normalizarUsuarioAcceso(entrada.substring(0, separador));
            String clave = entrada.substring(separador + 1).trim();
            if (usuario.isEmpty() || clave.isEmpty()) {
                continue;
            }
            String rol = esLoginProfesorConfig(usuario) ? "profesor" : "alumno";
            usuarios.put(
                    usuario,
                    new UsuarioAlmacenado(codificador.encode(clave), capitalizarUsuario(usuario), rol));
        }
    }

    private String rolEfectivo(UsuarioAlmacenado cuenta, String login) {
        if (cuenta.esProfesor() || esLoginProfesorConfig(login)) {
            return "profesor";
        }
        return "alumno";
    }

    private boolean esLoginProfesorConfig(String login) {
        String lista = propiedadesLogin.getProfesores();
        if (lista == null || lista.isBlank() || login == null) {
            return false;
        }
        String norm = login.strip().toLowerCase();
        for (String entrada : lista.split(",")) {
            if (norm.equals(entrada.strip().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private Path ficheroUsuarios() {
        return Path.of(propiedades.getDirectorioDatos())
                .toAbsolutePath()
                .normalize()
                .resolve("usuarios.json");
    }

    private static String normalizarUsuarioAcceso(String usuario) {
        if (usuario == null) {
            return "";
        }
        return usuario.strip().toLowerCase();
    }

    private static String capitalizarUsuario(String usuario) {
        if (usuario == null || usuario.isEmpty()) {
            return usuario;
        }
        return usuario.substring(0, 1).toUpperCase() + usuario.substring(1);
    }
}
