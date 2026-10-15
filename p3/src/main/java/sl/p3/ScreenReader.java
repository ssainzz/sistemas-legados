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

/** Captura de pantalla de la ventana de DOSBox + OCR (Tess4J / Tesseract1) + sincronizacion. */
public final class ScreenReader {
    /** Variantes de preprocesado que se prueban en orden si el texto no pasa la validacion: {escala, umbral}. */
    private static final int[][] VARIANTS = {{2, 0}, {3, 128}, {3, 160}, {2, 100}};

    private final Config cfg;
    private final DosboxWindow window;
    private final Robot robot;
    private final Tesseract1 tess;
    private int debugCounter = 0;

    public ScreenReader(Config cfg, DosboxWindow window) {
        this.cfg = cfg;
        this.window = window;
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

    /** Espera hasta que dos capturas consecutivas sean identicas (o se agote stable.timeout.ms). */
    public BufferedImage captureStable() {
        return waitStable(capture());
    }

    /**
     * Ejecuta una accion de teclado y devuelve la pantalla resultante: espera a que la imagen CAMBIE
     * respecto a la anterior (hasta change.timeout.ms) y despues a que se ESTABILICE.
     * Si la pantalla no cambia (tecla ignorada, misma pantalla) devuelve la captura tras el timeout.
     */
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

    private BufferedImage waitStable(BufferedImage start) {
        BufferedImage last = start;
        long deadline = System.currentTimeMillis() + cfg.stableTimeoutMs;
        while (System.currentTimeMillis() < deadline) {
            sleep(cfg.pollIntervalMs);
            BufferedImage next = capture();
            if (same(last, next)) return next;
            last = next;
        }
        return last; // TODO: avisar en el log de que la pantalla no llego a estabilizarse
    }

    private static boolean same(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        int w = a.getWidth(), h = a.getHeight();
        return Arrays.equals(a.getRGB(0, 0, w, h, null, 0, w), b.getRGB(0, 0, w, h, null, 0, w));
    }

    // -------------------------------------------------------------------- OCR

    /** OCR con el preprocesado de la configuracion. */
    public String ocr(BufferedImage img) {
        return ocrRaw(img, cfg.ocrScale, cfg.ocrBinarize);
    }

    /**
     * OCR con reintentos: prueba varias combinaciones de escala/binarizado hasta que el texto
     * cumpla la validacion (por ejemplo, numeracion consecutiva en un listado). Devuelve el ultimo
     * intento si ninguno es valido.
     */
    public String ocrBest(BufferedImage img, Predicate<String> valid) {
        String text = ocr(img);
        if (valid.test(text)) return text;
        for (int[] v : VARIANTS) {
            text = ocrRaw(img, v[0], v[1]);
            if (valid.test(text)) return text;
        }
        return text;
    }

    private String ocrRaw(BufferedImage img, int scale, int threshold) {
        BufferedImage prepared = preprocess(img, Math.max(1, scale), threshold, cfg.ocrInvert);
        saveDebug(prepared);
        try {
            return tess.doOCR(prepared);
        } catch (TesseractException e) {
            throw new LegacyException("Fallo del OCR", e);
        }
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
            // la depuracion nunca debe romper la peticion
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
