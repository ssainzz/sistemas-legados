package sl.p3;

import sl.p3.ScreenParser.ProgramRecord;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static sl.p3.ScreenParser.has;

/**
 * Los tres casos de uso sobre la aplicación BASIC. Cada operación toma un cerrojo (solo hay un DOSBox), da el foco a
 * DOSBox, comprueba que está en el menú, teclea esperando cada pantalla y vuelve al menú.
 *
 * Para no alterar los datos: nunca se usan las opciones 1, 2, 3 ni 5, y nunca se envía 'S'.
 */
public final class LegacyService {
    private static final int ROWS_PER_PAGE = 18;
    /** Esperas máximas (ms): respuesta normal y búsqueda por nombre, que recorre los registros. */
    private static final int SHORT_MS = 8000, SEARCH_MS = 30000;
    /** Mínimo de caracteres del nombre: la búsqueda es por subcadena y con menos sale casi todo. */
    public static final int MIN_NOMBRE = 3;
    /** Espera máxima al menú en la primera operación, mientras DOSBox arranca (ms). */
    private static final int STARTUP_MS = 20000;
    /** Si ya se hizo alguna operación (solo la primera espera el arranque). */
    private boolean started;

    private final ReentrantLock lock = new ReentrantLock();
    private final DosboxWindow window;
    private final KeyboardDriver kb;
    private final ScreenReader screen;

    public LegacyService(Config cfg) {
        this.window = new DosboxWindow(cfg.windowTitlePrefix);
        this.kb = new KeyboardDriver(cfg.keyDelayMs);
        this.screen = new ScreenReader(cfg, window);
    }

    public DosboxWindow window() {
        return window;
    }

    public ScreenReader screenReader() {
        return screen;
    }

    // ------------------------------------------------- caso de uso 1: registros

    /** Opción 4: "CONTIENE n ARCHIVOS" y Enter. */
    public int contarRegistros() {
        return locked(() -> {
            begin();
            String t = waitFor(() -> kb.typeChar('4'), SHORT_MS, "CONTIENE");
            expect(t, "CONTIENE");
            int n = ScreenParser.parseCount(t)
                    .orElseThrow(() -> new LegacyException("No se pudo leer el numero de registros:\n" + t));
            waitFor(kb::enter, SHORT_MS, "ACABAR"); // vuelta al menú
            return n;
        });
    }

    // ---------------------------------------- caso de uso 2: programa por nombre

    /**
     * Opción 7, N, nombre y Enter. La aplicación enseña los resultados de uno en uno ("ES ESTE?"): se responde N a
     * cada uno hasta "NO HAY NINGUN PROGRAMA", y después N a "BUSCAR ALGUNO MAS". Distingue mayúsculas y busca por
     * subcadena ("MONTY" también encuentra "AUF WIEDERSEHEN MONTY"), así que el nombre se escribe tal cual.
     */
    public List<ProgramRecord> buscarPorNombre(String nombre) {
        String name = nombre.trim();
        if (name.length() < MIN_NOMBRE)
            throw new IllegalArgumentException("El nombre debe tener al menos " + MIN_NOMBRE + " caracteres");
        for (char c : name.toCharArray()) {
            if (!KeyboardDriver.isSupported(c))
                throw new IllegalArgumentException("Caracter no soportado en el nombre: '" + c + "'");
        }
        return locked(() -> {
            begin();
            expect(waitFor(() -> kb.typeChar('7'), SHORT_MS, "SABES EL NUMERO"), "SABES EL NUMERO");
            expect(waitFor(() -> kb.answer('N'), SHORT_MS, "NOMBRE DEL PROGRAMA"), "NOMBRE DEL PROGRAMA");

            List<ProgramRecord> found = new ArrayList<>();
            Set<Integer> vistos = new HashSet<>();
            // Pantalla siguiente: el aviso final o un "ES ESTE?" con un registro nuevo (tras la N, la anterior
            // sigue visible un momento).
            Predicate<String> siguiente = t -> has(t, "NO HAY NINGUN") || (has(t, "ES ESTE")
                    && ScreenParser.parseSingle(t).map(r -> !vistos.contains(r.numero())).orElse(false));
            // Cada resultado puede tardar varios segundos: la aplicación recorre los registros uno a uno.
            String t = waitUntil(() -> {
                kb.type(name);
                kb.enter();
            }, SEARCH_MS, siguiente);
            while (!has(t, "NO HAY NINGUN")) {
                expect(t, "ES ESTE");
                final String screenText = t;
                ProgramRecord rec = ScreenParser.parseSingle(t)
                        .orElseThrow(() -> new LegacyException("No se pudo leer el programa:\n" + screenText));
                if (!vistos.add(rec.numero()))
                    throw new LegacyException("La busqueda no avanza: el registro " + rec.numero() + " sale dos veces");
                found.add(rec);
                t = waitUntil(() -> kb.answer('N'), SEARCH_MS, siguiente);
            }
            expect(waitFor(kb::enter, SHORT_MS, "BUSCAR ALGUNO MAS"), "BUSCAR ALGUNO MAS");
            waitFor(() -> kb.answer('N'), SHORT_MS, "ACABAR");
            return found;
        });
    }

    // ------------------------------------------------ caso de uso 3: por cinta

    /**
     * Opción 6 y Enter: recorre las páginas del listado (18 filas) con espacio y se queda con las de la cinta.
     * Un programa en "A-45" está en la cinta A y en la 45. El orden debe ser ANTIGÜEDAD.
     */
    public List<ProgramRecord> listarCinta(String cinta) {
        String raw = cinta.trim().toUpperCase();
        // Una cinta es una letra o un número (A-Z y 0-33 en los datos): se rechaza lo demás sin recorrer el listado.
        if (!raw.matches("[A-Z]|[0-9]{1,3}"))
            throw new IllegalArgumentException("Cinta no valida: '" + cinta + "' (una letra A-Z o un numero)");
        String id = raw.matches("[0-9]+") ? String.valueOf(Integer.parseInt(raw)) : raw; // "05" es la cinta 5
        return locked(() -> {
            int total = contarRegistros();
            begin();
            String header = waitFor(() -> kb.typeChar('6'), SHORT_MS, "LISTADO DE DATOS");
            expect(header, "LISTADO DE DATOS");
            if (!has(header, "ANTIGUEDAD"))
                throw new LegacyException("El orden actual no es ANTIGUEDAD:\n" + header);

            List<ProgramRecord> all = new ArrayList<>();
            int expected = 1;
            int maxPages = total / ROWS_PER_PAGE + 3;
            // "ENTER PARA VER DESDE EL PRINCIPIO": la primera página empieza en el registro 1
            BufferedImage img = screen.captureUntil(kb::enter, pageStartsAt(1, total), SHORT_MS);
            for (int page = 0; page < maxPages; page++) {
                final int first = expected;
                String text = screen.ocrBest(img,
                        t -> ScreenParser.checkPage(ScreenParser.parseListingPage(t), first).isEmpty());
                List<ProgramRecord> rows = ScreenParser.parseListingPage(text);
                List<String> problems = ScreenParser.checkPage(rows, first);
                if (!problems.isEmpty())
                    throw new LegacyException(conCausa("Pagina " + (page + 1) + " ilegible: " + problems, text));
                all.addAll(rows);
                expected = rows.get(rows.size() - 1).numero() + 1;
                if (expected > total) {
                    // Última página: cualquier tecla vuelve al menú.
                    waitFor(() -> kb.typeChar('X'), SHORT_MS, "ACABAR");
                    break;
                }
                img = screen.captureUntil(kb::space, pageStartsAt(expected, total), SHORT_MS);
            }
            if (expected <= total)
                throw new LegacyException("Listado incompleto: " + (expected - 1) + " de " + total);

            List<ProgramRecord> result = new ArrayList<>();
            for (ProgramRecord r : all) if (r.estaEnCinta(id)) result.add(r);
            return result;
        });
    }

    // ------------------------------------------------- herramientas de desarrollo

    /** Guarda las pantallas del listado completo en dir (el corpus de pruebas). Solo pulsa 6, Enter y espacio. */
    public int capturarCorpus(java.nio.file.Path dir) {
        return locked(() -> {
            begin();
            try {
                screen.save(screen.capture(), dir.resolve("menu.png"));
                int n = 0;
                BufferedImage prev = screen.captureAfter(() -> kb.typeChar('6'));
                screen.save(prev, dir.resolve("listado-cabecera.png"));
                prev = screen.captureAfter(kb::enter);
                for (int page = 1; page <= 50; page++) {
                    screen.save(prev, dir.resolve(String.format("listado-%02d.png", page)));
                    n = page;
                    BufferedImage next = screen.captureAfter(kb::space);
                    if (ScreenReader.sameImage(prev, next)) break; // la pantalla ya no cambia
                    prev = next;
                }
                return n;
            } catch (java.io.IOException e) {
                throw new LegacyException("No se pudo guardar el corpus", e);
            }
        });
    }

    // -------------------------------------------------------------- internos

    private <T> T locked(Supplier<T> op) {
        lock.lock();
        try {
            return op.get();
        } finally {
            lock.unlock();
        }
    }

    /** Foco en DOSBox y vuelta al menú. En la primera operación antes se espera a que DOSBox termine de arrancar. */
    private void begin() {
        window.focus();
        String t = started
                ? screen.ocr(screen.captureStable())
                : waitUntil(() -> {}, STARTUP_MS, LegacyService::isMenu);
        if (!isMenu(t)) recoverToMenu();
        started = true;
    }

    private static boolean isMenu(String text) {
        return has(text, "BUSCAR DATOS") && has(text, "ACABAR");
    }

    /** Vuelve al menú tras un fallo a medias: responde según la pantalla, N a toda pregunta, Enter si no la conoce. */
    private void recoverToMenu() {
        String t = screen.ocr(screen.captureStable());
        for (int i = 0; i < 10; i++) {
            if (isMenu(t)) return;
            final String screenText = t;
            if (has(t, "OTRA TECLA PARA ACABAR")) {           // listado: cualquier tecla que no sea espacio
                t = after(() -> kb.typeChar('X'));
            } else if (has(t, "ES ESTE") || has(t, "ALTERAR") || has(t, "BUSCAR ALGUNO MAS")
                    || has(t, "SABES EL NUMERO")) {            // preguntas S/N de la búsqueda
                t = after(() -> kb.answer('N'));
            } else if (has(t, "NOMBRE DEL PROGRAMA") && !has(t, "NO HAY NINGUN")) {
                t = after(() -> {                              // pide un nombre: uno que no existe
                    kb.type("ZZZZQ");
                    kb.enter();
                });
            } else {
                t = after(kb::enter);
            }
            if (t.equals(screenText) && !isMenu(t)) t = after(kb::enter);
        }
        if (isMenu(t)) return;
        throw new LegacyException(conCausa("La aplicacion legada no esta en el menu y no se pudo recuperar", t));
    }

    /** Ejecuta la acción y espera a que salga alguna de las frases (o se agote el tiempo). Devuelve el texto. */
    private String waitFor(Runnable action, int timeoutMs, String... phrases) {
        return waitUntil(action, timeoutMs, t -> {
            for (String p : phrases) if (has(t, p)) return true;
            return false;
        });
    }

    /** Como {@link #waitFor}, con cualquier condición sobre el texto. */
    private String waitUntil(Runnable action, int timeoutMs, Predicate<String> done) {
        return screen.ocr(screen.captureUntil(action, done, timeoutMs));
    }

    /**
     * La página del listado que empieza en el registro n ya está entera. Las filas se pintan poco a poco y el aviso
     * "PULSA SPACE" de la página anterior sigue visible, así que hay que contar las filas.
     */
    private static Predicate<String> pageStartsAt(int n, int total) {
        int expectedRows = Math.min(ROWS_PER_PAGE, total - n + 1);
        return t -> {
            List<ProgramRecord> rows = ScreenParser.parseListingPage(t);
            return has(t, "PULSA SPACE") && rows.size() == expectedRows && rows.get(0).numero() == n;
        };
    }

    /** Ejecuta la acción y devuelve el texto de la pantalla resultante. */
    private String after(Runnable action) {
        return screen.ocr(screen.captureAfter(action));
    }

    private void expect(String text, String phrase) {
        if (!has(text, phrase))
            throw new LegacyException(conCausa("Pantalla inesperada (se esperaba '" + phrase + "')", text));
    }

    /** Mensaje de error con el texto de la pantalla y, delante, la causa probable (DOSBox tapado, sin foco...). */
    private String conCausa(String msg, String screenText) {
        String causa = window.problema();
        return (causa == null ? "" : causa + "\n") + msg + ":\n" + screenText;
    }
}
