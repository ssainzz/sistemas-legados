package sl.p3;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;

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
 * TODO: comprobar con escalado de pantalla distinto del 100 % que las coordenadas de la ventana
 * (pixeles fisicos) coinciden con las de Robot (pixeles logicos). Si no, usar capture.* en la config.
 */
public final class DosboxWindow {
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

    /** Restaura (si estaba minimizada) y da el foco a la ventana. */
    public void focus() {
        HWND h = require();
        User32.INSTANCE.ShowWindow(h, WinUser.SW_RESTORE);
        User32.INSTANCE.SetForegroundWindow(h);
    }

    /** Area cliente (sin barra de titulo ni bordes) en coordenadas de pantalla. */
    public Rectangle clientArea() {
        HWND h = require();
        RECT r = new RECT();
        User32.INSTANCE.GetClientRect(h, r);
        POINT origin = new POINT(0, 0);
        User32.INSTANCE.ClientToScreen(h, origin);
        return new Rectangle(origin.x, origin.y, r.right - r.left, r.bottom - r.top);
    }
}
