package sl.p3;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

/**
 * Envia teclas a la aplicacion legada con java.awt.Robot, SIEMPRE tecla a tecla y con una pausa.
 *
 * Importante (comprobado a mano): si se escribe una cadena de golpe, GW-BASIC/DOSBox interpreta
 * parte de las teclas como teclas de funcion (aparecen macros como TRON, TROFF o KEY en la linea).
 * Por eso cada caracter es un keyPress + keyRelease con un delay, y las mayusculas llevan Shift
 * explicito. La aplicacion solo acepta S/N en MAYUSCULA.
 *
 * Solo se soportan A-Z, 0-9, espacio y '-' . Otros caracteres (por ejemplo el apostrofo de
 * "EVERYONE'S A WALLY") dependen de la distribucion de teclado y quedan como TODO.
 */
public final class KeyboardDriver {
    private final Robot robot;
    private final int delayMs;

    public KeyboardDriver(int delayMs) {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new IllegalStateException("No se puede crear java.awt.Robot", e);
        }
        this.delayMs = delayMs;
    }

    /** Pulsa un caracter (tecla del menu, letra de respuesta...). */
    public void typeChar(char c) {
        boolean shift = Character.isUpperCase(c);
        int vk = keyCode(c);
        if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(vk);
        robot.keyRelease(vk);
        if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
        pause();
    }

    /** Escribe un texto caracter a caracter (el texto se pasa tal cual: mayusculas = Shift). */
    public void type(String text) {
        for (char c : text.toCharArray()) typeChar(c);
    }

    public void enter() {
        tap(KeyEvent.VK_ENTER);
    }

    public void space() {
        tap(KeyEvent.VK_SPACE);
    }

    /** Respuesta S/N de la aplicacion: letra mayuscula + Enter. */
    public void answer(char upperLetter) {
        typeChar(upperLetter);
        enter();
    }

    public static boolean isSupported(char c) {
        try {
            keyCode(c);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void tap(int vk) {
        robot.keyPress(vk);
        robot.keyRelease(vk);
        pause();
    }

    private void pause() {
        robot.delay(delayMs);
    }

    private static int keyCode(char c) {
        if (c >= 'A' && c <= 'Z') return KeyEvent.VK_A + (c - 'A');
        if (c >= 'a' && c <= 'z') return KeyEvent.VK_A + (c - 'a');
        if (c >= '0' && c <= '9') return KeyEvent.VK_0 + (c - '0');
        if (c == ' ') return KeyEvent.VK_SPACE;
        if (c == '-') return KeyEvent.VK_MINUS;
        throw new IllegalArgumentException("Caracter no soportado por el teclado: '" + c + "'");
    }
}
