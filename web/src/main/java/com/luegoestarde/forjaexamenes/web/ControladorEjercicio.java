// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import com.luegoestarde.forjaexamenes.servicio.AlmacenSesionesEjercicios;
import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioCuentasUsuarios;
import com.luegoestarde.forjaexamenes.servicio.ServicioDificultadAdaptativa;
import com.luegoestarde.forjaexamenes.servicio.ServicioDificultadAdaptativa.AjusteDificultad;
import com.luegoestarde.forjaexamenes.servicio.ServicioEvaluador;
import com.luegoestarde.forjaexamenes.servicio.ServicioDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntornoPractica;
import com.luegoestarde.forjaexamenes.servicio.ServicioGenerador;
import com.luegoestarde.forjaexamenes.servicio.ServicioHistorialIntentos;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import com.luegoestarde.forjaexamenes.servicio.ServicioPdf;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoPortable;
import com.luegoestarde.forjaexamenes.servicio.ServicioPrecargaEjercicios;
import com.luegoestarde.forjaexamenes.util.MapeadorJson;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ServicioDocumentacion servicioDocumentacion;
    private final AlmacenSesionesEjercicios almacenSesiones;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioEntornoPractica servicioEntornoPractica;
    private final ServicioCuentasUsuarios cuentasUsuarios;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioDificultadAdaptativa servicioDificultadAdaptativa;
    private final ServicioPrecargaEjercicios servicioPrecarga;
    private final ServicioBancoPortable servicioBancoPortable;
    private final ServicioHistorialIntentos servicioHistorial;
    private final ObjectMapper mapeadorJson;
    private final Random aleatorio = new Random();

    public ControladorEjercicio(ServicioGenerador servicioGenerador,
                              ServicioEvaluador servicioEvaluador,
                              ServicioPdf servicioPdf,
                              ServicioDocumentacion servicioDocumentacion,
                              AlmacenSesionesEjercicios almacenSesiones,
                              ServicioMetadatosEjercicio metadatosEjercicio,
                              ServicioEstadisticasUsuario servicioEstadisticas,
                              ServicioEntornoPractica servicioEntornoPractica,
                              ServicioCuentasUsuarios cuentasUsuarios,
                              ServicioAccesoProfesor accesoProfesor,
                              ServicioDificultadAdaptativa servicioDificultadAdaptativa,
                              ServicioPrecargaEjercicios servicioPrecarga,
                              ServicioBancoPortable servicioBancoPortable,
                              ServicioHistorialIntentos servicioHistorial) {
        this.servicioGenerador = servicioGenerador;
        this.servicioEvaluador = servicioEvaluador;
        this.servicioPdf = servicioPdf;
        this.servicioDocumentacion = servicioDocumentacion;
        this.almacenSesiones = almacenSesiones;
        this.metadatosEjercicio = metadatosEjercicio;
        this.servicioEstadisticas = servicioEstadisticas;
        this.servicioEntornoPractica = servicioEntornoPractica;
        this.cuentasUsuarios = cuentasUsuarios;
        this.accesoProfesor = accesoProfesor;
        this.servicioDificultadAdaptativa = servicioDificultadAdaptativa;
        this.servicioPrecarga = servicioPrecarga;
        this.servicioBancoPortable = servicioBancoPortable;
        this.servicioHistorial = servicioHistorial;
        this.mapeadorJson = MapeadorJson.snakeCase();
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) String modulo,
                        @RequestParam(required = false) Boolean sorpresa,
                        @RequestParam(required = false) Integer nivel,
                        @RequestParam(required = false) String capitulo,
                        @RequestParam(required = false) String seccion,
                        Model modelo) throws Exception {
        prepararNuevoEjercicio(modulo, sorpresa, nivel, capitulo, seccion, modelo);
        return "ejercicio";
    }

    @GetMapping("/fragment/nuevo")
    public String fragmentoNuevo(@RequestParam(required = false) String modulo,
                                 @RequestParam(required = false) Boolean sorpresa,
                                 @RequestParam(required = false) Integer nivel,
                                 @RequestParam(required = false) String capitulo,
                                 @RequestParam(required = false) String seccion,
                                 Model modelo) throws Exception {
        prepararNuevoEjercicio(modulo, sorpresa, nivel, capitulo, seccion, modelo);
        return "fragments/ejercicio-contenido :: contenido";
    }

    /** Genera y guarda el escenario y lo expone en el modelo (común a página y fragmento). */
    private void prepararNuevoEjercicio(String modulo, Boolean sorpresa, Integer nivel,
                                        String capitulo, String seccion, Model modelo) throws Exception {
        Escenario escenario = crearYGuardarEscenario(modulo, sorpresa, nivel, capitulo, seccion);
        modelo.addAttribute("escenario", escenario);
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
        ResultadoEvaluacion resultado = evaluarRespuesta(id, respuesta, tiempoSegundos);
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
        ResultadoEvaluacion resultado = evaluarRespuesta(id, respuesta, tiempoSegundos);
        Escenario escenario = obtenerEscenario(id);
        modelo.addAttribute("escenario", escenario);
        modelo.addAttribute("resultado", resultado);
        long tiempoEfectivo = tiempoSegundos != null
                ? tiempoSegundos
                : almacenSesiones.obtenerTiempoSegundos(id).orElse(0L);
        if (tiempoSegundos != null) {
            modelo.addAttribute("tiempoSegundos", tiempoSegundos);
        } else if (tiempoEfectivo > 0) {
            modelo.addAttribute("tiempoSegundos", tiempoEfectivo);
        }
        String login = metadatosEjercicio.loginActual();
        Optional<AjusteDificultad> ajuste = servicioDificultadAdaptativa.evaluarTrasIntento(login);
        adjuntarCabeceraProgresoLocal(respuestaHttp, escenario, resultado, id, tiempoEfectivo, ajuste, login);
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
        } else if (resultado.getTiempoSegundos() != null) {
            modelo.addAttribute("tiempoSegundos", resultado.getTiempoSegundos());
        } else {
            almacenSesiones.obtenerTiempoSegundos(id).ifPresent(t -> modelo.addAttribute("tiempoSegundos", t));
        }
        return "resultado";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> descargarPdf(@PathVariable String id) throws Exception {
        Escenario escenario = obtenerEscenario(id);
        ResultadoEvaluacion resultado = almacenSesiones.obtenerResultado(id).orElse(null);
        Long tiempo = almacenSesiones.obtenerTiempoSegundos(id).orElse(null);
        Path rutaPdf = servicioPdf.generarPdf(escenario, resultado, tiempo);
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

    /** Paquete listo para importar en otro equipo (banco/aprobados/). */
    @GetMapping("/{id}/exportar-banco.json")
    public ResponseEntity<byte[]> exportarParaBanco(@PathVariable String id) throws Exception {
        Escenario escenario = obtenerEscenario(id);
        byte[] bytes = servicioBancoPortable.exportarEjercicio(escenario);
        String nombre = "banco-" + escenario.getId() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    @GetMapping("/api/modulos")
    @ResponseBody
    public List<String> listarModulos() {
        return modulosParaSorpresa();
    }

    @GetMapping("/api/modulos-documentacion")
    @ResponseBody
    public List<ServicioDocumentacion.ModuloDocumentacion> listarModulosDocumentacion() {
        return servicioDocumentacion.listarModulosIndexados();
    }

    @GetMapping("/api/modulos-documentacion/{modulo}/secciones")
    @ResponseBody
    public List<ServicioDocumentacion.SeccionDocumentacion> listarSeccionesDocumentacion(
            @PathVariable String modulo) {
        return servicioDocumentacion.listarSecciones(modulo);
    }

    private List<String> modulosParaSorpresa() {
        List<String> lista = new ArrayList<>(MODULOS_SORPRESA);
        if (servicioDocumentacion.geminiConfigurado()) {
            lista.addAll(servicioDocumentacion.listarIdsModulos());
        }
        return lista;
    }

    private Escenario crearYGuardarEscenario(String modulo, Boolean sorpresa, Integer nivel,
                                             String capitulo, String seccion) throws Exception {
        Escenario escenario = crearEscenario(modulo, sorpresa, nivel, capitulo, seccion);
        if (servicioEntornoPractica.debeLimpiarAlNuevoEjercicio(escenario.getModulo())) {
            servicioEntornoPractica.limpiar();
        }
        metadatosEjercicio.marcarEscenario(
                escenario,
                metadatosEjercicio.loginActual(),
                metadatosEjercicio.nombreVisibleActual());
        almacenSesiones.guardarEscenario(escenario);
        return escenario;
    }

    private Escenario crearEscenario(String modulo, Boolean sorpresa, Integer nivel,
                                     String capitulo, String seccion) throws Exception {
        Optional<String> moduloOpt;
        if (Boolean.TRUE.equals(sorpresa) || modulo == null || modulo.isBlank()) {
            if (Boolean.TRUE.equals(sorpresa)) {
                List<String> candidatos = modulosParaSorpresa();
                String elegido = candidatos.get(aleatorio.nextInt(candidatos.size()));
                moduloOpt = Optional.of(elegido);
            } else {
                moduloOpt = Optional.empty();
            }
        } else {
            moduloOpt = Optional.of(modulo);
        }
        int nivelEfectivo = resolverNivelEfectivo(moduloOpt, nivel);

        String mod = moduloOpt.orElse("");
        boolean sinFiltro = (capitulo == null || capitulo.isBlank())
                && (seccion == null || seccion.isBlank());
        if (mod.startsWith("docs_") && sinFiltro) {
            Optional<Escenario> precargado = servicioPrecarga.tomar(mod, nivelEfectivo);
            if (precargado.isPresent()) {
                return precargado.get();
            }
        }

        return servicioGenerador.generar(
                moduloOpt,
                nivelEfectivo,
                Optional.ofNullable(capitulo),
                Optional.ofNullable(seccion));
    }

    private int resolverNivelEfectivo(Optional<String> moduloOpt, Integer nivelParam) {
        String login = metadatosEjercicio.loginActual();
        if (login != null && !login.isBlank() && !accesoProfesor.esProfesor(login)) {
            return cuentasUsuarios.obtenerNivelGemini(login);
        }
        if (nivelParam != null) {
            return Math.max(1, Math.min(3, nivelParam));
        }
        return 2;
    }

    private ResultadoEvaluacion evaluarRespuesta(String id, String respuesta, Long tiempoSegundos) throws Exception {
        Escenario escenario = obtenerEscenario(id);
        String nombreVisible = metadatosEjercicio.nombreVisibleActual();
        ResultadoEvaluacion resultado = servicioEvaluador.evaluar(escenario, respuesta);
        metadatosEjercicio.enriquecerResultado(resultado, escenario, nombreVisible, tiempoSegundos);
        almacenSesiones.guardarResultado(id, resultado, tiempoSegundos);
        String login = metadatosEjercicio.loginActual();
        servicioEstadisticas.registrar(
                login,
                escenario.getModulo(),
                resultado.getNota(),
                resultado.isAprobado(),
                tiempoSegundos,
                escenario.getTitulo(),
                escenario.getId());
        servicioHistorial.registrar(
                login,
                nombreVisible,
                escenario,
                resultado,
                respuesta,
                tiempoSegundos);
        return resultado;
    }

    private void adjuntarCabeceraProgresoLocal(
            HttpServletResponse respuestaHttp,
            Escenario escenario,
            ResultadoEvaluacion resultado,
            String ejercicioId,
            long tiempoSegundos,
            Optional<AjusteDificultad> ajusteDificultad,
            String login) throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("modulo", escenario.getModulo());
        datos.put("titulo", escenario.getTitulo());
        datos.put("enunciado", escenario.getEnunciado());
        datos.put("nota", resultado.getNota());
        datos.put("aprobado", resultado.isAprobado());
        datos.put("tiempoSegundos", tiempoSegundos);
        datos.put("ejercicioId", ejercicioId);
        ajusteDificultad.ifPresent(a -> {
            datos.put("mensajeDificultad", a.mensaje());
            datos.put("nivelDificultad", a.nivelNuevo());
        });
        if (login != null && !login.isBlank()) {
            datos.put("nivelDificultad", cuentasUsuarios.obtenerNivelGemini(login));
        }
        String json = mapeadorJson.writeValueAsString(datos);
        String b64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        respuestaHttp.setHeader("X-Forja-Progreso", b64);
    }

    private Escenario obtenerEscenario(String id) {
        return almacenSesiones.obtenerEscenario(id, metadatosEjercicio.loginActual())
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado: " + id));
    }
}
