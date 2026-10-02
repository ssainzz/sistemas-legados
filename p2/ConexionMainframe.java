import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConexionMainframe {

    private static final String HOST = "155.210.152.51:3270";
    private static final String USUARIO = "grupo_07";
    private static final String CLAVE = "secreto6";
    private static final String PROGRAMA = "tareas.c";

    // Textos de las pantallas del mainframe
    private static final String PROMPT_SO = "*Go";          // MUSIC/SP esperando un comando
    private static final String PROMPT_FSI = "Command ===>"; // menú a pantalla completa de MUSIC/SP (FSI)
    private static final String MARCA_MAS = "More...";      // pantalla llena: hay que pulsar ENTER
    private static final String MARCA_LOGIN = "userid";     // pantalla de identificación
    private static final String COMANDO_SALIR_SO = "off";
    private static final String MARCA_OCUPADO = "Working";  // el mainframe aún está procesando
    private static final String REGLA = "------T";          // regla que separa la salida de la línea de estado
    // Palabras que identifican cada opción de menú y cada pregunta
    private static final String MENU_ASIGNAR = "ASSIGN|ASIGNAR";
    private static final String MENU_VER = "\\b(VIEW|VER)\\b";
    private static final String MENU_SALIR = "EXIT|SALIR";
    private static final String MENU_VOLVER = "MENU|MAIN|PRINCIPAL|BACK|RETURN|VOLVER";
    private static final String MENU_GENERAL = "GENERAL";
    private static final String MENU_ESPECIFICA = "SPECIFIC|ESPECIFIC";
    private static final String MENU_TODAS = "\\b(ALL|TODAS)\\b";
    private static final String CAMPO_FECHA = "DATE|FECHA|DDMM";
    private static final String CAMPO_NOMBRE = "NAME|NOMBRE";
    private static final String CAMPO_DESCRIPCION = "DESC";
    // Opciones de un menú: "1.ASSIGN TASKS  2.VIEW TASKS  3.EXIT"
    private static final Pattern OPCION = Pattern.compile(
            "\\(?(\\d)\\s*[.)\\-]\\s*([A-Za-z]+(?: [A-Za-z]+)*)");
    private static final Pattern LINEA_MENU = Pattern.compile("^\\s*\\(?\\d\\s*[.)\\-]\\s*[A-Za-z].*");
    // Línea de una tarea: "TASK 3: SPECIFIC 0101 PEPE COMPRAR PAN"
    private static final Pattern LINEA_TAREA = Pattern.compile(
            "(?:TASK|TAREA)\\s*(\\d+)\\s*:?\\s*(GENERAL|SPECIFIC|ESPECIFICA)\\s+(\\d{3,4})\\s+(\\S+)\\s*(.*)",
            Pattern.CASE_INSENSITIVE);

    // Tiempos en milisegundos
    private static final long TIEMPO_MINIMO_CONEXION = 1500; // el enunciado exige al menos 1 s
    private static final long ESPERA_PANTALLA = 5000;
    private static final long ESPERA_LOGIN = 30000;
    private static final long INTERVALO_SONDEO = 150;
    private static final int MAX_PAGINAS = 100;

    private final ReentrantLock cerrojo = new ReentrantLock();
    private Process process;
    private BufferedReader reader;
    private PrintWriter out;
    private PrintWriter log;
    private String ultimaPantallaRegistrada;
    // Último menú mostrado por la aplicación: número de opción y su texto
    private Map<String, String> menuActual = new LinkedHashMap<>();

    private long instanteConexion = -1;
    private boolean sesionIniciada;
    private boolean appAbierta;
    private boolean desconectado;

    public void iniciarProceso() throws Exception {
        cerrojo.lock();
        try {
            try {
                log = new PrintWriter(new FileWriter("pantallas.log"), true);
            } catch (IOException e) {
                log = null; // el registro es solo una ayuda, no es imprescindible
            }

            // Inicia el emulador en segundo plano
            ProcessBuilder pb = new ProcessBuilder(comandoEmulador());
            pb.redirectErrorStream(true);
            process = pb.start();

            // Configura los canales de lectura y escritura
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
        } catch (Exception e) {
            registrar("! ERROR: " + e);
            throw e;
        } finally {
            cerrojo.unlock();
        }
    }

    public void conectarYLogin() throws Exception {
        cerrojo.lock();
        try {
            // Conecta con el servidor
            try {
                enviar("Connect(" + HOST + ")");
            } finally {
                instanteConexion = System.currentTimeMillis();
            }
            enviarTolerante("Wait(10,InputField)");

            // Avanza por las pantallas de bienvenida hasta el prompt del sistema
            boolean credencialesEnviadas = false;
            long instanteCredenciales = 0;
            long limite = System.currentTimeMillis() + ESPERA_LOGIN;
            String p = pantalla();
            while (!enPromptSO(p)) {
                if (System.currentTimeMillis() > limite) {
                    throw new IOException("No se ha podido iniciar sesión en el mainframe. Última pantalla:\n"
                            + recortar(p));
                }
                boolean pideLogin = p.toLowerCase().contains(MARCA_LOGIN);
                if (pideLogin && !credencialesEnviadas) {
                    // Introduce usuario y contraseña
                    enviar("String(\"" + escapar(USUARIO) + "\")");
                    enviar("Tab()");
                    enviar("String(\"" + escapar(CLAVE) + "\")");
                    enviar("Enter()");
                    credencialesEnviadas = true;
                    instanteCredenciales = System.currentTimeMillis();
                } else if (hayMas(p)) {
                    enviar("Enter()");
                } else if (pideLogin && System.currentTimeMillis() - instanteCredenciales > ESPERA_PANTALLA) {
                    throw new IOException("El mainframe ha rechazado el usuario o la clave. Última pantalla:\n"
                            + recortar(p));
                }
                Thread.sleep(INTERVALO_SONDEO);
                p = esperarEstable(p, 1000);
            }
            sesionIniciada = true;
        } catch (Exception e) {
            registrar("! ERROR: " + e);
            throw e;
        } finally {
            cerrojo.unlock();
        }
    }

    public void abrirAplicacion() throws Exception {
        cerrojo.lock();
        try {
            // Ejecuta el programa legado de tareas
            List<String> lineas = new ArrayList<>();
            String p = responder(PROGRAMA, lineas);
            for (String linea : lineas) {
                // El mainframe avisa si no puede ejecutar el fichero
                if (linea.toUpperCase().contains(PROGRAMA.toUpperCase()) && linea.contains("ERR")) {
                    throw new IOException("El mainframe no puede ejecutar " + PROGRAMA + ": " + linea.trim());
                }
            }
            long limite = System.currentTimeMillis() + ESPERA_LOGIN;
            while (!enMenuPrincipal()) {
                if (System.currentTimeMillis() > limite) {
                    throw new IOException("No se ha podido abrir " + PROGRAMA + ". Última pantalla:\n" + recortar(p));
                }
                if (hayMas(p)) {
                    enviar("Enter()");
                }
                p = esperarEstable(p, 1000);
            }
            appAbierta = true;
        } catch (Exception e) {
            registrar("! ERROR: " + e);
            throw e;
        } finally {
            cerrojo.unlock();
        }
    }

    public List<Tarea> listarTareas() throws Exception {
        cerrojo.lock();
        try {
            List<String> lineas = new ArrayList<>();
            irAlMenuPrincipal();
            responder(opcionObligatoria(MENU_VER), lineas);

            // Elige en el submenú qué tareas ver
            if (!enMenuPrincipal()) {
                String[] tipos = opcion(MENU_TODAS) != null
                        ? new String[] {MENU_TODAS}
                        : new String[] {MENU_GENERAL, MENU_ESPECIFICA};
                if (opcion(tipos[0]) == null) {
                    throw new IOException("No se reconoce el menú de consulta de tareas. Última pantalla:\n"
                            + recortar(pantalla()));
                }
                for (String tipo : tipos) {
                    if (enMenuPrincipal()) {
                        responder(opcionObligatoria(MENU_VER), lineas);
                    }
                    String o = opcion(tipo);
                    if (o != null) {
                        responder(o, lineas);
                    }
                }
            }
            irAlMenuPrincipal();
            return extraerTareas(lineas);
        } catch (Exception e) {
            registrar("! ERROR: " + e);
            throw e;
        } finally {
            cerrojo.unlock();
        }
    }

    public void nuevaTareaGeneral(String fecha, String descripcion) throws Exception {
        nuevaTarea(true, fecha, null, descripcion);
    }

    public void nuevaTareaEspecifica(String fecha, String nombre, String descripcion) throws Exception {
        nuevaTarea(false, fecha, nombre, descripcion);
    }

    private void nuevaTarea(boolean general, String fecha, String nombre, String descripcion) throws Exception {
        cerrojo.lock();
        try {
            irAlMenuPrincipal();
            String p = responder(opcionObligatoria(MENU_ASIGNAR));
            if (enMenuPrincipal()) {
                throw new IOException("No se reconoce el menú de alta de tareas. Última pantalla:\n" + recortar(p));
            }
            p = responder(opcionObligatoria(general ? MENU_GENERAL : MENU_ESPECIFICA));

            // Contesta a cada pregunta de la aplicación
            Map<String, String> pendientes = new LinkedHashMap<>();
            pendientes.put(CAMPO_FECHA, fecha);
            if (!general) {
                pendientes.put(CAMPO_NOMBRE, nombre);
            }
            pendientes.put(CAMPO_DESCRIPCION, descripcion);
            while (!pendientes.isEmpty()) {
                String campo = campoPedido(p, pendientes.keySet());
                p = responder(pendientes.remove(campo));
            }
            irAlMenuPrincipal();
        } catch (Exception e) {
            registrar("! ERROR: " + e);
            throw e;
        } finally {
            cerrojo.unlock();
        }
    }

    public void desconectarSeguro() {
        boolean enPosesion = false;
        try {
            // Espera como mucho 10 s a que termine la operación en curso
            enPosesion = cerrojo.tryLock(10, TimeUnit.SECONDS);
            if (process == null || desconectado) {
                return;
            }
            desconectado = true;

            if (enPosesion && process.isAlive() && instanteConexion > 0) {
                try {
                    salirOrdenadamente();
                } catch (Exception e) {
                    registrar("! Salida ordenada incompleta: " + e.getMessage());
                }
            }

            // Pausa obligatoria por seguridad del servidor
            esperarTiempoMinimo();

            // Cierra la conexión y el proceso
            if (enPosesion && process.isAlive()) {
                try {
                    enviar("Disconnect()");
                } catch (Exception e) {
                    registrar("! " + e.getMessage());
                }
                out.println("Quit()");
            }
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (log != null) {
                log.flush();
            }
            if (enPosesion) {
                cerrojo.unlock();
            }
        }
    }

    // Sale de la aplicación legada y cierra la sesión de MUSIC/SP
    private void salirOrdenadamente() throws Exception {
        if (appAbierta) {
            irAlMenuPrincipal();
            responder(opcionObligatoria(MENU_SALIR));
            appAbierta = false;
        }
        if (sesionIniciada && enPromptSO(pantalla())) {
            responder(COMANDO_SALIR_SO);
            sesionIniciada = false;
        }
    }

    private void esperarTiempoMinimo() throws InterruptedException {
        if (instanteConexion < 0) {
            return;
        }
        long restante = TIEMPO_MINIMO_CONEXION - (System.currentTimeMillis() - instanteConexion);
        if (restante > 0) {
            Thread.sleep(restante);
        }
    }

    // Navegación por la aplicación legada

    // El menú principal es el que tiene las opciones de asignar y ver
    private boolean enMenuPrincipal() {
        return opcion(MENU_ASIGNAR) != null && opcion(MENU_VER) != null;
    }

    private void irAlMenuPrincipal() throws Exception {
        String p = pantalla();
        for (int i = 0; i < 4 && !enPromptSO(p); i++) {
            if (enMenuPrincipal()) {
                return;
            }
            String volver = opcion(MENU_VOLVER);
            if (volver == null) {
                break;
            }
            p = responder(volver);
        }
        throw new IOException("No se ha podido volver al menú principal. Última pantalla:\n" + recortar(p));
    }

    // Número de la opción del menú actual que contiene alguna de las palabras
    private String opcion(String palabras) {
        Pattern patron = Pattern.compile(palabras);
        for (Map.Entry<String, String> o : menuActual.entrySet()) {
            if (patron.matcher(o.getValue().toUpperCase()).find()) {
                return o.getKey();
            }
        }
        return null;
    }

    private String opcionObligatoria(String palabras) throws IOException {
        String o = opcion(palabras);
        if (o == null) {
            throw new IOException("No se encuentra la opción " + palabras + " en el menú " + menuActual.values()
                    + ". Última pantalla:\n" + recortar(pantalla()));
        }
        return o;
    }

    // Devuelve el último menú que aparece en pantalla
    private static Map<String, String> ultimoMenu(String pantalla) {
        Map<String, String> ultimo = new LinkedHashMap<>();
        Map<String, String> bloque = new LinkedHashMap<>();
        for (String linea : contenido(pantalla)) {
            if (LINEA_MENU.matcher(linea).matches()) {
                Matcher m = OPCION.matcher(linea);
                while (m.find()) {
                    bloque.put(m.group(1), m.group(2));
                }
                ultimo = bloque;
            } else {
                bloque = new LinkedHashMap<>();
            }
        }
        return ultimo;
    }

    // Campo que está pidiendo la aplicación
    private static String campoPedido(String pantalla, Collection<String> pendientes) {
        List<String> lineas = contenido(pantalla);
        for (int i = lineas.size() - 1; i >= Math.max(0, lineas.size() - 2); i--) {
            for (String campo : pendientes) {
                if (Pattern.compile(campo).matcher(lineas.get(i).toUpperCase()).find()) {
                    return campo;
                }
            }
        }
        return pendientes.iterator().next();
    }

    private static List<Tarea> extraerTareas(List<String> lineas) {
        // El mapa evita tareas repetidas
        Map<String, Tarea> tareas = new LinkedHashMap<>();
        for (String linea : lineas) {
            Matcher m = LINEA_TAREA.matcher(linea);
            if (!m.find()) {
                continue;
            }
            int id = Integer.parseInt(m.group(1));
            boolean general = m.group(2).equalsIgnoreCase("GENERAL");
            String nombre = m.group(4);
            String descripcion = m.group(5).trim();
            if (general) {
                // Las generales no tienen nombre, vienen con guiones
                if (!nombre.matches("-+")) {
                    descripcion = (nombre + " " + descripcion).trim();
                }
                nombre = "";
            }
            tareas.put(m.group(2).toUpperCase() + id, new Tarea(id, general, m.group(3), nombre, descripcion));
        }
        List<Tarea> ordenadas = new ArrayList<>(tareas.values());
        ordenadas.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        return ordenadas;
    }

    // Sincronización con el terminal

    private String responder(String texto) throws Exception {
        return responder(texto, null);
    }

    // Escribe el texto, pulsa ENTER y devuelve la pantalla resultante
    private String responder(String texto, List<String> acumulador) throws Exception {
        String antes = pantalla();
        enviarTolerante("Wait(5,Unlock)");
        if (!texto.isEmpty()) {
            enviar("String(\"" + escapar(texto) + "\")");
        }
        enviar("Enter()");

        String anterior = antes;
        for (int i = 0; i < MAX_PAGINAS; i++) {
            String p = esperarEstable(anterior, ESPERA_PANTALLA);
            if (acumulador != null) {
                acumulador.addAll(Arrays.asList(p.split("\n")));
            }
            if (!hayMas(p)) {
                return p;
            }
            enviar("Enter()");
            anterior = p;
        }
        throw new IOException("El mainframe no deja de pedir que se avance de página.");
    }

    // Espera a que la pantalla cambie y deje de moverse
    private String esperarEstable(String anterior, long timeoutMs) throws Exception {
        long limite = System.currentTimeMillis() + timeoutMs;
        String previa = null;
        while (true) {
            String actual = pantalla();
            boolean haCambiado = anterior == null || !actual.equals(anterior);
            if (haCambiado && !pendienteDeProcesar(actual) && actual.equals(previa)) {
                return actual;
            }
            if (System.currentTimeMillis() > limite) {
                return actual;
            }
            previa = actual;
            Thread.sleep(INTERVALO_SONDEO);
        }
    }

    // Indica si el mainframe todavía no ha contestado a lo tecleado
    private static boolean pendienteDeProcesar(String pantalla) {
        if (ultimasLineas(pantalla, 1).contains(MARCA_OCUPADO)) {
            return true;
        }
        String[] lineas = pantalla.split("\n");
        int conTexto = 0;
        for (int i = lineas.length - 1; i >= 0 && !lineas[i].trim().startsWith(REGLA); i--) {
            if (!lineas[i].trim().isEmpty()) {
                conTexto++;
            }
            if (i == 0) {
                return false; // pantalla sin línea de entrada
            }
        }
        return conTexto > 1; // algo más que la línea de estado
    }

    // Indica si MUSIC/SP está esperando un comando
    private static boolean enPromptSO(String pantalla) {
        List<String> lineas = contenido(pantalla);
        String ultimas = String.join("\n", lineas.subList(Math.max(0, lineas.size() - 2), lineas.size()));
        return pantalla.contains(PROMPT_FSI) || ultimas.contains(PROMPT_SO);
    }

    // Líneas escritas por el programa, sin la línea de estado ni los "?"
    private static List<String> contenido(String pantalla) {
        String[] lineas = pantalla.split("\n");
        int fin = lineas.length;
        for (int i = lineas.length - 1; i >= 0; i--) {
            if (lineas[i].trim().startsWith(REGLA)) {
                fin = i;
                break;
            }
        }
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < fin; i++) {
            String l = lineas[i].trim();
            if (!l.isEmpty() && !l.equals("?")) {
                resultado.add(lineas[i]);
            }
        }
        return resultado;
    }

    private static boolean hayMas(String pantalla) {
        return ultimasLineas(pantalla, 1).contains(MARCA_MAS);
    }

    // Las n últimas líneas no vacías de la pantalla
    private static String ultimasLineas(String pantalla, int n) {
        List<String> resultado = new ArrayList<>();
        String[] lineas = pantalla.split("\n");
        for (int i = lineas.length - 1; i >= 0 && resultado.size() < n; i--) {
            if (!lineas[i].trim().isEmpty()) {
                resultado.add(0, lineas[i]);
            }
        }
        return String.join("\n", resultado);
    }

    // Diálogo con ws3270

    private String pantalla() throws IOException {
        // Pide el contenido actual de la pantalla
        String p = String.join("\n", enviar("Ascii()"));
        if (!p.equals(ultimaPantallaRegistrada)) {
            ultimaPantallaRegistrada = p;
            registrar(recortar(p));
        }
        Map<String, String> menu = ultimoMenu(p);
        if (!menu.isEmpty()) {
            menuActual = menu;
        }
        return p;
    }

    // Envía un comando a ws3270 y lee su respuesta hasta "ok" o "error"
    private List<String> enviar(String comando) throws IOException {
        boolean silencioso = comando.equals("Ascii()");
        if (!silencioso) {
            registrar("> " + (comando.contains(CLAVE) ? "String(\"********\")" : comando));
        }
        out.println(comando);
        if (out.checkError()) {
            throw new IOException("Se ha perdido la comunicación con el emulador ws3270.");
        }

        List<String> datos = new ArrayList<>();
        String linea;
        while ((linea = reader.readLine()) != null) {
            if (linea.equals("ok")) {
                return datos;
            }
            if (linea.equals("error")) {
                throw new IOException("ws3270 ha rechazado " + (comando.contains(CLAVE) ? "la clave" : comando)
                        + ": " + String.join(" ", datos).trim());
            }
            if (linea.startsWith("data:")) {
                datos.add(linea.length() > 6 ? linea.substring(6) : "");
            }
        }
        throw new IOException("El emulador ws3270 ha terminado inesperadamente.");
    }

    // Envía un comando ignorando si falla
    private void enviarTolerante(String comando) {
        try {
            enviar(comando);
        } catch (IOException e) {
            registrar("! " + e.getMessage());
        }
    }

    private static List<String> comandoEmulador() {
        // Permite indicar otro emulador con -Dws3270
        String propiedad = System.getProperty("ws3270");
        if (propiedad != null && !propiedad.trim().isEmpty()) {
            return Arrays.asList(propiedad.trim().split("\\s+"));
        }
        // Busca ws3270.exe junto a la aplicación, en la instalación de wc3270 y en el PATH
        List<File> candidatos = new ArrayList<>();
        candidatos.add(new File("ws3270.exe"));
        for (String variable : new String[] {"ProgramFiles", "ProgramFiles(x86)"}) {
            String carpeta = System.getenv(variable);
            if (carpeta != null) {
                candidatos.add(new File(carpeta, "wc3270\\ws3270.exe"));
            }
        }
        for (File candidato : candidatos) {
            if (candidato.isFile()) {
                return Arrays.asList(candidato.getAbsolutePath());
            }
        }
        return Arrays.asList("ws3270.exe");
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Quita los espacios y las líneas en blanco del final
    private static String recortar(String pantalla) {
        StringBuilder sb = new StringBuilder();
        for (String linea : pantalla.split("\n")) {
            sb.append(linea.replaceAll("\\s+$", "")).append('\n');
        }
        return sb.toString().replaceAll("\\n+$", "");
    }

    private void registrar(String texto) {
        if (log != null) {
            log.println(texto);
            log.println();
        }
    }
}
