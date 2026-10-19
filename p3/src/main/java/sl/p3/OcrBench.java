package sl.p3;

import net.sourceforge.tess4j.Tesseract1;
import sl.p3.ScreenParser.ProgramRecord;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramienta de desarrollo (comando bench): mide Tesseract con distintas configuraciones sobre las pantallas de
 * corpus, tomando como referencia el lector por plantillas. Escribe ocr-resultados.md y las imágenes de capturas/.
 */
public final class OcrBench {
    private record Cfg(String name, int scale, int threshold, boolean invert) {}

    private static final List<Cfg> CONFIGS = List.of(
            new Cfg("Escala 1, grises", 1, 0, false),
            new Cfg("Escala 2, grises (defecto)", 2, 0, false),
            new Cfg("Escala 3, grises", 3, 0, false),
            new Cfg("Escala 4, grises", 4, 0, false),
            new Cfg("Escala 2, binarizado 128", 2, 128, false),
            new Cfg("Escala 3, binarizado 128", 3, 128, false),
            new Cfg("Escala 3, binarizado 160", 3, 160, false),
            new Cfg("Escala 4, binarizado 128", 4, 128, false),
            new Cfg("Escala 2, invertido", 2, 0, true),
            new Cfg("Escala 3, invertido y binarizado 128", 3, 128, true));

    /** Páginas de muestra, repartidas por todo el listado. */
    static final int[] SAMPLE_PAGES = {1, 5, 10, 15, 20, 25, 30, 35, 40, 43};

    private OcrBench() {}

    public static void run(Path corpus, Path outMd, Path capturas) throws Exception {
        GlyphReader truthReader = new GlyphReader();
        Tesseract1 tess = new Tesseract1();
        tess.setDatapath("tessdata");
        tess.setLanguage("spa");
        tess.setPageSegMode(6);
        tess.setTessVariable("preserve_interword_spaces", "1");

        List<BufferedImage> images = new ArrayList<>();
        List<String> truths = new ArrayList<>();
        for (int p : SAMPLE_PAGES) {
            BufferedImage img = ImageIO.read(corpus.resolve(String.format("listado-%02d.png", p)).toFile());
            images.add(img);
            truths.add(truthReader.read(img));
        }

        StringBuilder md = new StringBuilder();
        md.append("# Resultados de Tesseract sobre el corpus real\n\n");
        md.append("Referencia: texto del lector por plantillas (exacto). Muestra: ").append(SAMPLE_PAGES.length)
                .append(" paginas del listado repartidas por los 763 registros.\n\n");
        md.append("| Configuración | Precisión de caracteres | Filas exactas | Páginas válidas | ms/página |\n");
        md.append("|---|---|---|---|---|\n");

        int totalRows = 0;
        for (String t : truths) totalRows += ScreenParser.parseListingPage(t).size();

        Cfg best = null;
        double bestScore = -1;
        List<String[]> tableRows = new ArrayList<>();
        for (Cfg c : CONFIGS) {
            double acc = 0;
            int rowsOk = 0, pagesOk = 0;
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < images.size(); i++) {
                String text = tess.doOCR(ScreenReader.preprocess(images.get(i), c.scale, c.threshold, c.invert));
                acc += charAccuracy(text, truths.get(i));
                List<ProgramRecord> truthRows = ScreenParser.parseListingPage(truths.get(i));
                List<ProgramRecord> got = ScreenParser.parseListingPage(text);
                int ok = 0;
                for (ProgramRecord r : truthRows) if (got.contains(r)) ok++;
                rowsOk += ok;
                if (ok == truthRows.size() && got.size() == truthRows.size()) pagesOk++;
            }
            long ms = (System.currentTimeMillis() - t0) / images.size();
            double avg = 100.0 * acc / images.size();
            String[] row = {c.name, String.format("%.1f %%", avg), rowsOk + " de " + totalRows,
                    pagesOk + " de " + images.size(), String.valueOf(ms)};
            tableRows.add(row);
            md.append("| ").append(String.join(" | ", row)).append(" |\n");
            System.out.println(String.join(" | ", row));
            if (rowsOk > bestScore) {
                bestScore = rowsOk;
                best = c;
            }
        }
        md.append("\nMejor configuración por filas exactas: **").append(best.name).append("**.\n");

        // La mejor configuración, sobre las 43 páginas (763 registros).
        int rows763 = 0, pages = 0, pagesOk = 0;
        for (int p = 1; p <= 43; p++) {
            BufferedImage img = ImageIO.read(corpus.resolve(String.format("listado-%02d.png", p)).toFile());
            String truth = truthReader.read(img);
            String text = tess.doOCR(ScreenReader.preprocess(img, best.scale, best.threshold, best.invert));
            List<ProgramRecord> truthRows = ScreenParser.parseListingPage(truth);
            List<ProgramRecord> got = ScreenParser.parseListingPage(text);
            int ok = 0;
            for (ProgramRecord r : truthRows) if (got.contains(r)) ok++;
            rows763 += ok;
            pages++;
            if (ok == truthRows.size() && got.size() == truthRows.size()) pagesOk++;
        }
        md.append("\nCon esa configuración sobre el listado completo: **").append(rows763)
                .append(" de 763** registros exactos y **").append(pagesOk).append(" de ").append(pages)
                .append("** páginas sin ningún error.\n");
        System.out.println("Completo (" + best.name + "): " + rows763 + "/763 filas, " + pagesOk + "/" + pages + " paginas");

        tuningAndTraining(corpus, images, truths, totalRows, md);
        if (outMd.getParent() != null) Files.createDirectories(outMd.getParent());
        Files.writeString(outMd, md.toString(), StandardCharsets.UTF_8);

        // ---------------- imágenes para la memoria
        Files.createDirectories(capturas);
        BufferedImage raw = ImageIO.read(corpus.resolve("listado-01.png").toFile());
        ImageIO.write(raw, "png", capturas.resolve("10-captura-cruda.png").toFile());
        ImageIO.write(ScreenReader.preprocess(raw, 2, 0, false), "png",
                capturas.resolve("11-captura-preprocesada.png").toFile());

        BufferedImage menu = ImageIO.read(corpus.resolve("menu.png").toFile());
        String menuTruth = truthReader.read(menu);
        ImageIO.write(compare(menu, menuTruth, menuTruth, "Lector por plantillas (motor final): menú, sin errores"),
                "png", capturas.resolve("12-ocr-texto-bueno.png").toFile());

        BufferedImage page = ImageIO.read(corpus.resolve("listado-14.png").toFile());
        String pageTruth = truthReader.read(page);
        String defaultText = tess.doOCR(ScreenReader.preprocess(page, 2, 0, false));
        ImageIO.write(compare(page, defaultText, pageTruth, "Tesseract, escala 2 sin binarizar (esqueleto): errores en rojo"),
                "png", capturas.resolve("13-ocr-texto-con-errores.png").toFile());

        ImageIO.write(tableImage(tableRows, "Tesseract: precisión según la configuración (referencia: lector por plantillas)"),
                "png", capturas.resolve("14-tabla-comparativa-ocr.png").toFile());
        System.out.println("Imagenes guardadas en " + capturas.toAbsolutePath());
    }

    // ------------------------------------------- ajustes y modelo entrenado

    /**
     * Segunda tabla: ajustes sin entrenar y modelo entrenado. En las páginas de muestra se usa dos-prueba, que
     * no las ha visto; dos (entrenado con todas) solo se mide sobre el listado completo.
     */
    private static void tuningAndTraining(Path corpus, List<BufferedImage> images, List<String> truths, int totalRows,
                                          StringBuilder md) throws Exception {
        StringBuilder chars = new StringBuilder();
        for (String t : truths)
            for (char ch : t.toCharArray()) if (ch > ' ' && chars.indexOf(String.valueOf(ch)) < 0) chars.append(ch);

        Tesseract1 tuned = tesseract("tessdata", "spa");
        tuned.setConfigs(List.of(Path.of("entrenamiento", "sin-diccionarios.cfg").toAbsolutePath().toString()));
        tuned.setTessVariable("tessedit_char_whitelist", chars.toString());
        Tesseract1 held = tesseract("entrenamiento", "dos-prueba");
        Tesseract1 dos = tesseract("tessdata", "dos");

        md.append("\n## Ajustes de Tesseract y modelo entrenado\n\n");
        md.append("Mismas páginas de muestra. El modelo entrenado que se mide aquí (`dos-prueba`) no ha visto estas "
                + "páginas.\n\n");
        md.append("| Configuración | Precisión de caracteres | Filas exactas | Páginas válidas | ms/página |\n");
        md.append("|---|---|---|---|---|\n");
        row(md, "spa, escala 2 sin suavizar, sin diccionarios y con lista blanca", tuned,
                img -> ScreenReader.preprocessGrid(img, 2, 0), images, truths, totalRows);
        row(md, "Entrenado (dos-prueba), rejilla: escala 2 sin suavizar y hueco de 4 px", held,
                img -> ScreenReader.preprocessGrid(img, 2, TrainingData.GAP), images, truths, totalRows);

        md.append("\nSobre el listado completo (763 registros), con la rejilla:\n\n");
        md.append("- `dos-prueba` (sin las páginas de muestra): ").append(full(corpus, held)).append('\n');
        md.append("- `dos` (todas las pantallas, el que usa la aplicación): ").append(full(corpus, dos)).append('\n');
    }

    private static Tesseract1 tesseract(String datapath, String lang) {
        Tesseract1 t = new Tesseract1();
        t.setDatapath(datapath);
        t.setLanguage(lang);
        t.setPageSegMode(6);
        t.setTessVariable("preserve_interword_spaces", "1");
        return t;
    }

    private static void row(StringBuilder md, String name, Tesseract1 t,
                            java.util.function.UnaryOperator<BufferedImage> prep, List<BufferedImage> images,
                            List<String> truths, int totalRows) throws Exception {
        double acc = 0;
        int rowsOk = 0, pagesOk = 0;
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < images.size(); i++) {
            String text = t.doOCR(prep.apply(images.get(i)));
            acc += charAccuracy(text, truths.get(i));
            List<ProgramRecord> truthRows = ScreenParser.parseListingPage(truths.get(i));
            List<ProgramRecord> got = ScreenParser.parseListingPage(text);
            int ok = 0;
            for (ProgramRecord r : truthRows) if (got.contains(r)) ok++;
            rowsOk += ok;
            if (ok == truthRows.size() && got.size() == truthRows.size()) pagesOk++;
        }
        String line = String.format("| %s | %.1f %% | %d de %d | %d de %d | %d |", name,
                100.0 * acc / images.size(), rowsOk, totalRows, pagesOk, images.size(),
                (System.currentTimeMillis() - t0) / images.size());
        md.append(line).append('\n');
        System.out.println(line);
    }

    private static String full(Path corpus, Tesseract1 t) throws Exception {
        GlyphReader truthReader = new GlyphReader();
        int ok = 0, pagesOk = 0;
        for (int p = 1; p <= 43; p++) {
            BufferedImage img = ImageIO.read(corpus.resolve(String.format("listado-%02d.png", p)).toFile());
            List<ProgramRecord> truthRows = ScreenParser.parseListingPage(truthReader.read(img));
            List<ProgramRecord> got = ScreenParser.parseListingPage(
                    t.doOCR(ScreenReader.preprocessGrid(img, 2, TrainingData.GAP)));
            int k = 0;
            for (ProgramRecord r : truthRows) if (got.contains(r)) k++;
            ok += k;
            if (k == truthRows.size() && got.size() == truthRows.size()) pagesOk++;
        }
        String s = "**" + ok + " de 763** registros exactos y **" + pagesOk + " de 43** páginas sin errores";
        System.out.println(s);
        return s;
    }

    // -------------------------------------------------------------- métricas

    private static String norm(String s) {
        StringBuilder sb = new StringBuilder();
        for (String line : s.split("\\R")) {
            String l = line.trim().replaceAll("\\s+", " ");
            if (!l.isEmpty()) sb.append(l).append('\n');
        }
        return sb.toString();
    }

    /** 1 - distancia de edición / longitud de la referencia (con los espacios normalizados). */
    static double charAccuracy(String got, String truth) {
        String a = norm(got), b = norm(truth);
        return Math.max(0, 1.0 - (double) ScreenParser.levenshtein(a, b) / Math.max(1, b.length()));
    }

    // ----------------------------------------------------------- imágenes

    private static BufferedImage compare(BufferedImage screen, String text, String truth, String title) {
        int w = screen.getWidth(), h = screen.getHeight();
        BufferedImage out = new BufferedImage(w * 2 + 30, h + 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString(title, 10, 22);
        g.drawImage(screen, 10, 40, null);
        g.setColor(Color.BLACK);
        g.fillRect(w + 20, 40, w, h);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        String[] lines = text.split("\\R", -1);
        String[] ref = truth.split("\\R", -1);
        for (int i = 0; i < Math.min(lines.length, 25); i++) {
            String l = lines[i];
            String r = i < ref.length ? ref[i] : "";
            boolean same = l.trim().replaceAll("\\s+", " ").equals(r.trim().replaceAll("\\s+", " "));
            g.setColor(same ? new Color(200, 200, 200) : new Color(255, 90, 90));
            g.drawString(l.length() > 80 ? l.substring(0, 80) : l, w + 24, 40 + 14 + i * 16);
        }
        g.dispose();
        return out;
    }

    private static BufferedImage tableImage(List<String[]> rows, String title) {
        String[] head = {"Configuración", "Caracteres", "Filas exactas", "Páginas válidas", "ms/página"};
        int[] colW = {400, 130, 140, 150, 100};
        int rowH = 30, width = 0;
        for (int c : colW) width += c;
        BufferedImage out = new BufferedImage(width + 20, (rows.size() + 2) * rowH + 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString(title, 10, 25);
        int y = 40;
        g.fillRect(10, y, width, rowH);
        g.setColor(Color.WHITE);
        int x = 10;
        for (int c = 0; c < head.length; c++) {
            g.drawString(head[c], x + 8, y + 20);
            x += colW[c];
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        for (int r = 0; r < rows.size(); r++) {
            y += rowH;
            g.setColor(r % 2 == 0 ? new Color(240, 240, 240) : Color.WHITE);
            g.fillRect(10, y, width, rowH);
            g.setColor(Color.BLACK);
            x = 10;
            for (int c = 0; c < head.length; c++) {
                g.drawString(rows.get(r)[c], x + 8, y + 20);
                x += colW[c];
            }
        }
        g.dispose();
        return out;
    }
}
