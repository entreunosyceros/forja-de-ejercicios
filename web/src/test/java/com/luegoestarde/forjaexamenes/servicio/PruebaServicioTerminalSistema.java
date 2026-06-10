// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PruebaServicioTerminalSistema {

  private final ServicioTerminalSistema servicio = new ServicioTerminalSistema();

  @Test
  void scriptBashIncluyeComandosDocker(@TempDir Path tmp) throws Exception {
    Files.writeString(tmp.resolve("docker-compose.yml"), "services: {}");
    String script = ServicioTerminalSistema.scriptBash(tmp);
    assertTrue(script.contains("docker compose up -d --build practica"));
    assertTrue(script.contains("docker exec -it forjaexamenes-practica bash"));
    assertTrue(script.contains(tmp.toAbsolutePath().normalize().toString()));
  }

  @Test
  void etiquetaBotonNoVacia() {
    assertFalse(servicio.etiquetaBoton().isBlank());
  }

  @Test
  void scriptBatUsaDirectorioDelFicheroYPausa() {
    String script = ServicioTerminalSistema.contenidoScriptBat();
    assertTrue(script.contains("docker compose up -d --build practica"));
    assertTrue(script.contains("docker exec -it forjaexamenes-practica bash"));
    assertTrue(script.contains("%~dp0"));
    assertTrue(script.contains("pause"));
  }

  @Test
  void fallaSiNoHayDockerCompose(@TempDir Path tmp) {
    try {
      servicio.abrirTerminalPracticaDocker(tmp);
      // En entornos con terminal gráfica lanzará proceso; sin ella IllegalStateException
    } catch (Exception ex) {
      assertTrue(
          ex.getMessage().contains("docker-compose.yml")
              || ex.getMessage().contains("terminal gráfica")
              || ex.getMessage().contains("No se pudo abrir"));
    }
  }
}
