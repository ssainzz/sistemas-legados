package sl.p3;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lector de pantalla por plantillas de caracter (alternativa al OCR de Tesseract).
 *
 * DOSBox dibuja el modo texto de GW-BASIC como una cuadrilla exacta de 80x25 celdas de 8x16 pixeles
 * y siempre con la misma fuente, sin suavizado. En lugar de "reconocer" formas, cada celda se compara
 * con el dibujo de cada caracter conocido (font8x16.txt) y se elige el de menos pixeles distintos.
 * El resultado es exacto y no depende de escala, binarizado ni entrenamiento.
 *
 * Una celda que no se parece a nada (distancia > MAX_DISTANCE) se devuelve como '#', para que un
 * caracter desconocido se note en lugar de convertirse en otro distinto.
 */
public final class GlyphReader {
    public static final int COLS = 80, ROWS = 25, CELL_W = 8, CELL_H = 16;
    static final char UNKNOWN = '#';
    private static final int MAX_DISTANCE = 24;
    private static final int LUMINANCE_THRESHOLD = 80;

    private record Glyph(char ch, long hi, long lo) {}

    private final List<Glyph> glyphs = new ArrayList<>();

    public GlyphReader() {
        glyphs.add(new Glyph(' ', 0, 0));
        try (InputStream in = GlyphReader.class.getResourceAsStream("/font8x16.txt")) {
            if (in == null) throw new IllegalStateException("Falta el recurso font8x16.txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int sep = line.indexOf('|');
                long[] bits = parseHex(line.substring(sep + 1).trim());
                glyphs.add(new Glyph(line.charAt(0), bits[0], bits[1]));
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer font8x16.txt", e);
        }
    }

    /** Texto de la pantalla: 25 lineas de 80 caracteres, sin espacios al final de cada linea. */
    public String read(BufferedImage img) {
        if (img.getWidth() != COLS * CELL_W || img.getHeight() != ROWS * CELL_H) {
            throw new LegacyException("La captura mide " + img.getWidth() + "x" + img.getHeight()
                    + " y se esperaba " + COLS * CELL_W + "x" + ROWS * CELL_H
                    + " (zona cliente de DOSBox sin escalar)");
        }
        StringBuilder out = new StringBuilder();
        for (int row = 0; row < ROWS; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < COLS; col++) line.append(readCell(img, col, row));
            out.append(line.toString().stripTrailing()).append('\n');
        }
        return out.toString();
    }

    private char readCell(BufferedImage img, int col, int row) {
        long hi = 0, lo = 0;
        for (int y = 0; y < CELL_H; y++) {
            int bitsRow = 0;
            for (int x = 0; x < CELL_W; x++) {
                int rgb = img.getRGB(col * CELL_W + x, row * CELL_H + y);
                int lum = ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255)) / 3;
                bitsRow = (bitsRow << 1) | (lum > LUMINANCE_THRESHOLD ? 1 : 0);
            }
            if (y < 8) hi = (hi << 8) | bitsRow;
            else lo = (lo << 8) | bitsRow;
        }
        char best = UNKNOWN;
        int bestDist = Integer.MAX_VALUE;
        for (Glyph g : glyphs) {
            int d = Long.bitCount(g.hi ^ hi) + Long.bitCount(g.lo ^ lo);
            if (d < bestDist) {
                bestDist = d;
                best = g.ch;
            }
        }
        return bestDist <= MAX_DISTANCE ? best : UNKNOWN;
    }

    private static long[] parseHex(String hex32) {
        return new long[]{Long.parseUnsignedLong(hex32.substring(0, 16), 16),
                Long.parseUnsignedLong(hex32.substring(16, 32), 16)};
    }
}
