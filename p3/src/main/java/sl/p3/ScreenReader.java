package sl.p3;

import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;

/** Captura de la ventana de DOSBox, lectura (plantillas o Tesseract) y espera de pantallas. */
public final class ScreenReader {
    /** {escala, umbral} que se prueban con Tesseract si el texto no pasa la validación. */
    private static final int[][] VARIANTS = {{2, 0}, {3, 128}, {3, 160}, {2, 100}};

    private final Config cfg;
    private final DosboxWindow window;
    private final Robot robot;
    private final Tesseract1 tess;
    /** Lector por plantillas (null si ocr.engine=tesseract). */
    private final GlyphReader glyph;
    private int debugCounter = 0;

    public ScreenReader(Config cfg, DosboxWindow window) {
        this.cfg = cfg;
        this.window = window;
        this.glyph = cfg.ocrEngine.equalsIgnoreCase("glyph") ? new GlyphReader() : null;
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new IllegalStateException("No se puede crear java.awt.Robot", e);
        }
        this.tess = new Tesseract1();
        tess.setDatapath(cfg.tessdataPath);       // carpeta que contiene spa.traineddata
        tess.setLanguage(cfg.ocrLanguage);
        tess.setPageSegMode(6);                   // bloque uniforme de texto
        tess.setTessVariable("preserve_interword_spaces", "1"); // conserva las columnas
    }

    // ---------------------------------------------------------------- captura

    public Rectangle region() {
        if (cfg.captureW > 0 && cfg.captureH > 0) {
            return new Rectangle(cfg.captureX, cfg.captureY, cfg.captureW, cfg.captureH);
        }
        return window.clientArea();
    }

    public BufferedImage capture() {
        return robot.createScreenCapture(region());
    }

    /** Espera a que dos capturas seguidas sean iguales (como mucho stable.timeout.ms). */
    public BufferedImage captureStable() {
        return waitStable(capture());
    }

    /** Ejecuta la acción y devuelve la pantalla cuando cambia y se estabiliza (o al agotar change.timeout.ms). */
    public BufferedImage captureAfter(Runnable action) {
        BufferedImage before = capture();
        action.run();
        long deadline = System.currentTimeMillis() + cfg.changeTimeoutMs;
        BufferedImage current = before;
        while (System.currentTimeMillis() < deadline) {
            sleep(cfg.pollIntervalMs);
            current = capture();
            if (!same(before, current)) break;
        }
        return waitStable(current);
    }

    /**
     * Ejecuta la acción y espera a que el texto cumpla {@code ready} y se estabilice. Hace falta porque la búsqueda
     * tarda varios segundos sin que cambie la pantalla. Al agotar el tiempo devuelve la última captura.
     */
    public BufferedImage captureUntil(Runnable action, Predicate<String> ready, int timeoutMs) {
        action.run();
        long deadline = System.currentTimeMillis() + timeoutMs;
        BufferedImage img = capture();
        while (!ready.test(ocr(img)) && System.currentTimeMillis() < deadline) {
            sleep(cfg.pollIntervalMs);
            img = capture();
        }
        return waitStable(img);
    }

    private BufferedImage waitStable(BufferedImage start) {
        BufferedImage last = start;
        long deadline = System.currentTimeMillis() + cfg.stableTimeoutMs;
        while (System.currentTimeMillis() < deadline) {
            sleep(cfg.pollIntervalMs);
            BufferedImage next = capture();
            if (same(last, next)) return next;
            last = next;
        }
        // No es un error, pero ayuda a entender un fallo posterior.
        System.err.println("Aviso: la pantalla de DOSBox no llego a estabilizarse en " + cfg.stableTimeoutMs
                + " ms (sigue cambiando). Se usa la ultima captura.");
        return last;
    }

    static boolean sameImage(BufferedImage a, BufferedImage b) {
        return same(a, b);
    }

    private static boolean same(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        int w = a.getWidth(), h = a.getHeight();
        return Arrays.equals(a.getRGB(0, 0, w, h, null, 0, w), b.getRGB(0, 0, w, h, null, 0, w));
    }

    // -------------------------------------------------------------------- OCR

    /** Lee la pantalla con el motor y el preprocesado de la configuración. */
    public String ocr(BufferedImage img) {
        return ocrRaw(img, cfg.ocrScale, cfg.ocrBinarize);
    }

    /** Como {@link #ocr}, pero con Tesseract reintenta otros preprocesados si el texto no es válido. */
    public String ocrBest(BufferedImage img, Predicate<String> valid) {
        String text = ocr(img);
        if (valid.test(text) || glyph != null) return text; // el lector por plantillas es exacto: no hay variantes
        for (int[] v : VARIANTS) {
            text = ocrRaw(img, v[0], v[1]);
            if (valid.test(text)) return text;
        }
        return text;
    }

    private String ocrRaw(BufferedImage img, int scale, int threshold) {
        if (glyph != null) {
            saveDebug(img);
            return glyph.read(img);
        }
        BufferedImage prepared = cfg.ocrPreprocess.equalsIgnoreCase("rejilla")
                ? preprocessGrid(img, Math.max(1, scale), cfg.ocrColumnGap)
                : preprocess(img, Math.max(1, scale), threshold, cfg.ocrInvert);
        saveDebug(prepared);
        try {
            return tess.doOCR(prepared);
        } catch (TesseractException e) {
            throw new LegacyException("Fallo del OCR", e);
        }
    }

    /**
     * Preprocesado "rejilla" del modelo dos: escala sin suavizar, texto negro sobre blanco y {@code gap} píxeles
     * blancos entre columnas, para que Tesseract no parta las letras ni meta espacios. TrainingData usa esta misma
     * función: si cambia, hay que volver a entrenar.
     */
    static BufferedImage preprocessGrid(BufferedImage src, int scale, int gap) {
        int cw = 8 * scale, w = src.getWidth() * scale, h = src.getHeight() * scale;
        BufferedImage big = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = big.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        int cols = w / cw;
        BufferedImage out = new BufferedImage(cols * (cw + gap), h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                int col = x / (cw + gap), dx = x % (cw + gap);
                int v = dx < cw ? 255 - big.getRaster().getSample(col * cw + dx, y, 0) : 255;
                out.getRaster().setSample(x, y, 0, v);
            }
        }
        return out;
    }

    /** Escala, pasa a grises, (opcional) binariza e invierte. */
    static BufferedImage preprocess(BufferedImage src, int scale, int threshold, boolean invert) {
        int w = src.getWidth() * scale, h = src.getHeight() * scale;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        if (threshold > 0 || invert) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int v = out.getRaster().getSample(x, y, 0);
                    if (threshold > 0) v = v > threshold ? 255 : 0;
                    if (invert) v = 255 - v;
                    out.getRaster().setSample(x, y, 0, v);
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------- utilidades

    private void saveDebug(BufferedImage img) {
        if (!cfg.debugSave) return;
        try {
            Files.createDirectories(cfg.debugDir);
            Path file = cfg.debugDir.resolve(String.format("ocr-%04d.png", ++debugCounter));
            ImageIO.write(img, "png", file.toFile());
        } catch (IOException e) {
            // la depuración nunca debe romper la petición
        }
    }

    /** Guarda una captura en disco (para calibrar la zona de captura). */
    public void save(BufferedImage img, Path file) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        ImageIO.write(img, "png", file.toFile());
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LegacyException("Interrumpido", e);
        }
    }
}
