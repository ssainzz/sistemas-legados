package sl.p3;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

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
 * Se soportan A-Z, a-z, 0-9, espacio y los signos que aparecen en los nombres de DATOS.DAT
 * ({@link #SIGNOS}). Las letras, numeros y espacio ocupan la misma tecla en cualquier distribucion
 * y van con Robot. Los signos no: el apostrofo, por ejemplo, esta junto al 0 en el teclado espanol
 * y junto al Enter en el ingles. Para ellos se pregunta a Windows que tecla (y si lleva Shift)
 * produce el caracter en la distribucion actual (VkKeyScan) y se pulsa esa tecla con su codigo de
 * exploracion (MapVirtualKey + keybd_event). DOSBox usa la misma distribucion que Windows
 * (keyboardlayout=auto), asi que recibe el mismo caracter.
 */
public final class KeyboardDriver {
    /** Signos que aparecen en nombres reales de DATOS.DAT ("KNIGHT'S LORE", "B.C. BILL", ...). */
    static final String SIGNOS = "-'.!&";

    /** Funciones de user32 para pulsar los signos segun la distribucion de teclado de Windows. */
    private interface Win32Keys extends StdCallLibrary {
        Win32Keys INSTANCE = Native.load("user32", Win32Keys.class);

        short VkKeyScanW(char ch);

        int MapVirtualKeyW(int uCode, int uMapType);

        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
    }

    private static final int WIN_VK_SHIFT = 0x10, MAPVK_VK_TO_VSC = 0, KEYEVENTF_KEYUP = 0x2;

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
        if (SIGNOS.indexOf(c) >= 0) {
            typeSigno(c);
            return;
        }
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
        if (SIGNOS.indexOf(c) >= 0) return signoKey(c) >= 0;
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
        throw new IllegalArgumentException("Caracter no soportado por el teclado: '" + c + "'");
    }

    // ------------------------------------------------- signos segun la distribucion

    /**
     * Tecla de Windows (byte bajo) y Shift (bit 0x100) que producen el signo en la distribucion
     * actual, o -1 si no existe o necesita Ctrl/Alt (AltGr), que no se envian.
     */
    private static int signoKey(char c) {
        int r = Win32Keys.INSTANCE.VkKeyScanW(c);
        if (r == -1 || (r & 0x600) != 0) return -1;
        return r & 0x1FF;
    }

    private void typeSigno(char c) {
        int key = signoKey(c);
        if (key < 0) throw new IllegalArgumentException("Caracter no soportado por el teclado: '" + c + "'");
        int vk = key & 0xFF;
        boolean shift = (key & 0x100) != 0;
        if (shift) winKey(WIN_VK_SHIFT, false);
        winKey(vk, false);
        winKey(vk, true);
        if (shift) winKey(WIN_VK_SHIFT, true);
        pause();
    }

    /** Pulsa o suelta una tecla de Windows con su codigo de exploracion (DOSBox lee este ultimo). */
    private static void winKey(int vk, boolean up) {
        int scan = Win32Keys.INSTANCE.MapVirtualKeyW(vk, MAPVK_VK_TO_VSC);
        Win32Keys.INSTANCE.keybd_event((byte) vk, (byte) scan, up ? KEYEVENTF_KEYUP : 0, 0);
    }
}
