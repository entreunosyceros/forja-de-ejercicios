package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoPortable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
public class ControladorBanco {

    private final ServicioBancoPortable servicioBancoPortable;
    private final ServicioAccesoProfesor accesoProfesor;

    public ControladorBanco(
            ServicioBancoPortable servicioBancoPortable,
            ServicioAccesoProfesor accesoProfesor) {
        this.servicioBancoPortable = servicioBancoPortable;
        this.accesoProfesor = accesoProfesor;
    }

    @GetMapping("/profesor/banco/exportar.json")
    public ResponseEntity<byte[]> exportarPaqueteProfesor() throws Exception {
        if (!accesoProfesor.puedeAccederZonaProfesor()) {
            return ResponseEntity.status(403).build();
        }
        if (servicioBancoPortable.contarAprobados() == 0) {
            return ResponseEntity.notFound().build();
        }
        byte[] json = servicioBancoPortable.exportarPaqueteCompleto();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"banco-forja.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @PostMapping("/banco/importar")
    public String importar(
            @RequestParam("archivo") MultipartFile archivo,
            RedirectAttributes flash) {
        try {
            var resultado = servicioBancoPortable.importar(archivo.getBytes());
            StringBuilder msg = new StringBuilder();
            msg.append("Banco actualizado: ")
                    .append(resultado.importados())
                    .append(" nuevo(s), ")
                    .append(resultado.actualizados())
                    .append(" actualizado(s)");
            if (resultado.omitidos() > 0) {
                msg.append(", ").append(resultado.omitidos()).append(" omitido(s)");
            }
            msg.append(". Ya aparecen en «Ejercicios del banco».");
            flash.addFlashAttribute("mensajeBanco", msg.toString());
        } catch (Exception ex) {
            flash.addFlashAttribute("errorBanco", ex.getMessage());
        }
        return "redirect:/#banco-ejercicios";
    }
}
