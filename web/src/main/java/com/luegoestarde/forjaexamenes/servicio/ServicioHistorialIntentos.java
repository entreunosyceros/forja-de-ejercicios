// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.modelo.HistorialUsuario;
import com.luegoestarde.forjaexamenes.modelo.IntentoHistorial;
import com.luegoestarde.forjaexamenes.modelo.ResultadoEvaluacion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class ServicioHistorialIntentos {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_TEXTO = 8000;

    public record FilaResultado(
            String alumno,
            String login,
            String fecha,
            String modulo,
            String titulo,
            double nota,
            boolean aprobado,
            long tiempoSegundos,
            String intentoId,
            String ejercicioId) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioHistorialIntentos(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public synchronized void registrar(
            String login,
            String nombreVisible,
            Escenario escenario,
            ResultadoEvaluacion resultado,
            String respuesta,
            Long tiempoSegundos) throws IOException {
        if (login == null || login.isBlank() || escenario == null || resultado == null) {
            return;
        }

        HistorialUsuario historial = cargar(login);
        if (historial.getNombreVisible() == null || historial.getNombreVisible().isBlank()) {
            historial.setNombreVisible(nombreVisible != null ? nombreVisible : login);
        } else if (nombreVisible != null && !nombreVisible.isBlank()) {
            historial.setNombreVisible(nombreVisible);
        }
        historial.setLogin(login);

        IntentoHistorial intento = new IntentoHistorial();
        intento.setId(UUID.randomUUID().toString().substring(0, 12));
        intento.setFecha(LocalDateTime.now(ZONA).format(FORMATO));
        intento.setModulo(escenario.getModulo() != null ? escenario.getModulo() : "");
        intento.setTitulo(escenario.getTitulo() != null ? escenario.getTitulo() : escenario.getModulo());
        intento.setNota(resultado.getNota());
        intento.setAprobado(resultado.isAprobado());
        intento.setTiempoSegundos(tiempoSegundos != null ? tiempoSegundos : 0L);
        intento.setEjercicioId(escenario.getId() != null ? escenario.getId() : "");
        intento.setEnunciado(truncar(escenario.getEnunciado()));
        intento.setRespuesta(truncar(respuesta));

        List<IntentoHistorial> lista = new ArrayList<>(historial.getIntentos());
        lista.add(0, intento);
        historial.setIntentos(lista);
        guardar(login, historial);
    }

    public HistorialUsuario obtener(String login) {
        return cargar(login);
    }

    public List<IntentoHistorial> listarIntentos(String login) {
        return cargar(login).getIntentos();
    }

    public record IntentoDetalle(String alumno, String login, IntentoHistorial intento) {}

    public IntentoDetalle buscarIntento(String login, String intentoId) {
        HistorialUsuario h = cargar(login);
        String alumno = h.getNombreVisible() != null && !h.getNombreVisible().isBlank()
                ? h.getNombreVisible()
                : login;
        return h.getIntentos().stream()
                .filter(i -> intentoId.equals(i.getId()))
                .findFirst()
                .map(i -> new IntentoDetalle(alumno, login, i))
                .orElse(null);
    }

    public List<FilaResultado> listarFilas(Predicate<String> incluirLogin) throws IOException {
        Path carpeta = carpetaHistorial();
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        List<FilaResultado> filas = new ArrayList<>();
        try (var stream = Files.list(carpeta)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .forEach(fichero -> {
                        try {
                            HistorialUsuario h = mapeador.readValue(fichero.toFile(), HistorialUsuario.class);
                            String login = h.getLogin() != null && !h.getLogin().isBlank()
                                    ? h.getLogin()
                                    : fichero.getFileName().toString().replace(".json", "");
                            if (incluirLogin != null && !incluirLogin.test(login)) {
                                return;
                            }
                            String alumno = h.getNombreVisible() != null && !h.getNombreVisible().isBlank()
                                    ? h.getNombreVisible()
                                    : login;
                            for (IntentoHistorial i : h.getIntentos()) {
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
                        } catch (IOException ignored) {
                            // omitir corruptos
                        }
                    });
        }
        filas.sort(Comparator.comparing(FilaResultado::fecha).reversed());
        return filas;
    }

    public byte[] exportarCsv(Predicate<String> incluirLogin) throws IOException {
        List<FilaResultado> filas = listarFilas(incluirLogin);
        StringBuilder sb = new StringBuilder();
        sb.append("Alumno;Login;Fecha;Modulo;Titulo;Nota;Aprobado;Tiempo\n");
        for (FilaResultado f : filas) {
            sb.append(csv(f.alumno())).append(';')
                    .append(csv(f.login())).append(';')
                    .append(csv(f.fecha())).append(';')
                    .append(csv(f.modulo())).append(';')
                    .append(csv(f.titulo())).append(';')
                    .append(String.format(Locale.ROOT, "%.1f", f.nota())).append(';')
                    .append(f.aprobado() ? "Si" : "No").append(';')
                    .append(csv(ServicioEstadisticasUsuario.formatearTiempo(f.tiempoSegundos())))
                    .append('\n');
        }
        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return out;
    }

    public byte[] exportarCsvEntregas(List<FilaResultado> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Alumno;Login;Fecha;Modulo;Titulo;Nota;Aprobado;Tiempo\n");
        for (FilaResultado f : filas) {
            sb.append(csv(f.alumno())).append(';')
                    .append(csv(f.login())).append(';')
                    .append(csv(f.fecha())).append(';')
                    .append(csv(f.modulo())).append(';')
                    .append(csv(f.titulo())).append(';')
                    .append(String.format(Locale.ROOT, "%.1f", f.nota())).append(';')
                    .append(f.aprobado() ? "Si" : "No").append(';')
                    .append(csv(ServicioEstadisticasUsuario.formatearTiempo(f.tiempoSegundos())))
                    .append('\n');
        }
        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return out;
    }

    private HistorialUsuario cargar(String login) {
        if (login == null || login.isBlank()) {
            return new HistorialUsuario();
        }
        Path fichero = ficheroHistorial(login);
        if (!Files.isRegularFile(fichero)) {
            HistorialUsuario vacio = new HistorialUsuario();
            vacio.setLogin(login);
            return vacio;
        }
        try {
            HistorialUsuario h = mapeador.readValue(fichero.toFile(), HistorialUsuario.class);
            if (h.getLogin() == null || h.getLogin().isBlank()) {
                h.setLogin(login);
            }
            return h;
        } catch (IOException e) {
            HistorialUsuario vacio = new HistorialUsuario();
            vacio.setLogin(login);
            return vacio;
        }
    }

    private void guardar(String login, HistorialUsuario historial) throws IOException {
        Path fichero = ficheroHistorial(login);
        Files.createDirectories(fichero.getParent());
        mapeador.writerWithDefaultPrettyPrinter().writeValue(fichero.toFile(), historial);
    }

    private Path carpetaHistorial() {
        return Path.of(propiedades.getDirectorioDatos())
                .toAbsolutePath()
                .normalize()
                .resolve("historial");
    }

    private Path ficheroHistorial(String login) {
        String seguro = login.replaceAll("[^a-z0-9_\\-]", "");
        return carpetaHistorial().resolve(seguro + ".json");
    }

    private static String truncar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.strip();
        if (limpio.length() <= MAX_TEXTO) {
            return limpio;
        }
        return limpio.substring(0, MAX_TEXTO) + "…";
    }

    private static String csv(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace('\r', ' ').replace('\n', ' ').replace(';', ',').strip();
        if (limpio.contains("\"")) {
            return "\"" + limpio.replace("\"", "\"\"") + "\"";
        }
        return limpio;
    }
}
