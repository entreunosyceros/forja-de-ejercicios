// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import com.luegoestarde.forjaexamenes.util.EscrituraAtomica;
import com.luegoestarde.forjaexamenes.util.MapeadorJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Traza de discriminación por ejercicio: si casi todos aprueban, el criterio
 * puede ser un colador (o el ejercicio demasiado fácil).
 */
@Service
public class ServicioAnaliticaDiscriminacion {

    public record FilaAnalitica(
            String clave,
            String titulo,
            String modulo,
            int intentos,
            int aprobados,
            double notaMedia,
            double tasaAprobado,
            String alerta) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = MapeadorJson.snakeCase();

    public ServicioAnaliticaDiscriminacion(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public synchronized void registrar(Escenario escenario, ResultadoEvaluacion resultado) throws IOException {
        if (escenario == null || resultado == null) {
            return;
        }
        String clave = claveEjercicio(escenario);
        if (clave.isBlank()) {
            return;
        }
        Path fichero = ruta();
        ObjectNode raiz = cargar(fichero);
        ObjectNode ejercicios = raiz.withObject("ejercicios");
        ObjectNode fila = ejercicios.has(clave) && ejercicios.get(clave).isObject()
                ? (ObjectNode) ejercicios.get(clave)
                : mapeador.createObjectNode();

        int intentos = fila.path("intentos").asInt(0) + 1;
        int aprobados = fila.path("aprobados").asInt(0) + (resultado.isAprobado() ? 1 : 0);
        double sumaNotas = fila.path("suma_notas").asDouble(0) + resultado.getNota();
        double media = sumaNotas / intentos;
        double tasa = aprobados / (double) intentos;

        fila.put("titulo", escenario.getTitulo() != null ? escenario.getTitulo() : clave);
        fila.put("modulo", escenario.getModulo() != null ? escenario.getModulo() : "");
        fila.put("intentos", intentos);
        fila.put("aprobados", aprobados);
        fila.put("suma_notas", Math.round(sumaNotas * 100.0) / 100.0);
        fila.put("nota_media", Math.round(media * 100.0) / 100.0);
        fila.put("tasa_aprobado", Math.round(tasa * 1000.0) / 1000.0);
        fila.put("alerta", calcularAlerta(intentos, tasa, media));
        ejercicios.set(clave, fila);
        EscrituraAtomica.json(mapeador, fichero, raiz);
    }

    public List<FilaAnalitica> listar(int minimoIntentos) throws IOException {
        ObjectNode raiz = cargar(ruta());
        ObjectNode ejercicios = raiz.withObject("ejercicios");
        List<FilaAnalitica> filas = new ArrayList<>();
        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> it = ejercicios.fields();
        while (it.hasNext()) {
            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> e = it.next();
            var n = e.getValue();
            int intentos = n.path("intentos").asInt(0);
            if (intentos < minimoIntentos) {
                continue;
            }
            filas.add(new FilaAnalitica(
                    e.getKey(),
                    n.path("titulo").asText(""),
                    n.path("modulo").asText(""),
                    intentos,
                    n.path("aprobados").asInt(0),
                    n.path("nota_media").asDouble(0),
                    n.path("tasa_aprobado").asDouble(0),
                    n.path("alerta").asText("")));
        }
        filas.sort(Comparator
                .comparing((FilaAnalitica f) -> !f.alerta().isBlank())
                .reversed()
                .thenComparing(FilaAnalitica::tasaAprobado, Comparator.reverseOrder()));
        return filas;
    }

    private static String calcularAlerta(int intentos, double tasa, double media) {
        if (intentos < 5) {
            return "";
        }
        if (tasa >= 0.95 && media >= 8.5) {
            return "posible_colador";
        }
        if (tasa <= 0.15 && media <= 3.0) {
            return "posible_trampa";
        }
        return "";
    }

    private static String claveEjercicio(Escenario escenario) {
        String id = escenario.getId() != null ? escenario.getId().trim() : "";
        if (!id.isBlank()) {
            // Preferir id de banco estable (banco-…) frente a UUID de sesión corta
            if (id.startsWith("banco") || id.length() > 12) {
                return id;
            }
        }
        String modulo = escenario.getModulo() != null ? escenario.getModulo() : "";
        String titulo = escenario.getTitulo() != null ? escenario.getTitulo() : "";
        String base = (modulo + "::" + titulo).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return base.length() > 120 ? base.substring(0, 120) : base;
    }

    private Path ruta() throws IOException {
        Path dir = Path.of(propiedades.getDirectorioDatos()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir.resolve("analitica_ejercicios.json");
    }

    private ObjectNode cargar(Path fichero) throws IOException {
        if (!Files.isRegularFile(fichero)) {
            ObjectNode vacio = mapeador.createObjectNode();
            vacio.putObject("ejercicios");
            return vacio;
        }
        return (ObjectNode) mapeador.readTree(fichero.toFile());
    }
}
