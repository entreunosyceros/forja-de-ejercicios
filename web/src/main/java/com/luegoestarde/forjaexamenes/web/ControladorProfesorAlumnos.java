// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ConflictoImportacionEntregaException;
import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioCuentasUsuarios;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno.ResultadoImportacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno.TipoImportacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import com.luegoestarde.forjaexamenes.servicio.ServicioPanelProfesor;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
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

    static final String SESSION_RECORDAR_SOBRESCRIBIR = "forja.recordarSobrescribirEntregas";
    static final String SESSION_IMPORTACION_PENDIENTE = "forja.importacionEntregaPendiente";

    private final ServicioEntregasAlumno servicioEntregas;
    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioCuentasUsuarios cuentasUsuarios;
    private final ServicioPanelProfesor servicioPanel;
    private final ObjectMapper mapeadorJson = new ObjectMapper();

    public ControladorProfesorAlumnos(
            ServicioEntregasAlumno servicioEntregas,
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioAccesoProfesor accesoProfesor,
            ServicioMetadatosEjercicio metadatosEjercicio,
            ServicioCuentasUsuarios cuentasUsuarios,
            ServicioPanelProfesor servicioPanel) {
        this.servicioEntregas = servicioEntregas;
        this.servicioEstadisticas = servicioEstadisticas;
        this.accesoProfesor = accesoProfesor;
        this.metadatosEjercicio = metadatosEjercicio;
        this.cuentasUsuarios = cuentasUsuarios;
        this.servicioPanel = servicioPanel;
    }

    @GetMapping
    public String listar(Model modelo) throws Exception {
        String profesor = metadatosEjercicio.loginActual();
        var comparativa = servicioEntregas.construirComparativa(profesor);
        Map<String, Object> comparativaVista = mapeadorJson.convertValue(
                comparativa, new TypeReference<Map<String, Object>>() {});
        modelo.addAttribute("tituloPagina", "Seguimiento de alumnos");
        modelo.addAttribute("entregasImportadas", servicioEntregas.listarImportadas(profesor));
        modelo.addAttribute("comparativa", comparativaVista);
        modelo.addAttribute("comparativaModulos", comparativa.modulos());
        modelo.addAttribute("comparativaAlumnos", comparativa.alumnos());
        modelo.addAttribute("comparativaJson", mapeadorJson.writeValueAsString(comparativaVista));
        modelo.addAttribute("alumnosServidor", servicioEstadisticas.listarResumenesEnServidor(
                login -> !accesoProfesor.esProfesor(login)));
        modelo.addAttribute("metricas", servicioPanel.calcular(profesor));
        return "profesor-alumnos-lista";
    }

    @PostMapping("/importar")
    public String importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("nombreEtiqueta") String nombreEtiqueta,
            HttpSession sesion,
            RedirectAttributes flash) {
        try {
            if (!accesoProfesor.puedeAccederZonaProfesor()) {
                return "redirect:/";
            }
            byte[] contenido = archivo.getBytes();
            String profesor = metadatosEjercicio.loginActual();
            boolean autoSobrescribir = Boolean.TRUE.equals(sesion.getAttribute(SESSION_RECORDAR_SOBRESCRIBIR));
            try {
                ResultadoImportacion resultado = servicioEntregas.importar(
                        profesor, contenido, nombreEtiqueta, autoSobrescribir);
                flash.addFlashAttribute("mensajePerfilOk", formatearResultadoImportacion(resultado));
                return "redirect:/profesor/alumnos";
            } catch (ConflictoImportacionEntregaException conflicto) {
                sesion.setAttribute(SESSION_IMPORTACION_PENDIENTE, new ImportacionEntregaPendiente(
                        contenido,
                        nombreEtiqueta,
                        conflicto.getLoginAlumno(),
                        conflicto.getNombreEtiquetaExistente(),
                        conflicto.getImportadoEn(),
                        conflicto.getIdExistente()));
                return "redirect:/profesor/alumnos/importar/conflicto";
            }
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
            return "redirect:/profesor/alumnos";
        }
    }

    @GetMapping("/importar/conflicto")
    public String mostrarConflicto(HttpSession sesion, Model modelo) {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            return "redirect:/";
        }
        ImportacionEntregaPendiente pendiente =
                (ImportacionEntregaPendiente) sesion.getAttribute(SESSION_IMPORTACION_PENDIENTE);
        if (pendiente == null) {
            return "redirect:/profesor/alumnos";
        }
        modelo.addAttribute("tituloPagina", "Confirmar sobrescritura");
        modelo.addAttribute("conflicto", pendiente);
        return "profesor-alumnos-conflicto-importacion";
    }

    @PostMapping("/importar/confirmar")
    public String confirmarImportacion(
            @RequestParam("sobrescribir") boolean sobrescribir,
            @RequestParam(value = "recordarDecision", required = false) boolean recordarDecision,
            HttpSession sesion,
            RedirectAttributes flash) {
        try {
            if (!accesoProfesor.puedeAccederZonaProfesor()) {
                return "redirect:/";
            }
            ImportacionEntregaPendiente pendiente =
                    (ImportacionEntregaPendiente) sesion.getAttribute(SESSION_IMPORTACION_PENDIENTE);
            sesion.removeAttribute(SESSION_IMPORTACION_PENDIENTE);

            if (pendiente == null) {
                flash.addFlashAttribute("errorPerfil", "La importación pendiente ha caducado. Vuelve a seleccionar el fichero.");
                return "redirect:/profesor/alumnos";
            }

            if (recordarDecision) {
                sesion.setAttribute(SESSION_RECORDAR_SOBRESCRIBIR, true);
            }

            if (!sobrescribir) {
                flash.addFlashAttribute("mensajePerfilOk",
                        "Entregas procesadas: 0 nuevas, 0 actualizadas, 1 ignorada.");
                return "redirect:/profesor/alumnos";
            }

            String profesor = metadatosEjercicio.loginActual();
            ResultadoImportacion resultado = servicioEntregas.importar(
                    profesor,
                    pendiente.getContenido(),
                    pendiente.getNombreEtiqueta(),
                    true);
            flash.addFlashAttribute("mensajePerfilOk", formatearResultadoImportacion(resultado));
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

    @PostMapping("/eliminar-todas")
    public String eliminarTodasImportadas(RedirectAttributes flash) {
        try {
            String profesor = metadatosEjercicio.loginActual();
            int borradas = servicioEntregas.eliminarTodasImportadas(profesor);
            if (borradas > 0) {
                flash.addFlashAttribute("mensajePerfilOk",
                        "Se eliminaron " + borradas + " entrega(s) importada(s). Las tablas comparativas están vacías.");
            } else {
                flash.addFlashAttribute("mensajePerfilOk", "No había entregas importadas que eliminar.");
            }
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
        }
        return "redirect:/profesor/alumnos";
    }

    static String formatearResultadoImportacion(ResultadoImportacion resultado) {
        int nuevas = resultado.tipo() == TipoImportacion.NUEVA ? 1 : 0;
        int actualizadas = resultado.tipo() == TipoImportacion.ACTUALIZADA ? 1 : 0;
        return "Entregas procesadas: " + nuevas + " nueva(s), "
                + actualizadas + " actualizada(s), 0 ignorada(s). «"
                + resultado.entrega().getNombreEtiqueta() + "» ("
                + resultado.entrega().getEstadisticasServidor().getTotalIntentos()
                + " ejercicios).";
    }
}
