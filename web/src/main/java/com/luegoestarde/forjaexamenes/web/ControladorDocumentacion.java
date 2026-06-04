package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioIndexacionDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioIndexacionDocumentacion.ResultadoIndexacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioSubidaDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioSubidaDocumentacion.ResultadoSubida;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            ResultadoIndexacion indexacion = servicioIndexacion.reindexar();

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
            mensaje.append(" · ").append(indexacion.mensaje());

            atributos.addFlashAttribute("indexacionExito", indexacion.exito());
            atributos.addFlashAttribute("indexacionMensaje", mensaje.toString());
            atributos.addFlashAttribute("indexacionColecciones", indexacion.colecciones());
        } catch (Exception ex) {
            atributos.addFlashAttribute("indexacionExito", false);
            atributos.addFlashAttribute("indexacionMensaje", ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/reindexar")
    public String reindexar(RedirectAttributes atributos) {
        ResultadoIndexacion resultado = servicioIndexacion.reindexar();
        atributos.addFlashAttribute("indexacionExito", resultado.exito());
        atributos.addFlashAttribute("indexacionMensaje", resultado.mensaje());
        atributos.addFlashAttribute("indexacionColecciones", resultado.colecciones());
        return "redirect:/";
    }
}
