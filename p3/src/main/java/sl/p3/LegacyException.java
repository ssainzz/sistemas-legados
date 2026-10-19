package sl.p3;

/** Error al usar la aplicación legada (pantalla inesperada, ventana no encontrada...). */
public class LegacyException extends RuntimeException {
    public LegacyException(String message) {
        super(message);
    }

    public LegacyException(String message, Throwable cause) {
        super(message, cause);
    }
}
