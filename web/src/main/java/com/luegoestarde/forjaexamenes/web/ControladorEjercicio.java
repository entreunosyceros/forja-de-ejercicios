// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import com.luegoestarde.forjaexamenes.servicio.AlmacenSesionesEjercicios;
import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoPortable;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoPortable.ResultadoGuardadoBanco;
import com.luegoestarde.forjaexamenes.servicio.ServicioDificultadAdaptativa.AjusteDificultad;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import com.luegoestarde.forjaexamenes.servicio.ServicioPdf;
import com.luegoestarde.forjaexamenes.servicio.ServicioSesionEjercicio;
import com.luegoestarde.forjaexamenes.util.MapeadorJson;
import com.luegoestarde.forjaexamenes.util.NombresBanco;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ejercicio")
public class ControladorEjercicio {

    private final ServicioSesionEjercicio sesionEjercicio;
    private final ServicioPdf servicioPdf;
    private final AlmacenSesionesEjercicios almacenSesiones;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioBancoPortable servicioBancoPortable;
    private final ObjectMapper mapeadorJson;

    public ControladorEjercicio(
            ServicioSesionEjercicio sesionEjercicio,
            ServicioPdf servicioPdf,
            AlmacenSesionesEjercicios almacenSesiones,
            ServicioMetadatosEjercicio metadatosEjercicio,
            ServicioAccesoProfesor accesoProfesor,
            ServicioBancoPortable servicioBancoPortable) {
        this.sesionEjercicio = sesionEjercicio;
        this.servicioPdf = servicioPdf;
        this.almacenSesiones = almacenSesiones;
        this.metadatosEjercicio = metadatosEjercicio;
        this.accesoProfesor = accesoProfesor;
        this.servicioBancoPortable = servicioBancoPortable;
        this.mapeadorJson = MapeadorJson.snakeCase();
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) String modulo,
                        @RequestParam(required = false) Boolean sorpresa,
                        @RequestParam(required = false) Integer nivel,
                        @RequestParam(required = false) String capitulo,
                        @RequestParam(required = false) String seccion,
                        Model modelo) throws Exception {
        sesionEjercicio.prepararNuevoEjercicio(modulo, sorpresa, nivel, capitulo, seccion, modelo);
        return "ejercicio";
    }

    @GetMapping("/fragment/nuevo")
    public String fragmentoNuevo(@RequestParam(required = false) String modulo,
                                 @RequestParam(required = false) Boolean sorpresa,
                                 @RequestParam(required = false) Integer nivel,
                                 @RequestParam(required = false) String capitulo,
                                 @RequestParam(required = false) String seccion,
                                 Model modelo) throws Exception {
        sesionEjercicio.prepararNuevoEjercicio(modulo, sorpresa, nivel, capitulo, seccion, modelo);
        return "fragments/ejercicio-contenido :: contenido";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable String id, Model modelo) throws Exception {
        Escenario escenario = sesionEjercicio.obtenerEscenario(id);
        modelo.addAttribute("escenario", escenario);
        almacenSesiones.obtenerResultado(id).ifPresent(r -> modelo.addAttribute("resultado", r));
        sesionEjercicio.adjuntarEstadoSolucionReferencia(escenario, modelo);
        return "ejercicio";
    }

    @PostMapping("/{id}/evaluar")
    public String evaluar(@PathVariable String id,
                          @RequestParam String respuesta,
                          @RequestParam(required = false) Long tiempoSegundos,
                          RedirectAttributes atributosRedireccion) throws Exception {
        ResultadoEvaluacion resultado = sesionEjercicio.evaluarRespuesta(id, respuesta, tiempoSegundos);
        atributosRedireccion.addFlashAttribute("resultado", resultado);
        if (tiempoSegundos != null) {
            atributosRedireccion.addFlashAttribute("tiempoSegundos", tiempoSegundos);
        }
        return "redirect:/ejercicio/" + id + "/resultado";
    }

    @PostMapping("/{id}/evaluar-ajax")
    public String evaluarAjax(@PathVariable String id,
                              @RequestParam String respuesta,
                              @RequestParam(required = false) Long tiempoSegundos,
                              HttpServletResponse respuestaHttp,
                              Model modelo) throws Exception {
        ResultadoEvaluacion resultado = sesionEjercicio.evaluarRespuesta(id, respuesta, tiempoSegundos);
        Escenario escenario = sesionEjercicio.obtenerEscenario(id);
        modelo.addAttribute("escenario", escenario);
        modelo.addAttribute("resultado", resultado);
        sesionEjercicio.adjuntarEstadoSolucionReferencia(escenario, modelo);
        long tiempoEfectivo = sesionEjercicio.tiempoEfectivo(id, tiempoSegundos);
        if (tiempoSegundos != null) {
            modelo.addAttribute("tiempoSegundos", tiempoSegundos);
        } else if (tiempoEfectivo > 0) {
            modelo.addAttribute("tiempoSegundos", tiempoEfectivo);
        }
        String login = metadatosEjercicio.loginActual();
        Optional<AjusteDificultad> ajuste = sesionEjercicio.evaluarAjusteDificultad();
        sesionEjercicio.adjuntarCabeceraProgresoLocal(
                respuestaHttp, escenario, resultado, id, tiempoEfectivo, ajuste, login);
        return "fragments/resultado-contenido :: contenido";
    }

    @GetMapping("/{id}/resultado")
    public String resultado(@PathVariable String id,
                            @RequestParam(required = false) Long tiempoSegundos,
                            Model modelo) throws Exception {
        Escenario escenario = sesionEjercicio.obtenerEscenario(id);
        ResultadoEvaluacion resultado = almacenSesiones.obtenerResultado(id)
                .orElseThrow(() -> new IllegalArgumentException("Sin corrección para: " + id));
        modelo.addAttribute("escenario", escenario);
        modelo.addAttribute("resultado", resultado);
        sesionEjercicio.adjuntarEstadoSolucionReferencia(escenario, modelo);
        if (tiempoSegundos != null) {
            modelo.addAttribute("tiempoSegundos", tiempoSegundos);
        } else if (resultado.getTiempoSegundos() != null) {
            modelo.addAttribute("tiempoSegundos", resultado.getTiempoSegundos());
        } else {
            almacenSesiones.obtenerTiempoSegundos(id).ifPresent(t -> modelo.addAttribute("tiempoSegundos", t));
        }
        return "resultado";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> descargarPdf(@PathVariable String id) throws Exception {
        Escenario escenario = sesionEjercicio.obtenerEscenario(id);
        ResultadoEvaluacion resultado = almacenSesiones.obtenerResultado(id).orElse(null);
        if (!accesoProfesor.modoProfesorActivo() && resultado == null) {
            return ResponseEntity.status(403).build();
        }
        Long tiempo = almacenSesiones.obtenerTiempoSegundos(id).orElse(null);
        Path rutaPdf = servicioPdf.generarPdf(escenario, resultado, tiempo);
        Resource recurso = new FileSystemResource(rutaPdf);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + rutaPdf.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(rutaPdf))
                .body(recurso);
    }

    @PostMapping("/{id}/solucion-referencia")
    @ResponseBody
    public Map<String, String> actualizarSolucionReferencia(
            @PathVariable String id,
            @RequestParam(required = false) String solucionReferencia) {
        sesionEjercicio.actualizarSolucionReferencia(id, solucionReferencia);
        return Map.of("ok", "true", "mensaje", "Solución de referencia guardada.");
    }

    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<byte[]> exportarJson(@PathVariable String id) throws Exception {
        Escenario escenario = sesionEjercicio.obtenerEscenario(id);
        byte[] bytes = mapeadorJson.writerWithDefaultPrettyPrinter().writeValueAsBytes(escenario);
        String nombre = "ejercicio-" + escenario.getId() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    /** Guarda el ejercicio en banco/aprobados o banco/pendientes (no descarga al navegador). */
    @PostMapping("/{id}/guardar-banco")
    @ResponseBody
    public Map<String, Object> guardarEnBanco(
            @PathVariable String id,
            @RequestParam(required = false) String nombreArchivo) throws Exception {
        if (!accesoProfesor.modoProfesorActivo()) {
            throw new IllegalArgumentException("Solo el profesor puede guardar ejercicios en el banco.");
        }
        Escenario escenario = sesionEjercicio.obtenerEscenario(id);
        ResultadoEvaluacion evalReferencia = sesionEjercicio.evaluarSolucionReferencia(escenario);
        ResultadoGuardadoBanco guardado =
                servicioBancoPortable.guardarEjercicioLocal(escenario, evalReferencia, nombreArchivo);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("ok", true);
        respuesta.put("destino", guardado.destino());
        respuesta.put("id", guardado.id());
        respuesta.put("nombreArchivo", guardado.nombreArchivo());
        respuesta.put("ruta", guardado.rutaRelativa());
        respuesta.put("solucionValidada", guardado.solucionValidada());
        respuesta.put("notaReferencia", guardado.notaReferencia());
        respuesta.put("mensaje", guardado.mensaje());
        return respuesta;
    }

    /** Paquete portable para copiar a otra instalación (carpeta compartida). */
    @GetMapping("/{id}/exportar-banco.json")
    public ResponseEntity<byte[]> exportarParaBanco(
            @PathVariable String id,
            @RequestParam(required = false) String nombreArchivo) throws Exception {
        if (!accesoProfesor.modoProfesorActivo()) {
            return ResponseEntity.status(403).build();
        }
        Escenario escenario = sesionEjercicio.obtenerEscenario(id);
        byte[] bytes = servicioBancoPortable.exportarEjercicio(escenario);
        String slug = NombresBanco.resolverNombreArchivo(
                nombreArchivo, escenario.getTitulo(), escenario.getId());
        String nombre = slug + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }
}
