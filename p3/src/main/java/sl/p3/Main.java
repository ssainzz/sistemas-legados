package sl.p3;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Uso:
 *   java -jar target/wrapper-legacy.jar                 servidor web (por defecto)
 *   java -jar target/wrapper-legacy.jar calibrar        captura la ventana de DOSBox -> debug/calibracion.png + texto OCR
 *   java -jar target/wrapper-legacy.jar contar          caso de uso 1 por consola
 *   java -jar target/wrapper-legacy.jar programa MUGSY  caso de uso 2 por consola
 *   java -jar target/wrapper-legacy.jar cinta B         caso de uso 3 por consola
 *
 * Requisito: DOSBox abierto con la aplicacion (database.bat), visible y en el menu principal.
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
                new WebServer(cfg, legacy).start();
                System.out.println("Servidor en http://localhost:" + cfg.port + "/  (Ctrl+C para parar)");
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
            case "contar" -> System.out.println(legacy.contarRegistros());
            case "programa" -> System.out.println(legacy.buscarPorNombre(String.join(" ",
                    java.util.Arrays.copyOfRange(args, 1, args.length))));
            case "cinta" -> legacy.listarCinta(args[1]).forEach(System.out::println);
            default -> System.err.println("Comando desconocido: " + cmd);
        }
    }
}
