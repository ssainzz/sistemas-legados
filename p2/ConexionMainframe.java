import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ConexionMainframe {

    private Process process;
    private BufferedReader reader;
    private PrintWriter err;
    private PrintWriter out;

    public void iniciarProceso() throws Exception {
        // Inicia el emulador en segundo plano
        process = Runtime.getRuntime().exec("ws3270.exe");
        
        // Configura los canales de lectura y escritura
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        err = new PrintWriter(new OutputStreamWriter(process.getErrorStream()), true);
        out = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
    }

    public void conectarYLogin() throws Exception {
        // Conecta con el servidor
        out.println("Connect(155.210.152.51:3270)");
        Thread.sleep(2000);

        // Introduce usuario y contraseña
        out.println("String(\"grupo_07\")");
        out.println("Tab()");
        out.println("String(\"secreto6\")");
        out.println("Enter()");
        Thread.sleep(2000);
    }

    public void abrirAplicacion() throws Exception {
        // Ejecuta el programa legadode tareas
        out.println("String(\"tareas.c\")");
        out.println("Enter()");
        Thread.sleep(2000);
    }

    public List<String> extraerTareas() throws Exception {
        List<String> tareasExtraidas = new ArrayList<>();
        
        // Pide el contenido actual de la pantalla
        out.println("Ascii()");
        
        String linea;
        // Filtra y guarda los datos leídos
        while ((linea = reader.readLine()) != null) {
            if (linea.startsWith("data:")) {
                tareasExtraidas.add(linea.replace("data: ", "").trim());
            }
            if (linea.equals("ok") || linea.equals("error")) {
                break;
            }
        }
        return tareasExtraidas;
    }

    public void desconectarSeguro() {
        try {
            // Pausa obligatoria por seguridad del servidor
            Thread.sleep(1000);
            
            // Cierra la conexión y el proceso
            if (out != null) {
                out.println("Quit()");
            }
            if (process != null) {
                process.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}