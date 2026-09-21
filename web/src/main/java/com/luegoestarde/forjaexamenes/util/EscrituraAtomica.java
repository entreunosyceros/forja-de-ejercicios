// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Escritura de JSON a disco de forma atómica (fichero temporal + move)
 * para no dejar JSON truncados ante un corte de luz o Ctrl-C.
 */
public final class EscrituraAtomica {

    private EscrituraAtomica() {}

    public static void json(ObjectMapper mapeador, Path destino, Object datos) throws IOException {
        ObjectWriter escritor = mapeador.writerWithDefaultPrettyPrinter();
        json(escritor, destino, datos);
    }

    public static void json(ObjectWriter escritor, Path destino, Object datos) throws IOException {
        Path padre = destino.getParent();
        if (padre != null) {
            Files.createDirectories(padre);
        }
        Path dirTmp = padre != null ? padre : Path.of(".");
        Path tmp = Files.createTempFile(dirTmp, ".tmp-", ".json");
        try {
            escritor.writeValue(tmp.toFile(), datos);
            try {
                Files.move(
                        tmp,
                        destino,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } finally {
            if (tmp != null) {
                Files.deleteIfExists(tmp);
            }
        }
    }
}
