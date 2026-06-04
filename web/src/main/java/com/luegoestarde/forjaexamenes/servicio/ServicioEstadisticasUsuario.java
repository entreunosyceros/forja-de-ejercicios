package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario;
import com.luegoestarde.forjaexamenes.modelo.EstadisticasUsuario.EstadisticasModulo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class ServicioEstadisticasUsuario {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioEstadisticasUsuario(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public EstadisticasUsuario obtener(String login) {
        if (login == null || login.isBlank()) {
            return vacias();
        }
        Path fichero = ficheroEstadisticas(login);
        if (!Files.isRegularFile(fichero)) {
            return vacias();
        }
        try {
            return mapeador.readValue(fichero.toFile(), EstadisticasUsuario.class);
        } catch (IOException e) {
            return vacias();
        }
    }

    public synchronized void registrar(
            String login, String modulo, double nota, boolean aprobado, Long tiempoSegundos)
            throws IOException {
        if (login == null || login.isBlank() || modulo == null || modulo.isBlank()) {
            return;
        }

        EstadisticasUsuario stats = obtener(login);
        stats.setTotalIntentos(stats.getTotalIntentos() + 1);
        if (aprobado) {
            stats.setTotalAprobados(stats.getTotalAprobados() + 1);
            stats.setRachaActual(stats.getRachaActual() + 1);
        } else {
            stats.setTotalSuspensos(stats.getTotalSuspensos() + 1);
            stats.setRachaActual(0);
        }

        double sumaPrev = stats.getNotaMedia() * (stats.getTotalIntentos() - 1);
        stats.setNotaMedia(redondear((sumaPrev + nota) / stats.getTotalIntentos()));
        if (nota > stats.getMejorNota()) {
            stats.setMejorNota(nota);
        }

        if (tiempoSegundos != null && tiempoSegundos > 0) {
            stats.setTiempoTotalSegundos(stats.getTiempoTotalSegundos() + tiempoSegundos);
        }
        stats.setUltimaActividad(LocalDateTime.now(ZONA).format(FORMATO));

        EstadisticasModulo pm = stats.getPorModulo().computeIfAbsent(modulo, m -> new EstadisticasModulo());
        pm.setIntentos(pm.getIntentos() + 1);
        double sumaMod = pm.getNotaMedia() * (pm.getIntentos() - 1);
        pm.setNotaMedia(redondear((sumaMod + nota) / pm.getIntentos()));
        if (nota > pm.getMejorNota()) {
            pm.setMejorNota(nota);
        }
        if (aprobado) {
            pm.setAprobados(pm.getAprobados() + 1);
        }

        guardar(login, stats);
    }

    public static String formatearTiempo(long segundos) {
        if (segundos <= 0) {
            return "0:00";
        }
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long resto = segundos % 60;
        if (horas > 0) {
            return String.format("%d:%02d:%02d", horas, minutos, resto);
        }
        return String.format("%d:%02d", minutos, resto);
    }

    private void guardar(String login, EstadisticasUsuario stats) throws IOException {
        Path fichero = ficheroEstadisticas(login);
        Files.createDirectories(fichero.getParent());
        mapeador.writerWithDefaultPrettyPrinter().writeValue(fichero.toFile(), stats);
    }

    private Path ficheroEstadisticas(String login) {
        String seguro = login.replaceAll("[^a-z0-9_\\-]", "");
        return Path.of(propiedades.getDirectorioDatos())
                .toAbsolutePath()
                .normalize()
                .resolve("estadisticas")
                .resolve(seguro + ".json");
    }

    private static EstadisticasUsuario vacias() {
        return new EstadisticasUsuario();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
