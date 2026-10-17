package sl.p3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Que caracteres sabe escribir el robot. Usa la distribucion de teclado de Windows del PC. */
class KeyboardDriverTest {

    @BeforeEach
    void soloWindows() {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"), "VkKeyScan solo existe en Windows");
    }

    @Test
    void letrasNumerosEspacioYSignos() {
        for (char c : "AZaz09 -'.!&".toCharArray()) assertTrue(KeyboardDriver.isSupported(c), "'" + c + "'");
    }

    @Test
    void noSeEscribenAcentosNiSignosRaros() {
        for (char c : "áÉ@#\"".toCharArray()) assertFalse(KeyboardDriver.isSupported(c), "'" + c + "'");
    }

    /** Todos los nombres de la base de datos de la aplicacion legada se pueden escribir. */
    @Test
    void todosLosNombresDeDatosDat() throws IOException {
        List<String> noSoportados = new ArrayList<>();
        int n = 0;
        for (String line : Files.readAllLines(Path.of("Database-MSDOS/Database/DATOS.DAT"), StandardCharsets.ISO_8859_1)) {
            if (!line.startsWith("\"")) continue; // ultima linea: fin de fichero (Ctrl+Z)
            String nombre = line.substring(1, line.indexOf('"', 1));
            n++;
            for (char c : nombre.toCharArray()) {
                if (!KeyboardDriver.isSupported(c)) {
                    noSoportados.add(nombre);
                    break;
                }
            }
        }
        assertEquals(763, n);
        assertEquals(List.of(), noSoportados);
    }
}
