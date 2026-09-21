// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import com.luegoestarde.forjaexamenes.servicio.ServicioDificultadAdaptativa.AjusteDificultad;
import com.luegoestarde.forjaexamenes.util.MapeadorJson;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * Fachada de la sesión de ejercicio: generación, evaluación y metadatos de UI.
 * Mantiene {@link com.luegoestarde.forjaexamenes.web.ControladorEjercicio} fino (HTTP).
 */
@Service
public class ServicioSesionEjercicio {

    private static final List<String> MODULOS_SORPRESA = List.of(
            "redes", "sistemas", "bd", "docker", "git",
            "poo", "bd_sql", "bd_modelo", "bd_transacciones", "bd_jdbc"
    );

    private final ServicioGenerador servicioGenerador;
    private final ServicioEvaluador servicioEvaluador;
    private final ServicioDocumentacion servicioDocumentacion;
    private final AlmacenSesionesEjercicios almacenSesiones;
    private final ServicioMetadatosEjercicio metadatosEjercicio;
    private final ServicioEstadisticasUsuario servicioEstadisticas;
    private final ServicioEntornoPractica servicioEntornoPractica;
    private final ServicioCuentasUsuarios cuentasUsuarios;
    private final ServicioAccesoProfesor accesoProfesor;
    private final ServicioDificultadAdaptativa servicioDificultadAdaptativa;
    private final ServicioPrecargaEjercicios servicioPrecarga;
    private final ServicioHistorialIntentos servicioHistorial;
    private final ServicioAnaliticaDiscriminacion servicioAnalitica;
    private final ObjectMapper mapeadorJson;
    private final Random aleatorio = new Random();

    public ServicioSesionEjercicio(
            ServicioGenerador servicioGenerador,
            ServicioEvaluador servicioEvaluador,
            ServicioDocumentacion servicioDocumentacion,
            AlmacenSesionesEjercicios almacenSesiones,
            ServicioMetadatosEjercicio metadatosEjercicio,
            ServicioEstadisticasUsuario servicioEstadisticas,
            ServicioEntornoPractica servicioEntornoPractica,
            ServicioCuentasUsuarios cuentasUsuarios,
            ServicioAccesoProfesor accesoProfesor,
            ServicioDificultadAdaptativa servicioDificultadAdaptativa,
            ServicioPrecargaEjercicios servicioPrecarga,
            ServicioHistorialIntentos servicioHistorial,
            ServicioAnaliticaDiscriminacion servicioAnalitica) {
        this.servicioGenerador = servicioGenerador;
        this.servicioEvaluador = servicioEvaluador;
        this.servicioDocumentacion = servicioDocumentacion;
        this.almacenSesiones = almacenSesiones;
        this.metadatosEjercicio = metadatosEjercicio;
        this.servicioEstadisticas = servicioEstadisticas;
        this.servicioEntornoPractica = servicioEntornoPractica;
        this.cuentasUsuarios = cuentasUsuarios;
        this.accesoProfesor = accesoProfesor;
        this.servicioDificultadAdaptativa = servicioDificultadAdaptativa;
        this.servicioPrecarga = servicioPrecarga;
        this.servicioHistorial = servicioHistorial;
        this.servicioAnalitica = servicioAnalitica;
        this.mapeadorJson = MapeadorJson.snakeCase();
    }

    public Escenario prepararNuevoEjercicio(
            String modulo, Boolean sorpresa, Integer nivel,
            String capitulo, String seccion, Model modelo) throws Exception {
        Escenario escenario = crearYGuardarEscenario(modulo, sorpresa, nivel, capitulo, seccion);
        modelo.addAttribute("escenario", escenario);
        adjuntarEstadoSolucionReferencia(escenario, modelo);
        return escenario;
    }

    public Escenario obtenerEscenario(String id) {
        return almacenSesiones.obtenerEscenario(id, metadatosEjercicio.loginActual())
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado: " + id));
    }

    public ResultadoEvaluacion evaluarRespuesta(String id, String respuesta, Long tiempoSegundos)
            throws Exception {
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
        try {
            servicioAnalitica.registrar(escenario, resultado);
        } catch (Exception ignored) {
            // La analítica no debe romper la corrección
        }
        return resultado;
    }

    public Optional<AjusteDificultad> evaluarAjusteDificultad() throws Exception {
        return servicioDificultadAdaptativa.evaluarTrasIntento(metadatosEjercicio.loginActual());
    }

    public long tiempoEfectivo(String id, Long tiempoSegundos) {
        if (tiempoSegundos != null) {
            return tiempoSegundos;
        }
        return almacenSesiones.obtenerTiempoSegundos(id).orElse(0L);
    }

    public void adjuntarCabeceraProgresoLocal(
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

    public void adjuntarEstadoSolucionReferencia(Escenario escenario, Model modelo) {
        if (!accesoProfesor.modoProfesorActivo()) {
            return;
        }
        try {
            String solucion = escenario.getSolucionReferencia() != null
                    ? escenario.getSolucionReferencia() : "";
            ResultadoEvaluacion evaluacion = servicioEvaluador.evaluar(escenario, solucion);
            modelo.addAttribute("solucionReferenciaValida",
                    evaluacion.getPesoObtenido() >= evaluacion.getPesoTotal());
            modelo.addAttribute("notaSolucionReferencia", evaluacion.getNota());
        } catch (Exception ignored) {
            modelo.addAttribute("solucionReferenciaValida", false);
        }
    }

    public void actualizarSolucionReferencia(String id, String solucionReferencia) {
        if (!accesoProfesor.modoProfesorActivo()) {
            throw new IllegalArgumentException("Solo el profesor puede editar la solución de referencia.");
        }
        Escenario escenario = obtenerEscenario(id);
        escenario.setSolucionReferencia(solucionReferencia != null ? solucionReferencia : "");
        almacenSesiones.guardarEscenario(escenario);
    }

    public ResultadoEvaluacion evaluarSolucionReferencia(Escenario escenario) throws Exception {
        String solucion = escenario.getSolucionReferencia() != null
                ? escenario.getSolucionReferencia() : "";
        return servicioEvaluador.evaluar(escenario, solucion);
    }

    private Escenario crearYGuardarEscenario(
            String modulo, Boolean sorpresa, Integer nivel,
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

    private Escenario crearEscenario(
            String modulo, Boolean sorpresa, Integer nivel,
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

    private List<String> modulosParaSorpresa() {
        List<String> lista = new ArrayList<>(MODULOS_SORPRESA);
        if (servicioDocumentacion.geminiConfigurado()) {
            lista.addAll(servicioDocumentacion.listarIdsModulos());
        }
        return lista;
    }

    private int resolverNivelEfectivo(Optional<String> moduloOpt, Integer nivelParam) {
        String login = metadatosEjercicio.loginActual();
        if (login != null && !login.isBlank()) {
            return cuentasUsuarios.obtenerNivelGemini(login);
        }
        if (nivelParam != null) {
            return Math.max(1, Math.min(3, nivelParam));
        }
        return 2;
    }
}
