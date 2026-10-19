package sl.p3;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Localiza la ventana de DOSBox (Windows, via JNA), la trae al frente y devuelve su area cliente.
 *
 * Robot escribe en la ventana que tenga el foco y captura pixeles de pantalla, asi que la ventana
 * debe estar visible, sin minimizar y sin otras ventanas encima.
 *
 * Nota: al lanzar database.bat aparecen varias ventanas de DOSBox ("DOSBox Status Window" y la del
 * programa, cuyo titulo es "DOSBox 0.74, Cpu speed: ..."). Aqui se descarta la de estado. Si hay
 * mas de una instancia del programa abierta se usa la primera que devuelva Windows.
 *
 * Las coordenadas de Windows (pixeles fisicos) y las de Robot
 * coinciden porque Main fija sun.java2d.uiScale=1.
 */
public final class DosboxWindow {
    /** Funciones de user32 que jna-platform no trae en User32: se declaran aqui. */
    private interface MoreUser32 extends StdCallLibrary {
        MoreUser32 INSTANCE = Native.load("user32", MoreUser32.class);

        boolean ClientToScreen(HWND hWnd, POINT lpPoint);

        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

        boolean IsIconic(HWND hWnd);

        HWND WindowFromPoint(POINT.ByValue point);
    }

    private static final int GA_ROOT = 2, MONITOR_DEFAULTTONULL = 0;
    /** Máximo que se espera a que la ventana deje de estar minimizada tras pedir que se restaure (ms). */
    private static final int RESTORE_WAIT_MS = 3000;

    /** Máximo que se espera a que aparezca la ventana de DOSBox (ms). */
    private static final int WINDOW_WAIT_MS = 20000;

    private final String titlePrefix;

    public DosboxWindow(String titlePrefix) {
        this.titlePrefix = titlePrefix;
    }

    public Optional<HWND> find() {
        List<HWND> found = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;
            char[] buf = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, buf, buf.length);
            String title = Native.toString(buf);
            if (title.startsWith(titlePrefix) && !title.contains("Status Window")) found.add(hwnd);
            return true;
        }, null);
        return found.stream().findFirst();
    }

    public HWND require() {
        return find().orElseThrow(() -> new LegacyException(
                "No se encuentra la ventana de DOSBox (titulo que empieza por '" + titlePrefix
                        + "'). Ejecuta database.bat primero."));
    }

    /**
     * Espera hasta {@code timeoutMs} a que exista la ventana. arrancar.bat lanza DOSBox y el servidor a la vez, sin
     * pausa fija, asi que la primera operación puede llegar antes de que DOSBox haya abierto su ventana.
     */
    public HWND await(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        Optional<HWND> h = find();
        while (h.isEmpty() && System.currentTimeMillis() < deadline) {
            sleep(250);
            h = find();
        }
        return h.orElseGet(this::require); // si sigue sin aparecer, require() lanza el error con la explicacion
    }

    /**
     * Restaura (si estaba minimizada) y da el foco a la ventana (esperandola si aún no ha aparecido). Después comprueba
     * que se ve entera ({@link #problema()}): Robot captura los pixeles de la pantalla, y si otra ventana tapa DOSBox se
     * leería esa.
     */
    public void focus() {
        HWND h = await(WINDOW_WAIT_MS);
        if (MoreUser32.INSTANCE.IsIconic(h)) {
            User32.INSTANCE.ShowWindow(h, WinUser.SW_RESTORE);
            long deadline = System.currentTimeMillis() + RESTORE_WAIT_MS;
            while (MoreUser32.INSTANCE.IsIconic(h) && System.currentTimeMillis() < deadline) sleep(100);
            sleep(300); // la animación de restaurar sigue un momento despues de dejar de estar minimizada
        }
        // Windows solo deja pasar al frente a quien acaba de recibir una entrada de teclado.
        //  Se reintenta hasta que DOSBox sea la ventana activa.
        for (int i = 0; i < 5; i++) {
            MoreUser32.INSTANCE.keybd_event((byte) 0x12, (byte) 0, 0, 0);
            MoreUser32.INSTANCE.keybd_event((byte) 0x12, (byte) 0, 2, 0);
            User32.INSTANCE.SetForegroundWindow(h);
            sleep(150);
            if (h.equals(User32.INSTANCE.GetForegroundWindow())) {
                String p = problema();
                if (p != null) throw new LegacyException(p);
                return;
            }
        }
        throw new LegacyException("No se pudo poner DOSBox en primer plano (la ventana activa es "
                + describe(User32.INSTANCE.GetForegroundWindow()) + ").");
    }

    /**
     * Comprueba que el robot puede trabajar: DOSBox existe, no esta minimizado, es la ventana activa (las teclas van a
     * la ventana activa) y su área cliente se ve entera, sin otra ventana encima ni fuera de la pantalla. Devuelve una
     * explicación para el usuario, o null si todo esta bien. Se mira en 9 puntos repartidos por el área cliente, lejos
     * de las esquinas (por el redondeo).
     */
    public String problema() {
        Optional<HWND> found = find();
        if (found.isEmpty()) return "No se encuentra la ventana de DOSBox: se ha cerrado. Vuelve a abrir arrancar.bat.";
        HWND h = found.get();
        if (MoreUser32.INSTANCE.IsIconic(h)) return "La ventana de DOSBox esta minimizada.";
        HWND fg = User32.INSTANCE.GetForegroundWindow();
        if (!h.equals(fg))
            return "DOSBox ha dejado de ser la ventana activa (ahora es " + describe(fg) + "). No toques el teclado ni"
                    + " el raton mientras trabaja el robot, y vuelve a intentarlo.";
        Rectangle a = clientArea();
        for (int fy = 1; fy <= 3; fy++) {
            for (int fx = 1; fx <= 3; fx++) {
                POINT.ByValue p = new POINT.ByValue(a.x + a.width * fx / 4, a.y + a.height * fy / 4);
                if (User32.INSTANCE.MonitorFromPoint(p, MONITOR_DEFAULTTONULL) == null)
                    return "La ventana de DOSBox se sale de la pantalla: muevela para que se vea entera.";
                HWND at = MoreUser32.INSTANCE.WindowFromPoint(p);
                HWND root = at == null ? null : User32.INSTANCE.GetAncestor(at, GA_ROOT);
                if (!h.equals(root))
                    return "Hay otra ventana encima de DOSBox (" + describe(root) + "): apartala o cierrala para que"
                            + " DOSBox se vea entero.";
            }
        }
        return null;
    }

    /** "'titulo'" de una ventana, para los mensajes de error. */
    private static String describe(HWND h) {
        if (h == null) return "ninguna";
        char[] buf = new char[512];
        User32.INSTANCE.GetWindowText(h, buf, buf.length);
        String t = Native.toString(buf).trim();
        return t.isEmpty() ? "una ventana sin titulo" : "'" + t + "'";
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Área cliente (sin barra de título ni bordes) en coordenadas de pantalla. */
    public Rectangle clientArea() {
        HWND h = require();
        RECT r = new RECT();
        User32.INSTANCE.GetClientRect(h, r);
        POINT origin = new POINT(0, 0);
        MoreUser32.INSTANCE.ClientToScreen(h, origin);
        return new Rectangle(origin.x, origin.y, r.right - r.left, r.bottom - r.top);
    }
}
