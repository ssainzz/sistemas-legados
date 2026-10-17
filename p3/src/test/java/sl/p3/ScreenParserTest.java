package sl.p3;

import org.junit.jupiter.api.Test;
import sl.p3.ScreenParser.ProgramRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Textos de ejemplo tomados de las pantallas reales de la aplicacion legada. */
class ScreenParserTest {

    @Test
    void cuentaRegistros() {
        String t = "INFORMACION BASE DE DATOS\nCONTIENE  763  ARCHIVOS\n"
                + "LA BASE DE DATOS ESTA ORDENADA SEGUN EL CAMPO 'ANTIGUEDAD'.";
        assertEquals(763, ScreenParser.parseCount(t).orElseThrow());
    }

    @Test
    void programaPorNombre() {
        ProgramRecord r = ScreenParser.parseSingle("   1 - MUGSY   CONVERSACIONAL   CINTA:A\nES ESTE ? (S/N)  ?")
                .orElseThrow();
        assertEquals(1, r.numero());
        assertEquals("MUGSY", r.nombre());
        assertEquals("CONVERSACIONAL", r.tipo());
        assertEquals("A", r.cinta());
    }

    /** Nombres con signos y minusculas tal como salen en "ES ESTE?" (resultados reales de la busqueda). */
    @Test
    void programaPorNombreConSignosYMinusculas() {
        ProgramRecord r = ScreenParser.parseSingle("    87  - KNIGHT'S LORE   VIDEOAVENTURA   CINTA:G-U\nES ESTE ? (S/N)  ?")
                .orElseThrow();
        assertEquals(87, r.numero());
        assertEquals("KNIGHT'S LORE", r.nombre());
        assertEquals(List.of("G", "U"), r.cintas());

        r = ScreenParser.parseSingle("   219  - N.O.M.A.D.   ARCADE   CINTA:P-24").orElseThrow();
        assertEquals("N.O.M.A.D.", r.nombre());
        assertEquals("ARCADE", r.tipo());

        r = ScreenParser.parseSingle("   1  - FRIDAY 13th   ARCADE   CINTA:X").orElseThrow();
        assertEquals("FRIDAY 13th", r.nombre());
    }

    @Test
    void paginaDeListado() {
        String page = """
                N°   NOMBRE                  TIPO            CINTA   REGISTRO
                19   OLIMPICON               ARCADE          B       19
                20   MATCH POINT             S. DEPORTIVO    B-E     20
                21   THE BIRDS AND THE BEES  ARCADE          B       21
                PULSA SPACE PARA CONTINUAR U OTRA TECLA PARA ACABAR
                """;
        List<ProgramRecord> rows = ScreenParser.parseListingPage(page);
        assertEquals(3, rows.size());
        assertEquals("MATCH POINT", rows.get(1).nombre());
        assertEquals("S. DEPORTIVO", rows.get(1).tipo());
        assertEquals("B-E", rows.get(1).cinta());
        assertTrue(ScreenParser.checkPage(rows, 19).isEmpty());
        assertFalse(ScreenParser.checkPage(rows, 18).isEmpty());
    }

    @Test
    void toleraErroresDelOcrEnElTipo() {
        List<ProgramRecord> rows = ScreenParser.parseListingPage("3   HUNCHBACK   ARCADF   A   3");
        assertEquals("HUNCHBACK", rows.get(0).nombre());
        assertEquals("ARCADE", rows.get(0).tipo());
    }

    @Test
    void cintaCompuestaNoCoincidePorSubcadena() {
        ProgramRecord r = new ProgramRecord(1, "X", "ARCADE", "A-45", 1);
        assertTrue(r.estaEnCinta("A"));
        assertTrue(r.estaEnCinta("45"));
        assertFalse(r.estaEnCinta("4"));
    }

    @Test
    void reconocePantallas() {
        assertTrue(ScreenParser.has("M E N U\n7 - BUSCAR DATOS\n8 - ACABAR", "buscar datos"));
        assertTrue(ScreenParser.has("NO HAY NINGUN PROGRAMA CON ESE NOMBRE", "NO HAY NINGUN"));
    }
}
