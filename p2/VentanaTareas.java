import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class VentanaTareas extends JFrame {

    // Longitudes máximas de los campos
    private static final int MAX_NOMBRE = 20;
    private static final int MAX_DESCRIPCION = 60;
    private static final int[] DIAS_MES = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private ConexionMainframe conexion;
    private List<Tarea> tareas = new ArrayList<>();
    private boolean cerrando;

    private JTable tablaTareas;
    private DefaultTableModel modeloTabla;
    private JRadioButton rbTodas;
    private JRadioButton rbGenerales;
    private JRadioButton rbEspecificas;
    private JButton btnNueva;
    private JButton btnActualizar;
    private JButton btnSalir;
    private JLabel lblEstado;

    public VentanaTareas(ConexionMainframe conexion) {
        this.conexion = conexion;
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Gestor de Tareas - Grupo 07");
        setSize(700, 420);
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

        // Panel izquierdo / central (título + filtros + tabla)
        JPanel panelIzquierdo = new JPanel(new BorderLayout(10, 10));
        panelIzquierdo.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));

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

        // El filtro se aplica sobre las tareas ya leídas
        rbTodas.addActionListener(e -> refrescarTabla());
        rbGenerales.addActionListener(e -> refrescarTabla());
        rbEspecificas.addActionListener(e -> refrescarTabla());

        panelFiltros.add(rbTodas);
        panelFiltros.add(rbGenerales);
        panelFiltros.add(rbEspecificas);

        panelSuperior.add(panelFiltros, BorderLayout.CENTER);
        panelIzquierdo.add(panelSuperior, BorderLayout.NORTH);

        // 2. Tabla para la lista de tareas
        String[] columnas = {"ID", "Tipo", "Descripción", "Fecha", "Nombre"};

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Configurar la tabla como no editable por el usuario
            }
        };

        tablaTareas = new JTable(modeloTabla);
        tablaTareas.getColumnModel().getColumn(0).setMaxWidth(50);
        tablaTareas.getColumnModel().getColumn(2).setPreferredWidth(250);
        panelIzquierdo.add(new JScrollPane(tablaTareas), BorderLayout.CENTER);

        add(panelIzquierdo, BorderLayout.CENTER);

        // Panel derecho (botones Nueva / Actualizar / Salir)
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBorder(BorderFactory.createEmptyBorder(45, 10, 10, 15));

        JPanel panelBotones = new JPanel(new GridLayout(3, 1, 10, 15));

        btnNueva = new JButton("Nueva");
        btnActualizar = new JButton("Actualizar");
        btnSalir = new JButton("Salir");

        // Eventos de los botones
        btnNueva.addActionListener(e -> mostrarMenuNuevaTarea());
        btnActualizar.addActionListener(e -> cargarTareas());
        btnSalir.addActionListener(e -> cerrarAplicacion());

        panelBotones.add(btnNueva);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnSalir);

        panelDerecho.add(panelBotones, BorderLayout.NORTH);
        add(panelDerecho, BorderLayout.EAST);

        // Barra de estado
        lblEstado = new JLabel(" ");
        lblEstado.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        add(lblEstado, BorderLayout.SOUTH);
    }

    // Conecta con el mainframe y carga las tareas
    public void conectar() {
        ejecutarEnMainframe("Conectando con el mainframe...", true, () -> {
            conexion.iniciarProceso();
            conexion.conectarYLogin();
            conexion.abrirAplicacion();
            return conexion.listarTareas();
        });
    }

    private void cargarTareas() {
        ejecutarEnMainframe("Leyendo las tareas del mainframe...", false, () -> conexion.listarTareas());
    }

    // Ejecuta la operación en segundo plano y muestra las tareas que devuelve
    private void ejecutarEnMainframe(String mensaje, boolean errorFatal, Callable<List<Tarea>> operacion) {
        setOcupado(true, mensaje);
        new SwingWorker<List<Tarea>, Void>() {
            @Override
            protected List<Tarea> doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {
                if (cerrando) {
                    return;
                }
                try {
                    tareas = get();
                    refrescarTabla();
                    setOcupado(false, tareas.size() + " tareas en el mainframe");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    causa.printStackTrace();
                    if (errorFatal) {
                        mostrarError("Error Crítico de Conexión",
                                "No se pudo establecer la conexión inicial con el mainframe.\n"
                                + "Compruebe su conexión a internet y que ws3270.exe está junto a la aplicación.\n\n"
                                + "Detalle: " + causa.getMessage());
                        cerrarAplicacion();
                    } else {
                        mostrarError("Error", "La operación no se ha podido completar.\n\nDetalle: "
                                + causa.getMessage());
                        setOcupado(false, "Error en la última operación");
                    }
                }
            }
        }.execute();
    }

    private void refrescarTabla() {
        modeloTabla.setRowCount(0);
        for (Tarea t : tareas) {
            if ((rbGenerales.isSelected() && !t.esGeneral()) || (rbEspecificas.isSelected() && t.esGeneral())) {
                continue;
            }
            modeloTabla.addRow(new Object[]{
                t.getId(),
                t.esGeneral() ? "General" : "Específica",
                t.getDescripcion(),
                formatearFecha(t.getFecha()),
                t.esGeneral() ? "--" : t.getNombre()
            });
        }
    }

    private static String formatearFecha(String ddmm) {
        return ddmm.length() == 4 ? ddmm.substring(0, 2) + "/" + ddmm.substring(2) : ddmm;
    }

    private void setOcupado(boolean ocupado, String mensaje) {
        btnNueva.setEnabled(!ocupado);
        btnActualizar.setEnabled(!ocupado);
        setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        lblEstado.setText(mensaje);
    }

    private void mostrarError(String titulo, String mensaje) {
        JTextArea texto = new JTextArea(mensaje, 12, 60);
        texto.setEditable(false);
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(texto), titulo, JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarMenuNuevaTarea() {
        String[] opciones = {"General", "Específica", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(this,
                "Seleccione el tipo de tarea que desea crear:",
                "Nueva Tarea",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (seleccion == 0) {
            crearTarea(true);
        } else if (seleccion == 1) {
            crearTarea(false);
        }
    }

    private void crearTarea(boolean general) {
        JTextField txtDia = new JTextField(2);
        JTextField txtMes = new JTextField(2);
        JTextField txtNombre = new JTextField(10);
        JTextField txtDescripcion = new JTextField(15);

        JPanel panel = new JPanel(new GridLayout(general ? 3 : 4, 2, 5, 5));
        panel.add(new JLabel("Día (DD):"));
        panel.add(txtDia);
        panel.add(new JLabel("Mes (MM):"));
        panel.add(txtMes);
        if (!general) {
            panel.add(new JLabel("Nombre:"));
            panel.add(txtNombre);
        }
        panel.add(new JLabel("Descripción:"));
        panel.add(txtDescripcion);

        // Repite el diálogo hasta que los datos sean válidos
        while (true) {
            int resultado = JOptionPane.showConfirmDialog(this, panel,
                    general ? "Nueva Tarea General" : "Nueva Tarea Específica",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resultado != JOptionPane.OK_OPTION) {
                return;
            }

            String nombre = txtNombre.getText().trim();
            String desc = txtDescripcion.getText().trim();
            String fecha = validarFecha(txtDia.getText().trim(), txtMes.getText().trim());
            String error = null;
            if (fecha == null) {
                error = "La fecha no es válida.";
            } else if (!general && !nombre.matches("[\\p{Alnum}_-]{1," + MAX_NOMBRE + "}")) {
                error = "El nombre debe ser una sola palabra de " + MAX_NOMBRE
                        + " caracteres como máximo (letras sin tilde, números, '-' o '_').";
            } else if (!desc.matches("[\\p{Alnum}_-]{1," + MAX_DESCRIPCION + "}")) {
                error = "La descripción debe ser una sola palabra de " + MAX_DESCRIPCION
                        + " caracteres como máximo (letras sin tilde, números, '-' o '_').";
            }
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Datos no válidos", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Registra la tarea y vuelve a leer la lista
            ejecutarEnMainframe("Registrando la tarea en el mainframe...", false, () -> {
                if (general) {
                    conexion.nuevaTareaGeneral(fecha, desc);
                } else {
                    conexion.nuevaTareaEspecifica(fecha, nombre, desc);
                }
                return conexion.listarTareas();
            });
            return;
        }
    }

    // Devuelve la fecha en formato DDMM, o null si no es válida
    private static String validarFecha(String dia, String mes) {
        if (!dia.matches("\\d{1,2}") || !mes.matches("\\d{1,2}")) {
            return null;
        }
        int d = Integer.parseInt(dia);
        int m = Integer.parseInt(mes);
        if (m < 1 || m > 12 || d < 1 || d > DIAS_MES[m - 1]) {
            return null;
        }
        return String.format("%02d%02d", d, m);
    }

    private void cerrarAplicacion() {
        if (cerrando) {
            return;
        }
        cerrando = true;
        setOcupado(true, "Desconectando del mainframe...");
        btnSalir.setEnabled(false);

        // Desconecta en otro hilo para no congelar la ventana
        new Thread(() -> {
            conexion.desconectarSeguro();
            System.exit(0);
        }).start();
    }
}
