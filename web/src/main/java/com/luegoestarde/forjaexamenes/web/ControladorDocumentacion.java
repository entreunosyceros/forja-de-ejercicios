package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioIndexacionDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioIndexacionDocumentacion.ResultadoIndexacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioSubidaDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioSubidaDocumentacion.ResultadoSubida;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/documentacion")
public class ControladorDocumentacion {

    private final ServicioIndexacionDocumentacion servicioIndexacion;
    private final ServicioSubidaDocumentacion servicioSubida;

    public ControladorDocumentacion(
            ServicioIndexacionDocumentacion servicioIndexacion,
            ServicioSubidaDocumentacion servicioSubida) {
        this.servicioIndexacion = servicioIndexacion;
        this.servicioSubida = servicioSubida;
    }

    @PostMapping("/subir")
    public String subir(
            @RequestParam(required = false) String tema,
            @RequestParam(required = false) String temaNuevo,
            @RequestParam(required = false) String capitulo,
            @RequestParam("archivos") MultipartFile[] archivos,
            RedirectAttributes atributos) {
        try {
            ResultadoSubida subida = servicioSubida.guardarPdfs(tema, temaNuevo, capitulo, archivos);

            StringBuilder mensaje = new StringBuilder();
            mensaje.append("Guardados ")
                    .append(subida.guardados())
                    .append(" PDF en documentacion/")
                    .append(subida.carpetaDestino())
                    .append(": ")
                    .append(String.join(", ", subida.nombres()));
            if (!subida.errores().isEmpty()) {
                mensaje.append(" · Avisos: ").append(String.join("; ", subida.errores()));
            }
            if (servicioIndexacion.solicitarReindexacionAsincrona()) {
                mensaje.append(" · Indexación en segundo plano…");
                atributos.addFlashAttribute("indexacionEnCurso", true);
            } else {
                mensaje.append(" · Ya hay una indexación en curso.");
            }

            atributos.addFlashAttribute("indexacionExito", true);
            atributos.addFlashAttribute("indexacionMensaje", mensaje.toString());
        } catch (Exception ex) {
            atributos.addFlashAttribute("indexacionExito", false);
            atributos.addFlashAttribute("indexacionMensaje", ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/reindexar")
    public String reindexar(RedirectAttributes atributos) {
        if (servicioIndexacion.solicitarReindexacionAsincrona()) {
            atributos.addFlashAttribute("indexacionEnCurso", true);
            atributos.addFlashAttribute("indexacionExito", true);
            atributos.addFlashAttribute(
                    "indexacionMensaje",
                    "Indexación en segundo plano. Esta página se actualizará sola al terminar.");
        } else {
            var actual = servicioIndexacion.obtenerUltimoResultado();
            atributos.addFlashAttribute("indexacionExito", actual.exito());
            atributos.addFlashAttribute(
                    "indexacionMensaje",
                    "Ya hay una indexación en curso. " + actual.mensaje());
        }
        return "redirect:/";
    }

    @GetMapping("/estado-indexacion")
    @ResponseBody
    public Map<String, Object> estadoIndexacion() {
        var resultado = servicioIndexacion.obtenerUltimoResultado();
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("enCurso", servicioIndexacion.indexacionEnCurso());
        mapa.put("exito", resultado.exito());
        mapa.put("mensaje", resultado.mensaje());
        mapa.put("colecciones", resultado.colecciones());
        mapa.put("instante", resultado.instante() != null ? resultado.instante().toString() : null);
        return mapa;
    }
}
