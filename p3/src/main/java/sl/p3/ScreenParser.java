package sl.p3;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analisis del texto que devuelve el OCR. Sin dependencias de pantalla ni de Robot, para poder
 * probarlo con texto de ejemplo (ver ScreenParserTest).
 *
 * Pantallas conocidas (comprobadas en la aplicacion real):
 *  - Opcion 4:  "CONTIENE 763 ARCHIVOS"
 *  - Opcion 7:  "1 - MUGSY   CONVERSACIONAL   CINTA:A"
 *  - Opcion 6:  filas "Nº NOMBRE TIPO CINTA REGISTRO", 18 por pagina, por ejemplo
 *               "20  MATCH POINT   S. DEPORTIVO   B-E   20"   (cinta B-E = el programa esta en B y en E)
 */
public final class ScreenParser {
    private ScreenParser() {}

    /** Tipos de juego que ofrece la aplicacion (pantalla "Introducir datos"). */
    public static final List<String> TIPOS = List.of(
            "UTILIDAD", "ARCADE", "CONVERSACIONAL", "VIDEOAVENTURA",
            "SIMULADOR", "JUEGO DE MESA", "S. DEPORTIVO", "ESTRATEGIA");

    private static final Pattern CINTA_OK = Pattern.compile("[A-Z0-9]+(?:-[A-Z0-9]+)*");
    private static final Pattern COUNT = Pattern.compile("CONT\\w*\\s+(\\d+)\\s+ARCH\\w*");
    private static final Pattern SINGLE = Pattern.compile("^(\\d{1,3})\\s*-\\s*(.+?)\\s+CINTA\\s*:\\s*(\\S+)\\s*$");
    private static final Pattern ROW = Pattern.compile("^(\\d{1,3})\\s+(.+?)\\s+(\\S+)\\s+(\\d{1,3})$");

    /**
     * Un programa. {@code registro} es la columna REGISTRO del listado (coincide con {@code numero}
     * cuando el orden es por antiguedad); en la busqueda por nombre vale igual que {@code numero}.
     */
    public record ProgramRecord(int numero, String nombre, String tipo, String cinta, int registro) {
        /** "A-45" -> [A, 45]. */
        public List<String> cintas() {
            return Arrays.asList(cinta.split("-"));
        }

        /** Comparacion exacta por cinta (no por subcadena: "4" no esta en "45"). */
        public boolean estaEnCinta(String id) {
            return cintas().contains(id.trim().toUpperCase());
        }
    }

    // ---------------------------------------------------------------- opcion 4

    public static Optional<Integer> parseCount(String ocr) {
        Matcher m = COUNT.matcher(normalize(ocr));
        return m.find() ? Optional.of(Integer.parseInt(m.group(1))) : Optional.empty();
    }

    // ---------------------------------------------------------------- opcion 7

    public static Optional<ProgramRecord> parseSingle(String ocr) {
        for (String line : normalize(ocr).split("\\R")) {
            Matcher m = SINGLE.matcher(line.trim());
            if (!m.matches()) continue;
            String[] nt = splitNameAndType(m.group(2));
            int n = Integer.parseInt(m.group(1));
            return Optional.of(new ProgramRecord(n, nt[0], nt[1], cleanCinta(m.group(3)), n));
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- opcion 6

    /** Filas del listado reconocidas en una pagina. Las lineas que no encajan se ignoran. */
    public static List<ProgramRecord> parseListingPage(String ocr) {
        List<ProgramRecord> rows = new ArrayList<>();
        for (String line : normalize(ocr).split("\\R")) {
            Matcher m = ROW.matcher(line.trim());
            if (!m.matches()) continue;
            String cinta = cleanCinta(m.group(3));
            if (!CINTA_OK.matcher(cinta).matches()) continue;
            String[] nt = splitNameAndType(m.group(2));
            rows.add(new ProgramRecord(Integer.parseInt(m.group(1)), nt[0], nt[1], cinta,
                    Integer.parseInt(m.group(4))));
        }
        return rows;
    }

    /**
     * Comprueba que una pagina es coherente: numeracion consecutiva desde {@code firstExpected},
     * REGISTRO igual a Nº (orden por antiguedad), tipo reconocido. Devuelve la lista de problemas
     * (vacia = pagina valida). Sirve para detectar errores del OCR y reintentar.
     */
    public static List<String> checkPage(List<ProgramRecord> rows, int firstExpected) {
        List<String> problems = new ArrayList<>();
        if (rows.isEmpty()) problems.add("pagina sin filas");
        for (int i = 0; i < rows.size(); i++) {
            ProgramRecord r = rows.get(i);
            if (r.numero() != firstExpected + i)
                problems.add("fila " + i + ": numero " + r.numero() + " (esperado " + (firstExpected + i) + ")");
            if (r.registro() != r.numero())
                problems.add("fila " + i + ": registro " + r.registro() + " != numero " + r.numero());
            if (r.tipo().isEmpty()) problems.add("fila " + i + ": tipo no reconocido");
        }
        return problems;
    }

    // ------------------------------------------------------------- utilidades

    /** true si el texto de pantalla contiene la frase (sin acentos, sin distinguir mayusculas). */
    public static boolean has(String screenText, String phrase) {
        return normalize(screenText).contains(normalize(phrase));
    }

    static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toUpperCase();
    }

    /** Deja solo A-Z, 0-9 y '-'. TODO: corregir confusiones tipicas del OCR (B/8, E/F, O/0) si aparecen. */
    static String cleanCinta(String raw) {
        return raw.toUpperCase().replaceAll("[^A-Z0-9-]", "");
    }

    /** "ARCADE" al final de "HUNCHBACK ARCADE" -> {"HUNCHBACK", "ARCADE"}; tolera 1-2 errores de OCR. */
    static String[] splitNameAndType(String middle) {
        String m = middle.trim().toUpperCase();
        String bestType = null;
        int bestDist = Integer.MAX_VALUE, bestCut = -1;
        for (String t : TIPOS) {
            for (int len = t.length() - 1; len <= t.length() + 1; len++) {
                if (len <= 0 || len >= m.length()) continue;
                int cut = m.length() - len;
                if (m.charAt(cut - 1) != ' ') continue; // el tipo empieza tras un espacio
                int d = levenshtein(m.substring(cut), t);
                if (d < bestDist) {
                    bestDist = d;
                    bestType = t;
                    bestCut = cut;
                }
            }
        }
        if (bestType != null && bestDist <= 2) {
            return new String[]{m.substring(0, bestCut).trim(), bestType};
        }
        return new String[]{m, ""};
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
