package sl.p3;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import sl.p3.ScreenParser.ProgramRecord;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor web minimo (com.sun.net.httpserver, sin dependencias) con la API REST y los ficheros estaticos.
 *
 *   GET /api/registros          -> {"registros":763}
 *   GET /api/programa?nombre=X  -> {"resultados":[{"numero":..,"nombre":..,"tipo":..,"cinta":..,"registro":..,"exacto":..}]}
 *   GET /api/cinta?id=A         -> {"cinta":"A","resultados":[ ... ]}
 *   GET /                       -> index.html (de web.root)
 *
 * Cada peticion de la API consulta de verdad la aplicacion legada (no hay cache).
 */
public final class WebServer {
    private final Config cfg;
    private final LegacyService legacy;
    private HttpServer server;

    public WebServer(Config cfg, LegacyService legacy) {
        this.cfg = cfg;
        this.legacy = legacy;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(cfg.port), 0);
        server.createContext("/api/registros", ex -> api(ex, () ->
                "{\"registros\":" + legacy.contarRegistros() + "}"));
        server.createContext("/api/programa", ex -> api(ex, () -> {
            String nombre = param(ex, "nombre").trim();
            return "{\"resultados\":" + json(legacy.buscarPorNombre(nombre), nombre) + "}";
        }));
        server.createContext("/api/cinta", ex -> api(ex, () -> {
            String id = param(ex, "id");
            return "{\"cinta\":" + str(id.toUpperCase()) + ",\"resultados\":" + json(legacy.listarCinta(id), null) + "}";
        }));
        server.createContext("/", this::staticFile);
        // Un solo hilo: las peticiones se atienden de una en una (solo hay un DOSBox).
        server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ---------------------------------------------------------------- API

    private interface Body {
        String get() throws Exception;
    }

    private void api(HttpExchange ex, Body body) throws IOException {
        try {
            send(ex, 200, "application/json; charset=utf-8", body.get().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            send(ex, 400, "application/json; charset=utf-8", error(e));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "application/json; charset=utf-8", error(e));
        }
    }

    private static byte[] error(Exception e) {
        return ("{\"error\":" + str(String.valueOf(e.getMessage())) + "}").getBytes(StandardCharsets.UTF_8);
    }

    private static String param(HttpExchange ex, String name) {
        String q = ex.getRequestURI().getRawQuery();
        Map<String, String> m = new HashMap<>();
        if (q != null) {
            for (String kv : q.split("&")) {
                int i = kv.indexOf('=');
                if (i > 0) m.put(kv.substring(0, i), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
            }
        }
        String v = m.get(name);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Falta el parametro '" + name + "'");
        return v;
    }

    // ---------------------------------------------------------- estaticos

    private void staticFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = cfg.webRoot.resolve(path.substring(1)).normalize();
        if (!file.startsWith(cfg.webRoot) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain; charset=utf-8", "No encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(ex, 200, contentType(file.getFileName().toString()), Files.readAllBytes(file));
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, body.length);
        try (var out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    // --------------------------------------------------------------- JSON

    /**
     * Lista de programas en JSON. Si se pasa el nombre buscado, cada uno lleva {@code "exacto"}: true si
     * su nombre es exactamente ese (la aplicacion distingue mayusculas, asi que aqui tambien).
     */
    private static String json(List<ProgramRecord> list, String nombreBuscado) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            ProgramRecord r = list.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"numero\":").append(r.numero())
                    .append(",\"nombre\":").append(str(r.nombre()))
                    .append(",\"tipo\":").append(str(r.tipo()))
                    .append(",\"cinta\":").append(str(r.cinta()))
                    .append(",\"registro\":").append(r.registro());
            if (nombreBuscado != null) sb.append(",\"exacto\":").append(r.nombre().equals(nombreBuscado));
            sb.append('}');
        }
        return sb.append(']').toString();
    }

    private static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
