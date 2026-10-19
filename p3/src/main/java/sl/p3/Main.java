package sl.p3;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Uso: java -jar target/wrapper-legacy.jar [comando]
 *   servidor [--abrir]  servidor web (por defecto); --abrir abre el navegador
 *   contar | programa NOMBRE | cinta ID   los tres casos de uso por consola
 *   calibrar            guarda la captura de DOSBox en debug/ y muestra el texto leído
 *   corpus | bench | cajas                herramientas de desarrollo (pantallas, medida del OCR, entrenamiento)
 *
 * DOSBox debe estar abierto con la aplicación (arrancar.bat lo abre).
 */
public final class Main {
    /** Pantallas reales de DOSBox (las usan los tests, el bench y el entrenamiento). */
    static final Path CORPUS = Path.of("src", "test", "resources", "corpus");

    public static void main(String[] args) throws Exception {
        // Con escalado de pantalla (p. ej. 150 %) Windows da píxeles físicos y Robot lógicos: con escala 1 coinciden.
        // Debe fijarse antes de usar AWT.
        System.setProperty("sun.java2d.uiScale", "1");
        Config cfg = Config.load(Path.of("config.properties"));
        LegacyService legacy = new LegacyService(cfg);
        String cmd = args.length > 0 ? args[0] : "servidor";

        switch (cmd) {
            case "servidor" -> {
                try {
                    new WebServer(cfg, legacy).start();
                } catch (java.net.BindException e) {
                    System.err.println(puertoOcupado(cfg.port));
                    System.exit(1);
                }
                String url = "http://localhost:" + cfg.port + "/";
                System.out.println("Servidor en " + url + "  (Ctrl+C para parar)");
                // El navegador se abre cuando el servidor ya escucha.
                if (java.util.Arrays.asList(args).contains("--abrir")) abrirNavegador(url);
            }
            case "calibrar" -> {
                ScreenReader sr = legacy.screenReader();
                legacy.window().focus();
                Thread.sleep(500);
                BufferedImage img = sr.captureStable();
                Path out = cfg.debugDir.resolve("calibracion.png");
                sr.save(img, out);
                System.out.println("Zona de captura: " + sr.region());
                System.out.println("Imagen guardada en " + out.toAbsolutePath());
                System.out.println("--- texto OCR ---\n" + sr.ocr(img));
            }
            case "corpus" -> {
                Path dir = args.length > 1 ? Path.of(args[1]) : CORPUS;
                System.out.println("Pantallas guardadas: " + legacy.capturarCorpus(dir) + " en " + dir.toAbsolutePath());
            }
            case "bench" -> OcrBench.run(CORPUS, Path.of("ocr-resultados.md"), Path.of("capturas"));
            case "cajas" -> TrainingData.write(CORPUS, Path.of("entrenamiento", "datos"),
                    java.util.Arrays.asList(args).contains("--prueba"));
            case "contar" -> System.out.println(legacy.contarRegistros());
            case "programa" -> legacy.buscarPorNombre(String.join(" ",
                    java.util.Arrays.copyOfRange(args, 1, args.length))).forEach(System.out::println);
            case "cinta" -> legacy.listarCinta(args[1]).forEach(System.out::println);
            default -> System.err.println("Comando desconocido: " + cmd);
        }
    }

    /** Sin tildes: la consola de Windows no usa UTF-8. arrancar.bat ya lo comprueba antes de abrir DOSBox. */
    static String puertoOcupado(int port) {
        return "\nERROR: el puerto " + port + " ya lo esta usando otro programa y el servidor no puede arrancar.\n"
                + "  - Si ya hay otra ventana de arrancar.bat abierta, usa esa (http://localhost:" + port + "/)\n"
                + "    o cierrala antes de volver a arrancar.\n"
                + "  - Si no, abre config.properties, cambia server.port=" + port + " por otro puerto libre\n"
                + "    (por ejemplo server.port=" + (port + 1) + ") y vuelve a hacer doble clic en arrancar.bat.\n";
    }

    private static void abrirNavegador(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception | Error e) { // sin navegador por defecto: basta con la URL
            System.out.println("Abre " + url + " en el navegador.");
        }
    }
}
