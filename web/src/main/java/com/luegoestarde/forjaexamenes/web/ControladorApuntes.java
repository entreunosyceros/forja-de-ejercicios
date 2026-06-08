// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioApuntesPdf;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/ejercicio/apuntes")
public class ControladorApuntes {

    private final ServicioApuntesPdf servicioApuntesPdf;

    public ControladorApuntes(ServicioApuntesPdf servicioApuntesPdf) {
        this.servicioApuntesPdf = servicioApuntesPdf;
    }

    /**
     * Sirve el PDF del profesor (inline) para que el alumno lo abra en el navegador.
     * Enlazar con {@code #page=N} en la URL para ir a la página del fragmento.
     */
    /**
     * Visor en página con iframe (mejor soporte de {@code #page=N} que un enlace directo en algunos navegadores).
     */
    @GetMapping("/ver")
    public String verConPagina(
            @RequestParam String fuente,
            @RequestParam(required = false) Integer pagina,
            Model modelo) {
        modelo.addAttribute("fuente", fuente);
        modelo.addAttribute("pagina", pagina);
        modelo.addAttribute("tituloPagina", pagina != null && pagina > 0
                ? "Apuntes — página " + pagina
                : "Apuntes del profesor");
        return "apuntes-ver";
    }

    @GetMapping("/archivo")
    public ResponseEntity<Resource> verArchivo(@RequestParam String fuente) throws Exception {
        Path rutaPdf = servicioApuntesPdf.resolverPdf(fuente);
        Resource recurso = new FileSystemResource(rutaPdf);
        String nombre = rutaPdf.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(rutaPdf))
                .body(recurso);
    }
}
