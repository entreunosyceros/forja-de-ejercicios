// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoEjercicios;
import com.luegoestarde.forjaexamenes.servicio.ServicioDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioSubidaDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioIndexacionDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
import com.luegoestarde.forjaexamenes.servicio.ServicioTerminalSistema;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorInicio {

    private final ServicioDocumentacion servicioDocumentacion;
    private final ServicioIndexacionDocumentacion servicioIndexacion;
    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioBancoEjercicios servicioBanco;
    private final ServicioSubidaDocumentacion servicioSubida;
    private final PropiedadesForjaExamenes propiedades;
    private final ServicioTerminalSistema servicioTerminal;
    private final ObjectMapper mapeadorJson = new ObjectMapper();

    public ControladorInicio(ServicioDocumentacion servicioDocumentacion,
                             ServicioIndexacionDocumentacion servicioIndexacion,
                             ServicioEstadisticasUsuario servicioEstadisticas,
                             ServicioMetadatosEjercicio metadatosEjercicio,
                             ServicioBancoEjercicios servicioBanco,
                             ServicioSubidaDocumentacion servicioSubida,
                             PropiedadesForjaExamenes propiedades,
                             ServicioTerminalSistema servicioTerminal) {
        this.servicioDocumentacion = servicioDocumentacion;
        this.servicioIndexacion = servicioIndexacion;
        this.servicioEstadisticas = servicioEstadisticas;
        this.metadatosEjercicio = metadatosEjercicio;
        this.servicioBanco = servicioBanco;
        this.servicioSubida = servicioSubida;
        this.propiedades = propiedades;
        this.servicioTerminal = servicioTerminal;
    }

    @GetMapping("/")
    public String inicio(Model modelo) throws Exception {
        modelo.addAttribute("titulo", "luego es tarde... para estudiar");
        modelo.addAttribute("modulosDocumentacion", servicioDocumentacion.listarModulosIndexados());
        modelo.addAttribute("geminiConfigurado", servicioDocumentacion.geminiConfigurado());
        modelo.addAttribute("autoIndexarActivo", propiedades.isAutoIndexarDocumentacion());
        modelo.addAttribute("ultimaIndexacion", servicioIndexacion.obtenerUltimoResultado());
        modelo.addAttribute("indexacionEnCurso", servicioIndexacion.indexacionEnCurso());
        modelo.addAttribute("modulosBanco", servicioBanco.listarParaPortada());
        modelo.addAttribute("temasDocumentacion", servicioSubida.listarTemasEnDisco());
        modelo.addAttribute("subidaPdfMaxMb", propiedades.getSubidaPdfMaxMb());
        modelo.addAttribute("terminalPracticaDisponible", servicioTerminal.disponible());
        modelo.addAttribute("terminalPracticaEtiqueta", servicioTerminal.etiquetaBoton());
        String login = metadatosEjercicio.loginActual();
        if (login != null) {
            var stats = servicioEstadisticas.obtener(login);
            modelo.addAttribute("estadisticas", stats);
            modelo.addAttribute("tiempoPracticado",
                    ServicioEstadisticasUsuario.formatearTiempo(stats.getTiempoTotalSegundos()));
            modelo.addAttribute("historialServidorJson",
                    mapeadorJson.writeValueAsString(stats.getUltimosIntentos()));
        }
        return "inicio";
    }
}
