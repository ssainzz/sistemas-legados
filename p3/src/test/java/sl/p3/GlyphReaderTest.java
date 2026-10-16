package sl.p3;

import org.junit.jupiter.api.Test;
import sl.p3.ScreenParser.ProgramRecord;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba el lector por plantillas con pantallas reales de DOSBox guardadas en corpus/. */
class GlyphReaderTest {
    private final GlyphReader reader = new GlyphReader();

    private String read(String file) throws Exception {
        BufferedImage img = ImageIO.read(new File("corpus/" + file));
        return reader.read(img);
    }

    @Test
    void leeElMenuSinErrores() throws Exception {
        String t = read("menu.png");
        assertTrue(t.contains("M E N U"), t);
        assertTrue(t.contains("4 - INFORMACION BASE DE DATOS"), t);
        assertTrue(t.contains("5 - IMPRIMIR DATOS SEGUN ORDEN"), t);
        assertTrue(t.contains("8 - ACABAR"), t);
        assertFalse(t.contains("#"), t);
    }

    @Test
    void leeLaPantallaDeRegistros() throws Exception {
        String t = read("mensajes/info-registros.png");
        assertEquals(763, ScreenParser.parseCount(t).orElseThrow());
    }

    @Test
    void leeLaBusquedaPorNombre() throws Exception {
        ProgramRecord r = ScreenParser.parseSingle(read("mensajes/busca-es-este.png")).orElseThrow();
        assertEquals(643, r.numero());
        assertEquals("FEUD", r.nombre());
        assertEquals("VIDEOAVENTURA", r.tipo());
        assertEquals("21", r.cinta());
    }

    @Test
    void leeLos763RegistrosDelListadoCompleto() throws Exception {
        List<ProgramRecord> all = new ArrayList<>();
        int expected = 1;
        for (int page = 1; page <= 43; page++) {
            String t = read(String.format("listado-%02d.png", page));
            List<ProgramRecord> rows = ScreenParser.parseListingPage(t);
            assertEquals(List.of(), ScreenParser.checkPage(rows, expected), "pagina " + page + "\n" + t);
            assertFalse(t.substring(0, t.lastIndexOf("PULSA")).contains("#"), "caracter desconocido en pagina " + page + "\n" + t);
            all.addAll(rows);
            expected = rows.get(rows.size() - 1).numero() + 1;
        }
        assertEquals(763, all.size());
        assertEquals("MUGSY", all.get(0).nombre());
        assertEquals("BYTE MASTER", all.get(762).nombre());
    }
}
