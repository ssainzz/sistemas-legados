import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class VentanaTareas extends JFrame {

    private ConexionMainframe conexion;
    private JTable tablaTareas;
    private DefaultTableModel modeloTabla;
    private JRadioButton rbTodas;
    private JRadioButton rbGenerales;
    private JRadioButton rbEspecificas;
    private JButton btnNueva;
    private JButton btnSalir;

    public VentanaTareas(ConexionMainframe conexion) {
        this.conexion = conexion;
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Gestor de Tareas - Grupo 07");
        setSize(650, 420);
        // Evitamos que la ventana se cierre de golpe para controlar la desconexión
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
        setLayout(new BorderLayout(15, 15));

        // --- Panel izquierdo / central (título + filtros + tabla) ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout(10, 10));
        panelIzquierdo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));

        // 1. Título "Tareas" y panel de filtros superiores (3 bloques)
        JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));
        
        JLabel lblTitulo = new JLabel("Tareas");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        panelSuperior.add(lblTitulo, BorderLayout.NORTH);

        // Panel horizontal dividido en 3 celdas para los filtros ("Todas", "Generales", "Específicas")
        JPanel panelFiltros = new JPanel(new GridLayout(1, 3, 5, 5));
        panelFiltros.setBorder(BorderFactory.createTitledBorder("Filtro de Tareas"));

        rbTodas = new JRadioButton("Todas", true);
        rbGenerales = new JRadioButton("Generales");
        rbEspecificas = new JRadioButton("Específicas");

        ButtonGroup grupoFiltros = new ButtonGroup();
        grupoFiltros.add(rbTodas);
        grupoFiltros.add(rbGenerales);
        grupoFiltros.add(rbEspecificas);

        panelFiltros.add(rbTodas);
        panelFiltros.add(rbGenerales);
        panelFiltros.add(rbEspecificas);

        panelSuperior.add(panelFiltros, BorderLayout.CENTER);
        panelIzquierdo.add(panelSuperior, BorderLayout.NORTH);

        // 2. Tabla para la lista de tareas (ID, Descripción, Fecha)
        String[] columnas = {"ID", "Descripción", "Fecha"};
        String[][] datosIniciales = {
            {"XXXXX", "XXXXX", "XXXXX"}
        }; 
        
        modeloTabla = new DefaultTableModel(datosIniciales, columnas) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Configurar la tabla como no editable por el usuario
            }
        };
        
        tablaTareas = new JTable(modeloTabla);
        panelIzquierdo.add(new JScrollPane(tablaTareas), BorderLayout.CENTER);

        add(panelIzquierdo, BorderLayout.CENTER);

        // --- Panel derecho (botones Nueva / Salir) ---
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBorder(BorderFactory.createEmptyBorder(45, 10, 10, 15));

        JPanel panelBotones = new JPanel(new GridLayout(2, 1, 10, 15));

        btnNueva = new JButton("Nueva");
        btnSalir = new JButton("Salir");

        // Conectamos el botón Salir al cierre seguro
        btnSalir.addActionListener(e -> cerrarAplicacion());

        panelBotones.add(btnNueva);
        panelBotones.add(btnSalir);

        panelDerecho.add(panelBotones, BorderLayout.NORTH);
        add(panelDerecho, BorderLayout.EAST);
    }

    private void cerrarAplicacion() {
        // Garantizamos el mínimo de 1 segundo de espera antes de salir
        if (conexion != null) {
            conexion.desconectarSeguro(); 
        }
        System.exit(0);
    }
}