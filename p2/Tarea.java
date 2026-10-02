public class Tarea {

    private final int id;
    private final boolean general;
    private final String fecha;
    private final String nombre;
    private final String descripcion;

    public Tarea(int id, boolean general, String fecha, String nombre, String descripcion) {
        this.id = id;
        this.general = general;
        this.fecha = fecha;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getId() {
        return id;
    }

    public boolean esGeneral() {
        return general;
    }

    // Fecha tal y como la guarda el mainframe (DDMM)
    public String getFecha() {
        return fecha;
    }

    // Vacío en las tareas generales
    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
