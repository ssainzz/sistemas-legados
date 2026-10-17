package sl.p3;

import sl.p3.ScreenParser.ProgramRecord;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static sl.p3.ScreenParser.has;

/**
 * Los tres casos de uso de la practica sobre la aplicacion BASIC. Cada operacion:
 *   1. toma un lock (solo hay un DOSBox y un teclado: las peticiones web se serializan),
 *   2. da el foco a DOSBox y comprueba que esta en el MENU,
 *   3. envia teclas y lee la pantalla tras cada una (cambio + estabilizacion + OCR),
 *   4. deja la aplicacion otra vez en el menu.
 *
 * Reglas para no alterar los datos de la aplicacion legada:
 *   - Nunca se usan las opciones 1, 2, 3 ni 5 del menu (introducir, grabar, ordenar, imprimir).
 *   - A "QUIERES ALTERAR SUS DATOS?" se responde siempre N. La unica 'S' que se envia es la de "ES ESTE?".
 */
public final class LegacyService {
    private static final int ROWS_PER_PAGE = 18;
    /** Esperas maximas (ms): respuesta normal de la aplicacion y busqueda por nombre (recorre los registros). */
    private static final int SHORT_MS = 8000, SEARCH_MS = 30000;

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

    /** Opcion 4 -> "CONTIENE n ARCHIVOS" -> Enter (vuelve al menu). */
    public int contarRegistros() {
        return locked(() -> {
            begin();
            String t = waitFor(() -> kb.typeChar('4'), SHORT_MS, "CONTIENE");
            expect(t, "CONTIENE");
            int n = ScreenParser.parseCount(t)
                    .orElseThrow(() -> new LegacyException("No se pudo leer el numero de registros:\n" + t));
            waitFor(kb::enter, SHORT_MS, "ACABAR"); // vuelta al menu
            return n;
        });
    }

    // ---------------------------------------- caso de uso 2: programa por nombre

    /**
     * Opcion 7 -> N + Enter -> NOMBRE + Enter -> (ES ESTE?) S + Enter -> (ALTERAR?) N + Enter ->
     * (BUSCAR MAS?) N + Enter -> menu.   Si no existe: aviso + Enter -> (BUSCAR MAS?) N -> menu.
     *
     * Comprobado a mano: la aplicacion distingue mayusculas y busca el texto en cualquier parte del
     * nombre ("MONTY" encuentra tambien "AUF WIEDERSEHEN MONTY"). Con varios resultados pregunta
     * "ES ESTE?" por cada uno; N pasa al siguiente y, tras el ultimo, sale "NO HAY NINGUN PROGRAMA".
     * TODO: devolver todos los resultados (PENDIENTE.md 1.4) y no pasar el nombre a mayusculas (1.3).
     */
    public Optional<ProgramRecord> buscarPorNombre(String nombre) {
        String name = nombre.trim().toUpperCase();
        if (name.isEmpty()) throw new IllegalArgumentException("Nombre vacio");
        for (char c : name.toCharArray()) {
            if (!KeyboardDriver.isSupported(c))
                throw new IllegalArgumentException("Caracter no soportado en el nombre: '" + c + "'");
        }
        return locked(() -> {
            begin();
            expect(waitFor(() -> kb.typeChar('7'), SHORT_MS, "SABES EL NUMERO"), "SABES EL NUMERO");
            expect(waitFor(() -> kb.answer('N'), SHORT_MS, "NOMBRE DEL PROGRAMA"), "NOMBRE DEL PROGRAMA");
            // La aplicacion recorre los registros uno a uno: puede tardar varios segundos.
            String t = waitFor(() -> {
                kb.type(name);
                kb.enter();
            }, SEARCH_MS, "ES ESTE", "NO HAY NINGUN");
            if (has(t, "NO HAY NINGUN")) {
                expect(waitFor(kb::enter, SHORT_MS, "BUSCAR ALGUNO MAS"), "BUSCAR ALGUNO MAS");
                waitFor(() -> kb.answer('N'), SHORT_MS, "ACABAR");
                return Optional.<ProgramRecord>empty();
            }
            expect(t, "ES ESTE");
            ProgramRecord rec = ScreenParser.parseSingle(t)
                    .orElseThrow(() -> new LegacyException("No se pudo leer el programa:\n" + t));
            expect(waitFor(() -> kb.answer('S'), SHORT_MS, "ALTERAR"), "ALTERAR");
            expect(waitFor(() -> kb.answer('N'), SHORT_MS, "BUSCAR ALGUNO MAS"), "BUSCAR ALGUNO MAS");
            waitFor(() -> kb.answer('N'), SHORT_MS, "ACABAR");
            return Optional.of(rec);
        });
    }

    // ------------------------------------------------ caso de uso 3: por cinta

    /**
     * Opcion 6 (listado segun el orden, que debe ser ANTIGUEDAD = orden de grabacion) -> Enter
     * (desde el principio) -> se recorren todas las paginas con ESPACIO (18 filas por pagina) y se
     * filtran las filas cuya cinta contiene el identificador (A-45 pertenece a A y a 45).
     *
     * No se puede parar antes de la ultima pagina: los programas en varias cintas aparecen fuera de
     * su sitio (p. ej. el registro 20 esta en B-E). Cada pagina se valida con la numeracion
     * consecutiva y se reintenta el OCR con otros preprocesados si no cuadra.
     *
     * TODO: verificar como termina el listado (ultima pagina: prompt "PULSA SPACE..." o vuelta al menu).
     * TODO: si el orden no fuera ANTIGUEDAD habria que reordenar (opcion 3 -> 4, solo en memoria; no grabar).
     */
    public List<ProgramRecord> listarCinta(String cinta) {
        String id = cinta.trim().toUpperCase();
        if (!id.matches("[A-Z0-9]{1,3}")) throw new IllegalArgumentException("Cinta no valida: " + cinta);
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
            // "ENTER PARA VER DESDE EL PRINCIPIO": la primera pagina empieza en el registro 1
            BufferedImage img = screen.captureUntil(kb::enter, pageStartsAt(1, total), SHORT_MS);
            for (int page = 0; page < maxPages; page++) {
                final int first = expected;
                String text = screen.ocrBest(img,
                        t -> ScreenParser.checkPage(ScreenParser.parseListingPage(t), first).isEmpty());
                List<ProgramRecord> rows = ScreenParser.parseListingPage(text);
                List<String> problems = ScreenParser.checkPage(rows, first);
                if (!problems.isEmpty())
                    throw new LegacyException("Pagina " + (page + 1) + " ilegible: " + problems + "\n" + text);
                all.addAll(rows);
                expected = rows.get(rows.size() - 1).numero() + 1;
                if (expected > total) {
                    // Ultima pagina: cualquier tecla que no sea espacio (y tambien otro espacio) vuelve al menu.
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

    /**
     * Recorre el listado completo (opcion 6) y guarda cada pantalla tal cual se captura, sin OCR.
     * Sirve para construir el corpus con el que se mide el OCR y se aprende la fuente de DOSBox.
     * Solo pulsa 6, Enter y espacio: no modifica datos.
     */
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

    /** Foco en DOSBox y comprobacion de que estamos en el menu principal. */
    private void begin() {
        window.focus();
        if (!isMenu(screen.ocr(screen.captureStable()))) recoverToMenu();
    }

    private static boolean isMenu(String text) {
        return has(text, "BUSCAR DATOS") && has(text, "ACABAR");
    }

    /**
     * Recuperacion de emergencia (por ejemplo tras un error a mitad de una operacion): mira la
     * pantalla y responde lo que lleva al menu sin alterar datos. NUNCA envia 'S'; a toda pregunta
     * S/N se contesta 'N'. Si no reconoce la pantalla pulsa Enter.
     */
    private void recoverToMenu() {
        String t = screen.ocr(screen.captureStable());
        for (int i = 0; i < 10; i++) {
            if (isMenu(t)) return;
            final String screenText = t;
            if (has(t, "OTRA TECLA PARA ACABAR")) {           // listado: cualquier tecla que no sea espacio
                t = after(() -> kb.typeChar('X'));
            } else if (has(t, "ES ESTE") || has(t, "ALTERAR") || has(t, "BUSCAR ALGUNO MAS")
                    || has(t, "SABES EL NUMERO")) {            // preguntas S/N de la busqueda
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
        throw new LegacyException("La aplicacion legada no esta en el menu y no se pudo recuperar:\n" + t);
    }

    /**
     * Ejecuta la accion y espera hasta que en pantalla aparezca alguna de las frases (o se agote el
     * tiempo). Devuelve el texto de la pantalla; quien llama comprueba con {@link #expect} que es
     * la esperada, y si no lo es el mensaje de error incluye lo que habia en pantalla.
     */
    private String waitFor(Runnable action, int timeoutMs, String... phrases) {
        BufferedImage img = screen.captureUntil(action, t -> {
            for (String p : phrases) if (has(t, p)) return true;
            return false;
        }, timeoutMs);
        return screen.ocr(img);
    }

    /**
     * La pantalla es una pagina COMPLETA del listado que empieza en el registro {@code n}: tiene todas
     * sus filas (18, o las que queden hasta {@code total}). La aplicacion dibuja las filas poco a poco y
     * el aviso "PULSA SPACE" de la pagina anterior sigue en pantalla mientras tanto, asi que ninguna de
     * las dos cosas basta por separado para saber que la pagina ha terminado de pintarse.
     */
    private static java.util.function.Predicate<String> pageStartsAt(int n, int total) {
        int expectedRows = Math.min(ROWS_PER_PAGE, total - n + 1);
        return t -> {
            List<ProgramRecord> rows = ScreenParser.parseListingPage(t);
            return has(t, "PULSA SPACE") && rows.size() == expectedRows && rows.get(0).numero() == n;
        };
    }

    /** Ejecuta la accion y devuelve el texto OCR de la pantalla resultante. */
    private String after(Runnable action) {
        return screen.ocr(screen.captureAfter(action));
    }

    private static void expect(String text, String phrase) {
        if (!has(text, phrase))
            throw new LegacyException("Pantalla inesperada (se esperaba '" + phrase + "'):\n" + text);
    }
}
