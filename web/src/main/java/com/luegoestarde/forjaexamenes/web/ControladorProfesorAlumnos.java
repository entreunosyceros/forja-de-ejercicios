package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioCuentasUsuarios;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno;
import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profesor/alumnos")
public class ControladorProfesorAlumnos {

    private final ServicioEntregasAlumno servicioEntregas;
    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioCuentasUsuarios cuentasUsuarios;
    private final ObjectMapper mapeadorJson = new ObjectMapper();

    public ControladorProfesorAlumnos(
            ServicioEntregasAlumno servicioEntregas,
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioAccesoProfesor accesoProfesor,
            ServicioMetadatosEjercicio metadatosEjercicio,
            ServicioCuentasUsuarios cuentasUsuarios) {
        this.servicioEntregas = servicioEntregas;
        this.servicioEstadisticas = servicioEstadisticas;
        this.accesoProfesor = accesoProfesor;
        this.metadatosEjercicio = metadatosEjercicio;
        this.cuentasUsuarios = cuentasUsuarios;
    }

    @GetMapping
    public String listar(Model modelo) throws Exception {
        String profesor = metadatosEjercicio.loginActual();
        var comparativa = servicioEntregas.construirComparativa(profesor);
        modelo.addAttribute("tituloPagina", "Seguimiento de alumnos");
        modelo.addAttribute("entregasImportadas", servicioEntregas.listarImportadas(profesor));
        modelo.addAttribute("comparativa", comparativa);
        modelo.addAttribute("comparativaJson", mapeadorJson.writeValueAsString(comparativa));
        modelo.addAttribute("alumnosServidor", servicioEstadisticas.listarResumenesEnServidor(
                login -> !accesoProfesor.esProfesor(login)));
        return "profesor-alumnos-lista";
    }

    @PostMapping("/importar")
    public String importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("nombreEtiqueta") String nombreEtiqueta,
            RedirectAttributes flash) {
        try {
            if (!accesoProfesor.puedeAccederZonaProfesor()) {
                return "redirect:/";
            }
            String profesor = metadatosEjercicio.loginActual();
            EntregaAlumno entrega = servicioEntregas.importar(
                    profesor, archivo.getBytes(), nombreEtiqueta);
            flash.addFlashAttribute(
                    "mensajePerfilOk",
                    "Entrega importada como «" + entrega.getNombreEtiqueta() + "» ("
                            + entrega.getEstadisticasServidor().getTotalIntentos()
                            + " ejercicios).");
            return "redirect:/profesor/alumnos";
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
            return "redirect:/profesor/alumnos";
        }
    }

    @PostMapping("/importada/{id}/renombrar")
    public String renombrar(
            @PathVariable String id,
            @RequestParam("nombreEtiqueta") String nombreEtiqueta,
            RedirectAttributes flash) {
        try {
            String profesor = metadatosEjercicio.loginActual();
            servicioEntregas.renombrar(profesor, id, nombreEtiqueta);
            flash.addFlashAttribute("mensajePerfilOk", "Nombre actualizado.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
        }
        return "redirect:/profesor/alumnos";
    }

    @GetMapping("/importada/{id}")
    public String verImportada(@PathVariable String id, Model modelo) throws Exception {
        String profesor = metadatosEjercicio.loginActual();
        EntregaAlumno entrega = servicioEntregas.obtenerImportada(profesor, id);
        EstadisticasUsuario stats = entrega.getEstadisticasServidor();
        modelo.addAttribute("tituloPagina", entrega.getNombreEtiqueta());
        modelo.addAttribute("entrega", entrega);
        modelo.addAttribute("estadisticas", stats);
        modelo.addAttribute("tiempoPracticado",
                ServicioEstadisticasUsuario.formatearTiempo(stats.getTiempoTotalSegundos()));
        modelo.addAttribute("progresoLocal", entrega.getProgresoLocal());
        modelo.addAttribute("origen", "importada");
        modelo.addAttribute("entregaId", id);
        return "profesor-alumno-detalle";
    }

    @GetMapping("/servidor/{login}")
    public String verServidor(@PathVariable String login, Model modelo) {
        if (accesoProfesor.esProfesor(login)) {
            return "redirect:/profesor/alumnos";
        }
        EstadisticasUsuario stats = servicioEstadisticas.obtener(login);
        EntregaAlumno entrega = new EntregaAlumno();
        var alumno = new EntregaAlumno.AlumnoInfo();
        alumno.setLogin(login);
        alumno.setNombreVisible(cuentasUsuarios.nombreVisible(login));
        entrega.setAlumno(alumno);
        entrega.setNombreEtiqueta(cuentasUsuarios.nombreVisible(login));
        entrega.setEstadisticasServidor(stats);

        modelo.addAttribute("tituloPagina", "Alumno en este servidor: " + alumno.getNombreVisible());
        modelo.addAttribute("entrega", entrega);
        modelo.addAttribute("estadisticas", stats);
        modelo.addAttribute("tiempoPracticado",
                ServicioEstadisticasUsuario.formatearTiempo(stats.getTiempoTotalSegundos()));
        modelo.addAttribute("progresoLocal", (JsonNode) null);
        modelo.addAttribute("origen", "servidor");
        modelo.addAttribute("entregaId", login);
        return "profesor-alumno-detalle";
    }

    @PostMapping("/importada/{id}/eliminar")
    public String eliminarImportada(@PathVariable String id, RedirectAttributes flash) {
        try {
            String profesor = metadatosEjercicio.loginActual();
            if (servicioEntregas.eliminarImportada(profesor, id)) {
                flash.addFlashAttribute("mensajePerfilOk", "Entrega eliminada del panel.");
            } else {
                flash.addFlashAttribute("errorPerfil", "No se encontró la entrega.");
            }
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
        }
        return "redirect:/profesor/alumnos";
    }
}
