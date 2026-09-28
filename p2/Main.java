import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        try {
            // Instanciar la clase que maneja la conexión subyacente
            ConexionMainframe conexion = new ConexionMainframe();
            
            // Ejecutar la secuencia inicial de arranque de forma síncrona
            conexion.iniciarProceso();
            conexion.conectarYLogin();
            conexion.abrirAplicacion();
            
            // Arrancar la interfaz gráfica en el hilo de eventos de Swing (EDT)
            SwingUtilities.invokeLater(() -> {
                VentanaTareas ventana = new VentanaTareas(conexion);
                ventana.setVisible(true);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            // Mostrar un mensaje visual si hay un fallo crítico (ej. ws3270 no encontrado o servidor caído)
            JOptionPane.showMessageDialog(null, 
                "No se pudo establecer la conexión inicial con el mainframe.\nCompruebe su conexión a internet y asegúrese de tener ws3270.exe instalado.\n\nDetalle: " + e.getMessage(), 
                "Error Crítico de Conexión", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}