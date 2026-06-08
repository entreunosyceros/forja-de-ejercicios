package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/entrega")
public class ControladorEntregaAlumno {

    private final ServicioEntregasAlumno servicioEntregas;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ControladorEntregaAlumno(
            ServicioEntregasAlumno servicioEntregas,
            ServicioMetadatosEjercicio metadatosEjercicio) {
        this.servicioEntregas = servicioEntregas;
        this.metadatosEjercicio = metadatosEjercicio;
    }

    @GetMapping("/exportar.json")
    public ResponseEntity<byte[]> exportar() throws Exception {
        String login = metadatosEjercicio.loginActual();
        if (login == null) {
            return ResponseEntity.status(401).build();
        }
        EntregaAlumno entrega = servicioEntregas.construirExportacion(login, metadatosEjercicio.nombreVisibleActual());
        byte[] json = mapeador.writerWithDefaultPrettyPrinter().writeValueAsBytes(entrega);
        String nombre = "entrega-" + login + "-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @PostMapping(value = "/exportar.json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportarConProgresoLocal(@RequestBody JsonNode cuerpo) throws Exception {
        String login = metadatosEjercicio.loginActual();
        if (login == null) {
            return ResponseEntity.status(401).build();
        }
        EntregaAlumno entrega = servicioEntregas.construirExportacion(login, metadatosEjercicio.nombreVisibleActual());
        if (cuerpo != null && cuerpo.has("progresoLocal") && !cuerpo.get("progresoLocal").isNull()) {
            entrega.setProgresoLocal(cuerpo.get("progresoLocal"));
        } else if (cuerpo != null && !cuerpo.isNull()) {
            entrega.setProgresoLocal(cuerpo);
        }

        byte[] json = mapeador.writerWithDefaultPrettyPrinter().writeValueAsBytes(entrega);
        String nombre = "entrega-" + login + "-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/plantilla.json")
    public ResponseEntity<String> plantillaServidor() throws Exception {
        String login = metadatosEjercicio.loginActual();
        if (login == null) {
            return ResponseEntity.status(401).build();
        }
        EntregaAlumno entrega = servicioEntregas.construirExportacion(login, metadatosEjercicio.nombreVisibleActual());
        ObjectNode nodo = mapeador.valueToTree(entrega);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(nodo.toString());
    }
}
