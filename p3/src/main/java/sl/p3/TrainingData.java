package sl.p3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramienta de desarrollo (comando cajas [--prueba]): datos para entrenar Tesseract con las pantallas del corpus.
 * Por pantalla, la imagen con el preprocesado "rejilla" y su .box (cada carácter con su recuadro), sacado del lector
 * por plantillas. --prueba deja fuera las páginas de muestra del bench. Se entrena con entrenamiento/entrenar.sh.
 */
public final class TrainingData {
    static final int SCALE = 2, GAP = 4;

    private TrainingData() {}

    public static void write(Path corpus, Path out, boolean excludeSample) throws IOException {
        Files.createDirectories(out);
        List<String> sample = new ArrayList<>();
        for (int p : OcrBench.SAMPLE_PAGES) sample.add(String.format("listado-%02d.png", p));
        List<File> files = new ArrayList<>(List.of(corpus.toFile().listFiles((d, n) -> n.endsWith(".png"))));
        files.addAll(List.of(corpus.resolve("mensajes").toFile().listFiles((d, n) -> n.endsWith(".png"))));
        files.sort(null);
        GlyphReader reader = new GlyphReader();
        int n = 0, chars = 0;
        for (File f : files) {
            if (excludeSample && sample.contains(f.getName())) continue;
            BufferedImage img = ImageIO.read(f);
            String[] lines = reader.read(img).split("\n", -1);
            BufferedImage pre = ScreenReader.preprocessGrid(img, SCALE, GAP);
            StringBuilder box = new StringBuilder();
            for (int row = 0; row < GlyphReader.ROWS && row < lines.length; row++) {
                String line = lines[row];
                for (int col = 0; col < line.length(); col++) {
                    if (line.charAt(col) == ' ') continue;
                    String b = boxLine(pre, line.charAt(col), col, row);
                    if (b != null) {
                        box.append(b);
                        chars++;
                    }
                }
            }
            // Nombre que espera el entrenamiento: idioma.fuente.expN
            String name = String.format("dos.dosbox.exp%d", n++);
            ImageIO.write(pre, "png", out.resolve(name + ".png").toFile());
            Files.writeString(out.resolve(name + ".box"), box.toString(), StandardCharsets.UTF_8);
        }
        System.out.println(n + " pantallas y " + chars + " caracteres en " + out.toAbsolutePath()
                + (excludeSample ? " (sin las paginas de muestra del bench)" : ""));
    }

    /** "c x1 y1 x2 y2 0": recuadro de los píxeles negros de la celda (la y se cuenta desde abajo). */
    private static String boxLine(BufferedImage pre, char ch, int col, int row) {
        int cw = GlyphReader.CELL_W * SCALE, chh = GlyphReader.CELL_H * SCALE, h = pre.getHeight();
        int x0 = col * (cw + GAP), y0 = row * chh;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int y = y0; y < y0 + chh; y++) {
            for (int x = x0; x < x0 + cw; x++) {
                if (pre.getRaster().getSample(x, y, 0) < 128) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) return null;
        return ch + " " + minX + " " + (h - 1 - maxY) + " " + (maxX + 1) + " " + (h - minY) + " 0\n";
    }
}
