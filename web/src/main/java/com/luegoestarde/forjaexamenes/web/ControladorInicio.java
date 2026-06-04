package com.luegoestarde.forjaexamenes.web;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.servicio.ServicioBancoEjercicios;
import com.luegoestarde.forjaexamenes.servicio.ServicioDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioEstadisticasUsuario;
import com.luegoestarde.forjaexamenes.servicio.ServicioSubidaDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioIndexacionDocumentacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioMetadatosEjercicio;
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

    public ControladorInicio(ServicioDocumentacion servicioDocumentacion,
                             ServicioIndexacionDocumentacion servicioIndexacion,
                             ServicioEstadisticasUsuario servicioEstadisticas,
                             ServicioMetadatosEjercicio metadatosEjercicio,
                             ServicioBancoEjercicios servicioBanco,
                             ServicioSubidaDocumentacion servicioSubida,
                             PropiedadesForjaExamenes propiedades) {
        this.servicioDocumentacion = servicioDocumentacion;
        this.servicioIndexacion = servicioIndexacion;
        this.servicioEstadisticas = servicioEstadisticas;
        this.metadatosEjercicio = metadatosEjercicio;
        this.servicioBanco = servicioBanco;
        this.servicioSubida = servicioSubida;
        this.propiedades = propiedades;
    }

    @GetMapping("/")
    public String inicio(Model modelo) throws Exception {
        modelo.addAttribute("titulo", "luego es tarde... para estudiar");
        modelo.addAttribute("modulosDocumentacion", servicioDocumentacion.listarModulosIndexados());
        modelo.addAttribute("geminiConfigurado", servicioDocumentacion.geminiConfigurado());
        modelo.addAttribute("autoIndexarActivo", propiedades.isAutoIndexarDocumentacion());
        modelo.addAttribute("ultimaIndexacion", servicioIndexacion.obtenerUltimoResultado());
        modelo.addAttribute("modulosBanco", servicioBanco.listarParaPortada());
        modelo.addAttribute("temasDocumentacion", servicioSubida.listarTemasEnDisco());
        modelo.addAttribute("subidaPdfMaxMb", propiedades.getSubidaPdfMaxMb());
        String login = metadatosEjercicio.loginActual();
        if (login != null) {
            var stats = servicioEstadisticas.obtener(login);
            modelo.addAttribute("estadisticas", stats);
            modelo.addAttribute("tiempoPracticado",
                    ServicioEstadisticasUsuario.formatearTiempo(stats.getTiempoTotalSegundos()));
        }
        return "inicio";
    }
}
