package sl.p3;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

/**
 * Envía teclas a la aplicación legada, una a una y con pausa: si se escriben de golpe, GW-BASIC toma algunas por
 * teclas de función (salen TRON, KEY...). Las mayúsculas llevan Shift, y S/N siempre en mayúscula.
 *
 * Letras, números y espacio van con Robot. Los signos ({@link #SIGNOS}) cambian de tecla según la distribución del
 * teclado: se pregunta a Windows cuál es (VkKeyScan) y se pulsa con su código de exploración, que es lo que lee
 * DOSBox (con keyboardlayout=auto usa la misma distribución que Windows).
 */
public final class KeyboardDriver {
    /** Signos de los nombres de DATOS.DAT ("KNIGHT'S LORE", "B.C. BILL"...). */
    static final String SIGNOS = "-'.!&";

    /** Funciones de user32 para pulsar los signos. */
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

    /** Pulsa un carácter (opción del menú, respuesta...). */
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

    /** Escribe el texto tal cual, carácter a carácter. */
    public void type(String text) {
        for (char c : text.toCharArray()) typeChar(c);
    }

    public void enter() {
        tap(KeyEvent.VK_ENTER);
    }

    public void space() {
        tap(KeyEvent.VK_SPACE);
    }

    /** Respuesta S/N: letra mayúscula y Enter. */
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

    // ------------------------------------------------- signos según la distribución

    /** Tecla (byte bajo) y Shift (bit 0x100) del signo, o -1 si no existe o necesita AltGr. */
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

    /** Pulsa o suelta una tecla con su código de exploración (el que lee DOSBox). */
    private static void winKey(int vk, boolean up) {
        int scan = Win32Keys.INSTANCE.MapVirtualKeyW(vk, MAPVK_VK_TO_VSC);
        Win32Keys.INSTANCE.keybd_event((byte) vk, (byte) scan, up ? KEYEVENTF_KEYUP : 0, 0);
    }
}
