package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioCuentasUsuarios;
import com.luegoestarde.forjaexamenes.servicio.ServicioDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorPerfil {

    private final ServicioCuentasUsuarios cuentasUsuarios;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioDocumentacion servicioDocumentacion;

    public ControladorPerfil(
            ServicioCuentasUsuarios cuentasUsuarios,
            ServicioMetadatosEjercicio metadatosEjercicio,
            ServicioDocumentacion servicioDocumentacion) {
        this.cuentasUsuarios = cuentasUsuarios;
        this.metadatosEjercicio = metadatosEjercicio;
        this.servicioDocumentacion = servicioDocumentacion;
    }

    @GetMapping("/perfil")
    public String ver(Model modelo) {
        String login = metadatosEjercicio.loginActual();
        var cuenta = cuentasUsuarios.obtenerCuenta(login).orElseThrow();
        modelo.addAttribute("tituloPagina", "Mi perfil");
        modelo.addAttribute("usuarioAcceso", login);
        modelo.addAttribute("nombreVisible", cuenta.getNombreVisible());
        modelo.addAttribute("geminiConfigurado", servicioDocumentacion.geminiConfigurado());
        return "perfil";
    }

    @PostMapping("/perfil")
    public String guardar(
            @RequestParam String usuarioAcceso,
            @RequestParam String nombreVisible,
            @RequestParam String contrasenaActual,
            @RequestParam(required = false) String contrasenaNueva,
            @RequestParam(required = false) String contrasenaNuevaRepetida,
            HttpServletRequest peticion,
            HttpServletResponse respuesta,
            RedirectAttributes atributos) throws Exception {
        String login = metadatosEjercicio.loginActual();
        try {
            var resultado = cuentasUsuarios.actualizarPerfil(
                    login,
                    usuarioAcceso,
                    nombreVisible,
                    contrasenaActual,
                    contrasenaNueva,
                    contrasenaNuevaRepetida);

            if (resultado.usuarioAccesoCambiado()) {
                new SecurityContextLogoutHandler().logout(peticion, respuesta,
                        SecurityContextHolder.getContext().getAuthentication());
                atributos.addFlashAttribute("mensajePerfil", resultado.mensaje());
                return "redirect:/login?perfil=1";
            }

            atributos.addFlashAttribute("mensajePerfilOk", resultado.mensaje());
            return "redirect:/perfil";
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("errorPerfil", ex.getMessage());
            atributos.addFlashAttribute("usuarioAcceso", usuarioAcceso);
            atributos.addFlashAttribute("nombreVisible", nombreVisible);
            return "redirect:/perfil";
        }
    }
}
