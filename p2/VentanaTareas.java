import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

public class VentanaTareas extends JFrame {

    private ConexionMainframe conexion;
    private JTable tablaTareas;

    public VentanaTareas(ConexionMainframe conexion) {
        this.conexion = conexion;
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Gestor de Tareas - Grupo 07");
        setSize(600, 400);
        // Evitamos que la ventana se cierre de golpe para controlar la desconexión[cite: 1]
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        setLocationRelativeTo(null);

        // Controlamos el cierre desde la 'X' de la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarAplicacion();
            }
        });
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));

        // 1. Tabla para la lista de tareas (según el boceto)[cite: 1]
        String[] columnas = {"ID", "Descripción", "Estado"}; // Columnas genéricas iniciales
        String[][] datos = { {"XXXXX", "XXXXX", "XXXXX"} }; 
        tablaTareas = new JTable(datos, columnas);
        add(new JScrollPane(tablaTareas), BorderLayout.CENTER);

        // 2. Panel lateral derecho para los botones[cite: 1]
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayout(2, 1, 10, 10));

        JButton btnNueva = new JButton("Nueva");
        JButton btnSalir = new JButton("Salir");

        // Conectamos el botón Salir al cierre seguro
        btnSalir.addActionListener(e -> cerrarAplicacion());

        panelBotones.add(btnNueva);
        panelBotones.add(btnSalir);

        JPanel panelDerecho = new JPanel(new FlowLayout());
        panelDerecho.add(panelBotones);
        add(panelDerecho, BorderLayout.EAST);
    }

    private void cerrarAplicacion() {
        // Garantizamos el mínimo de 1 segundo de espera antes de salir[cite: 1]
        if (conexion != null) {
            conexion.desconectarSeguro(); 
        }
        System.exit(0);
    }
}