import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Instanciar la clase que maneja la conexión subyacente
        ConexionMainframe conexion = new ConexionMainframe();

        // Desconexión segura al terminar el programa
        Runtime.getRuntime().addShutdownHook(new Thread(conexion::desconectarSeguro));

        // Arrancar la interfaz gráfica y conectar en segundo plano
        SwingUtilities.invokeLater(() -> {
            VentanaTareas ventana = new VentanaTareas(conexion);
            ventana.setVisible(true);
            ventana.conectar();
        });
    }
}
