// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesLogin;
import com.luegoestarde.forjaexamenes.modelo.UsuarioAlmacenado;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ServicioAccesoProfesor {

    private final PropiedadesForjaExamenes propiedades;
    private final PropiedadesLogin propiedadesLogin;
    private final ServicioCuentasUsuarios cuentasUsuarios;

    public ServicioAccesoProfesor(
            PropiedadesForjaExamenes propiedades,
            PropiedadesLogin propiedadesLogin,
            ServicioCuentasUsuarios cuentasUsuarios) {
        this.propiedades = propiedades;
        this.propiedadesLogin = propiedadesLogin;
        this.cuentasUsuarios = cuentasUsuarios;
    }

    public boolean esProfesor(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        if (loginsProfesorConfig().contains(login.strip().toLowerCase(Locale.ROOT))) {
            return true;
        }
        return cuentasUsuarios.obtenerCuenta(login)
                .map(UsuarioAlmacenado::esProfesor)
                .orElse(false);
    }

    public boolean esProfesorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_PROFESOR".equals(authority.getAuthority())) {
                return true;
            }
        }
        String login = auth.getName();
        return esProfesor(login);
    }

    /** Acceso a rutas /profesor/* y herramientas de revisión. */
    public boolean puedeAccederZonaProfesor() {
        return esProfesorActual();
    }

    /** Solución visible, enlaces de revisión, etc. */
    public boolean modoProfesorActivo() {
        return propiedades.isModoProfesor() || esProfesorActual();
    }

    private Set<String> loginsProfesorConfig() {
        String lista = propiedadesLogin.getProfesores();
        if (lista == null || lista.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(lista.split(","))
                .map(s -> s.strip().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
