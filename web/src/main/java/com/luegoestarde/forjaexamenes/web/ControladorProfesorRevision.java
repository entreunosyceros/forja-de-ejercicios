package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.util.Map;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoEjercicios;
import com.luegoestarde.forjaexamenes.servicio.ServicioRevisionProfesor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profesor/revisar")
public class ControladorProfesorRevision {

    private final ServicioBancoEjercicios servicioBanco;
    private final ServicioRevisionProfesor servicioRevision;
    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ControladorProfesorRevision(
            ServicioBancoEjercicios servicioBanco,
            ServicioRevisionProfesor servicioRevision,
            PropiedadesForjaExamenes propiedades) {
        this.servicioBanco = servicioBanco;
        this.servicioRevision = servicioRevision;
        this.propiedades = propiedades;
    }

    @GetMapping
    public String listar(Model modelo) throws Exception {
        modelo.addAttribute("tituloPagina", "Revisar propuestas");
        modelo.addAttribute("pendientes", servicioBanco.listarPendientes());
        modelo.addAttribute("geminiGuardarPendientes", propiedades.isGeminiGuardarPendientes());
        return "profesor-revisar-lista";
    }

    @GetMapping("/{ejercicioId}")
    public String detalle(@PathVariable("ejercicioId") String ejercicioId, Model modelo) throws Exception {
        JsonNode revisionJson = servicioRevision.construirRevision(ejercicioId);
        Map<String, Object> revision = mapeador.convertValue(
                revisionJson, new TypeReference<Map<String, Object>>() {});
        modelo.addAttribute("tituloPagina", "Revisión " + ejercicioId);
        modelo.addAttribute("revision", revision);
        modelo.addAttribute("ejercicioId", ejercicioId);
        return "profesor-revisar-detalle";
    }

    @GetMapping("/{ejercicioId}/paquete")
    public ResponseEntity<byte[]> descargarPaquete(@PathVariable("ejercicioId") String ejercicioId)
            throws Exception {
        byte[] zip = servicioRevision.generarPaqueteZip(ejercicioId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"paquete-" + ejercicioId + ".zip\"")
                .contentType(ServicioRevisionProfesor.mediaTypeZip())
                .body(zip);
    }

    @PostMapping("/{ejercicioId}/aprobar")
    public String aprobar(
            @PathVariable("ejercicioId") String ejercicioId,
            RedirectAttributes flash) throws Exception {
        try {
            servicioBanco.aprobar(ejercicioId);
            flash.addFlashAttribute("mensajePerfilOk", "Ejercicio " + ejercicioId + " aprobado.");
        } catch (Exception ex) {
            flash.addFlashAttribute("errorPerfil", ex.getMessage());
            return "redirect:/profesor/revisar/" + ejercicioId;
        }
        return "redirect:/profesor/revisar";
    }

    @PostMapping("/{ejercicioId}/rechazar")
    public String rechazar(
            @PathVariable("ejercicioId") String ejercicioId,
            RedirectAttributes flash) throws Exception {
        servicioBanco.rechazar(ejercicioId);
        flash.addFlashAttribute("mensajePerfilOk", "Propuesta " + ejercicioId + " rechazada.");
        return "redirect:/profesor/revisar";
    }
}
