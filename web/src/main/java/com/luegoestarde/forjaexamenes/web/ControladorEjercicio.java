package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import com.luegoestarde.forjaexamenes.servicio.AlmacenSesionesEjercicios;
import com.luegoestarde.forjaexamenes.servicio.ServicioEvaluador;
import com.luegoestarde.forjaexamenes.servicio.ServicioGenerador;
import com.luegoestarde.forjaexamenes.servicio.ServicioPdf;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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

    private static final List<String> MODULOS_SORPRESA = List.of(
            "redes", "sistemas", "bd", "docker", "git",
            "poo", "bd_sql", "bd_modelo", "bd_transacciones", "bd_jdbc"
    );

    private final ServicioGenerador servicioGenerador;
    private final ServicioEvaluador servicioEvaluador;
    private final ServicioPdf servicioPdf;
    private final AlmacenSesionesEjercicios almacenSesiones;
    private final ObjectMapper mapeadorJson;
    private final Random aleatorio = new Random();

    public ControladorEjercicio(ServicioGenerador servicioGenerador,
                              ServicioEvaluador servicioEvaluador,
                              ServicioPdf servicioPdf,
                              AlmacenSesionesEjercicios almacenSesiones) {
        this.servicioGenerador = servicioGenerador;
        this.servicioEvaluador = servicioEvaluador;
        this.servicioPdf = servicioPdf;
        this.almacenSesiones = almacenSesiones;
        this.mapeadorJson = new ObjectMapper();
        this.mapeadorJson.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) String modulo,
                        @RequestParam(required = false) Boolean sorpresa,
                        @RequestParam(required = false) Integer nivel,
                        Model modelo) throws Exception {
        int nivelEfectivo = nivel != null ? nivel : 2;
        Escenario escenario = crearEscenario(modulo, sorpresa, nivelEfectivo);
        almacenSesiones.guardarEscenario(escenario);
        modelo.addAttribute("escenario", escenario);
        return "ejercicio";
    }

    @GetMapping("/fragment/nuevo")
    public String fragmentoNuevo(@RequestParam(required = false) String modulo,
                                 @RequestParam(required = false) Boolean sorpresa,
                                 @RequestParam(required = false) Integer nivel,
                                 Model modelo) throws Exception {
        Escenario escenario = crearEscenario(modulo, sorpresa, nivel);
        almacenSesiones.guardarEscenario(escenario);
        modelo.addAttribute("escenario", escenario);
        return "fragments/ejercicio-contenido :: contenido";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable String id, Model modelo) {
        Escenario escenario = obtenerEscenario(id);
        modelo.addAttribute("escenario", escenario);
        almacenSesiones.obtenerResultado(id).ifPresent(r -> modelo.addAttribute("resultado", r));
        return "ejercicio";
    }

    @PostMapping("/{id}/evaluar")
    public String evaluar(@PathVariable String id,
                          @RequestParam String respuesta,
                          @RequestParam(required = false) Long tiempoSegundos,
                          RedirectAttributes atributosRedireccion) throws Exception {
        ResultadoEvaluacion resultado = evaluarRespuesta(id, respuesta);
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
                              Model modelo) throws Exception {
        ResultadoEvaluacion resultado = evaluarRespuesta(id, respuesta);
        Escenario escenario = obtenerEscenario(id);
        modelo.addAttribute("escenario", escenario);
        modelo.addAttribute("resultado", resultado);
        if (tiempoSegundos != null) {
            modelo.addAttribute("tiempoSegundos", tiempoSegundos);
        }
        return "fragments/resultado-contenido :: contenido";
    }

    @GetMapping("/{id}/resultado")
    public String resultado(@PathVariable String id,
                            @RequestParam(required = false) Long tiempoSegundos,
                            Model modelo) {
        Escenario escenario = obtenerEscenario(id);
        ResultadoEvaluacion resultado = almacenSesiones.obtenerResultado(id)
                .orElseThrow(() -> new IllegalArgumentException("Sin corrección para: " + id));
        modelo.addAttribute("escenario", escenario);
        modelo.addAttribute("resultado", resultado);
        if (tiempoSegundos != null) {
            modelo.addAttribute("tiempoSegundos", tiempoSegundos);
        }
        return "resultado";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> descargarPdf(@PathVariable String id) throws Exception {
        Escenario escenario = obtenerEscenario(id);
        ResultadoEvaluacion resultado = almacenSesiones.obtenerResultado(id).orElse(null);
        Path rutaPdf = servicioPdf.generarPdf(escenario, resultado);
        Resource recurso = new FileSystemResource(rutaPdf);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + rutaPdf.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(rutaPdf))
                .body(recurso);
    }

    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<byte[]> exportarJson(@PathVariable String id) throws Exception {
        Escenario escenario = obtenerEscenario(id);
        byte[] bytes = mapeadorJson.writerWithDefaultPrettyPrinter().writeValueAsBytes(escenario);
        String nombre = "ejercicio-" + escenario.getId() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    @GetMapping("/api/modulos")
    @ResponseBody
    public List<String> listarModulos() {
        return MODULOS_SORPRESA;
    }

    private Escenario crearEscenario(String modulo, Boolean sorpresa, Integer nivel) throws Exception {
        int nivelEfectivo = nivel != null ? nivel : 2;
        Optional<String> moduloOpt;
        if (Boolean.TRUE.equals(sorpresa) || modulo == null || modulo.isBlank()) {
            if (Boolean.TRUE.equals(sorpresa)) {
                String elegido = MODULOS_SORPRESA.get(aleatorio.nextInt(MODULOS_SORPRESA.size()));
                moduloOpt = Optional.of(elegido);
            } else {
                moduloOpt = Optional.empty();
            }
        } else {
            moduloOpt = Optional.of(modulo);
        }
        return servicioGenerador.generar(moduloOpt, nivelEfectivo);
    }

    private ResultadoEvaluacion evaluarRespuesta(String id, String respuesta) throws Exception {
        Escenario escenario = obtenerEscenario(id);
        ResultadoEvaluacion resultado = servicioEvaluador.evaluar(escenario, respuesta);
        almacenSesiones.guardarResultado(id, resultado);
        return resultado;
    }

    private Escenario obtenerEscenario(String id) {
        return almacenSesiones.obtenerEscenario(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado: " + id));
    }
}
