package sl.p3;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Uso:
 *   java -jar target/wrapper-legacy.jar                 servidor web (por defecto; "servidor --abrir" abre el navegador)
 *   java -jar target/wrapper-legacy.jar calibrar        captura la ventana de DOSBox -> debug/calibracion.png + texto OCR
 *   java -jar target/wrapper-legacy.jar contar          caso de uso 1 por consola
 *   java -jar target/wrapper-legacy.jar programa MUGSY  caso de uso 2 por consola
 *   java -jar target/wrapper-legacy.jar cinta B         caso de uso 3 por consola
 *
 * Requisito: DOSBox abierto con la aplicacion (arrancar.bat o database.bat), visible y en el menu principal. Si
 * acaba de lanzarse, la primera operacion espera a que aparezca su ventana y el menu.
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        // Escalado de pantalla (p. ej. 150 %): Windows/JNA dan pixeles fisicos pero Robot usa logicos.
        // Con escala 1 de Java2D ambos coinciden. Debe fijarse antes de cualquier uso de AWT.
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
                // arrancar.bat pasa --abrir: el navegador se abre cuando el servidor ya escucha, y con el puerto real.
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
                Path dir = Path.of(args.length > 1 ? args[1] : "corpus");
                System.out.println("Pantallas guardadas: " + legacy.capturarCorpus(dir) + " en " + dir.toAbsolutePath());
            }
            case "bench" -> OcrBench.run(Path.of("corpus"), Path.of("ocr-resultados.md"), Path.of("capturas"));
            case "contar" -> System.out.println(legacy.contarRegistros());
            case "programa" -> legacy.buscarPorNombre(String.join(" ",
                    java.util.Arrays.copyOfRange(args, 1, args.length))).forEach(System.out::println);
            case "cinta" -> legacy.listarCinta(args[1]).forEach(System.out::println);
            default -> System.err.println("Comando desconocido: " + cmd);
        }
    }

    /** Sin acentos porque la consola de Windows (cmd) no usa UTF-8, no porque no sepamos. arrancar.bat comprueba lo mismo antes de abrir DOSBox. */
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
        } catch (Exception | Error e) { // sin escritorio o sin navegador por defecto: basta con la URL en la consola
            System.out.println("Abre " + url + " en el navegador.");
        }
    }
}
