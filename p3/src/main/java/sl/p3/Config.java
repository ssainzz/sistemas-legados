package sl.p3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuracion del wrapper. Lee config.properties (opcional) y aplica valores por defecto. */
public final class Config {
    public final String windowTitlePrefix;
    public final int captureX, captureY, captureW, captureH;
    public final int keyDelayMs, pollIntervalMs, changeTimeoutMs, stableTimeoutMs;
    public final String tessdataPath, ocrLanguage, ocrEngine;
    public final int ocrScale, ocrBinarize;
    public final boolean ocrInvert;
    public final int port;
    public final Path webRoot;
    public final boolean debugSave;
    public final Path debugDir;

    private Config(Properties p) {
        windowTitlePrefix = p.getProperty("window.title.prefix", "DOSBox 0.74");
        captureX = i(p, "capture.x", 0);
        captureY = i(p, "capture.y", 0);
        captureW = i(p, "capture.width", 0);
        captureH = i(p, "capture.height", 0);
        keyDelayMs = i(p, "key.delay.ms", 60);
        pollIntervalMs = i(p, "poll.interval.ms", 150);
        changeTimeoutMs = i(p, "change.timeout.ms", 4000);
        stableTimeoutMs = i(p, "stable.timeout.ms", 5000);
        tessdataPath = p.getProperty("tessdata.path", ".");
        ocrEngine = p.getProperty("ocr.engine", "glyph");
        ocrLanguage = p.getProperty("ocr.language", "spa");
        ocrScale = i(p, "ocr.scale", 2);
        ocrBinarize = i(p, "ocr.binarize", 0);
        ocrInvert = Boolean.parseBoolean(p.getProperty("ocr.invert", "false"));
        port = i(p, "server.port", 8090);
        webRoot = Path.of(p.getProperty("web.root", ".")).toAbsolutePath().normalize();
        debugSave = Boolean.parseBoolean(p.getProperty("debug.save", "false"));
        debugDir = Path.of(p.getProperty("debug.dir", "debug"));
    }

    public static Config load(Path file) {
        Properties p = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + file, e);
            }
        }
        return new Config(p);
    }

    private static int i(Properties p, String key, int def) {
        String v = p.getProperty(key);
        return v == null || v.isBlank() ? def : Integer.parseInt(v.trim());
    }
}
