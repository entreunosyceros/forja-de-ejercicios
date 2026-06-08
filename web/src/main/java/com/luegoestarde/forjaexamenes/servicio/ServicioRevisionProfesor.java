// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.InterpretePython;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class ServicioRevisionProfesor {

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioRevisionProfesor(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public JsonNode construirRevision(String ejercicioId) throws Exception {
        Path script = raiz().resolve("herramientas/revision_profesor.py");
        if (!Files.isRegularFile(script)) {
            throw new IllegalStateException("No se encuentra revision_profesor.py");
        }
        List<String> comando = List.of(
                InterpretePython.resolver(propiedades),
                script.toString(),
                "--id",
                ejercicioId);
        String json = ejecutarScript(comando, script.getParent().getParent());
        JsonNode raiz = mapeador.readTree(json);
        if (raiz.has("error")) {
            throw new IllegalArgumentException(raiz.path("error").asText("Error de revisión"));
        }
        return raiz;
    }

    public byte[] generarPaqueteZip(String ejercicioId) throws Exception {
        Path script = raiz().resolve("herramientas/paquete_entrega.py");
        Path carpeta = raiz().resolve("examenes/paquetes").resolve(ejercicioId);
        if (!Files.isRegularFile(script)) {
            throw new IllegalStateException("No se encuentra paquete_entrega.py");
        }
        List<String> comando = List.of(
                InterpretePython.resolver(propiedades),
                script.toString(),
                ejercicioId,
                "--salida",
                carpeta.toString());
        ejecutarScript(comando, raiz());
        if (!Files.isDirectory(carpeta)) {
            throw new IllegalStateException("No se generó el paquete en " + carpeta);
        }
        return zipCarpeta(carpeta);
    }

    private Path raiz() {
        return Path.of(propiedades.getRaiz()).toAbsolutePath().normalize();
    }

    private String ejecutarScript(List<String> comando, Path directorio) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.directory(directorio.toFile());
        pb.redirectErrorStream(true);
        Process proceso = pb.start();
        StringBuilder salida = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                salida.append(linea).append('\n');
            }
        }
        int codigo = proceso.waitFor();
        if (codigo != 0) {
            throw new IllegalStateException(
                    "Script falló (" + codigo + "): " + salida.toString().strip());
        }
        return salida.toString();
    }

    private byte[] zipCarpeta(Path carpeta) throws Exception {
        List<Path> ficheros = new ArrayList<>();
        try (var stream = Files.walk(carpeta)) {
            stream.filter(Files::isRegularFile).forEach(ficheros::add);
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (Path fichero : ficheros) {
                String entrada = carpeta.relativize(fichero).toString().replace('\\', '/');
                zip.putNextEntry(new ZipEntry(entrada));
                try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(fichero))) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    public static MediaType mediaTypeZip() {
        return MediaType.parseMediaType("application/zip");
    }
}
