// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.modelo.EntregaAlumno;
import com.luegoestarde.forjaexamenes.modelo.IntentoHistorial;
import com.luegoestarde.forjaexamenes.servicio.ServicioAccesoProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioEntregasAlumno;
import com.luegoestarde.forjaexamenes.servicio.ServicioHistorialIntentos;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import com.luegoestarde.forjaexamenes.servicio.ServicioPanelProfesor;
import com.luegoestarde.forjaexamenes.servicio.ServicioHistorialIntentos.FilaResultado;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profesor/resultados")
public class ControladorProfesorResultados {

    private final ServicioHistorialIntentos servicioHistorial;
    private final ServicioEntregasAlumno servicioEntregas;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioPanelProfesor servicioPanel;

    public ControladorProfesorResultados(
            ServicioHistorialIntentos servicioHistorial,
            ServicioEntregasAlumno servicioEntregas,
            ServicioAccesoProfesor accesoProfesor,
            ServicioMetadatosEjercicio metadatosEjercicio,
            ServicioPanelProfesor servicioPanel) {
        this.servicioHistorial = servicioHistorial;
        this.servicioEntregas = servicioEntregas;
        this.accesoProfesor = accesoProfesor;
        this.metadatosEjercicio = metadatosEjercicio;
        this.servicioPanel = servicioPanel;
    }

    @GetMapping
    public String listar(Model modelo) throws Exception {
        String profesor = metadatosEjercicio.loginActual();
        modelo.addAttribute("tituloPagina", "Resultados de alumnos");
        modelo.addAttribute("filasServidor", servicioHistorial.listarFilas(
                login -> !accesoProfesor.esProfesor(login)));
        modelo.addAttribute("filasImportadas", filasDesdeEntregasImportadas());
        modelo.addAttribute("metricas", servicioPanel.calcular(profesor));
        return "profesor-resultados-lista";
    }

    @GetMapping("/exportar.csv")
    public ResponseEntity<byte[]> exportarServidor() throws Exception {
        byte[] csv = servicioHistorial.exportarCsv(login -> !accesoProfesor.esProfesor(login));
        return respuestaCsv(csv, "resultados-servidor.csv");
    }

    @GetMapping("/importados/exportar.csv")
    public ResponseEntity<byte[]> exportarImportados() throws Exception {
        byte[] csv = servicioHistorial.exportarCsvEntregas(filasDesdeEntregasImportadas());
        return respuestaCsv(csv, "resultados-importados.csv");
    }

    @GetMapping("/{login}/{intentoId}")
    public String detalle(
            @PathVariable String login,
            @PathVariable String intentoId,
            Model modelo) {
        if (accesoProfesor.esProfesor(login)) {
            return "redirect:/profesor/resultados";
        }
        var detalle = servicioHistorial.buscarIntento(login, intentoId);
        if (detalle == null) {
            return "redirect:/profesor/resultados";
        }
        modelo.addAttribute("tituloPagina", "Intento de " + detalle.alumno());
        modelo.addAttribute("detalle", detalle);
        modelo.addAttribute("lenguajeCodigo", lenguajeResaltado(detalle.intento().getModulo()));
        return "profesor-resultado-detalle";
    }

    private List<FilaResultado> filasDesdeEntregasImportadas() throws Exception {
        String profesor = metadatosEjercicio.loginActual();
        if (profesor == null || profesor.isBlank()) {
            return List.of();
        }
        List<FilaResultado> filas = new ArrayList<>();
        for (var resumen : servicioEntregas.listarImportadas(profesor)) {
            try {
                EntregaAlumno entrega = servicioEntregas.obtenerImportada(profesor, resumen.id());
                String alumno = entrega.getNombreEtiqueta();
                String login = entrega.getAlumno().getLogin();
                for (IntentoHistorial i : entrega.getHistorialIntentos()) {
                    filas.add(new FilaResultado(
                            alumno,
                            login,
                            i.getFecha(),
                            i.getModulo(),
                            i.getTitulo(),
                            i.getNota(),
                            i.isAprobado(),
                            i.getTiempoSegundos(),
                            i.getId(),
                            i.getEjercicioId()));
                }
            } catch (Exception ignored) {
                // omitir entregas corruptas
            }
        }
        return filas;
    }

    private static String lenguajeResaltado(String modulo) {
        if (modulo == null || modulo.isBlank()) {
            return "plaintext";
        }
        String mod = modulo.strip().toLowerCase();
        if (mod.startsWith("bd") || mod.contains("sql")) {
            return "sql";
        }
        if (mod.equals("poo") || mod.contains("java")) {
            return "java";
        }
        if (mod.equals("docker") || mod.equals("git") || mod.equals("redes") || mod.equals("sistemas")) {
            return "bash";
        }
        return "plaintext";
    }

    private static ResponseEntity<byte[]> respuestaCsv(byte[] csv, String nombre) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }
}
