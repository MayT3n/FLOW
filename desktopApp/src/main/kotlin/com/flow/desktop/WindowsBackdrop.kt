package com.flow.desktop

import androidx.compose.ui.graphics.Color
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Window

/**
 * Native Windows window chrome integration (DWM). This does NOT make the window transparent and does
 * not claim Mica/Acrylic: all glass in FLOW is rendered by the app itself (design/Glass.kt) so it looks
 * identical on every machine. What DWM is used for:
 *  - immersive dark title bar following the app theme,
 *  - title bar / border colour matched to the wallpaper base (Windows 11, build 22000+),
 *  - rounded window corners (Windows 11).
 * Every call is best-effort; on older Windows builds unsupported attributes are ignored.
 */
object WindowsBackdrop {
    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: Pointer, valueSize: Int): Int
    }

    private val dwm by lazy { Native.load("dwmapi", DwmApi::class.java) }

    fun isSupported(): Boolean = System.getProperty("os.name").lowercase().contains("windows")

    private fun colorRef(c: Color): Int {
        val r = (c.red * 255f + .5f).toInt().coerceIn(0, 255)
        val g = (c.green * 255f + .5f).toInt().coerceIn(0, 255)
        val b = (c.blue * 255f + .5f).toInt().coerceIn(0, 255)
        return r or (g shl 8) or (b shl 16)
    }

    fun applyChrome(window: Window, dark: Boolean, base: Color): Boolean {
        if (!isSupported()) return false
        return runCatching {
            val hwnd = Native.getComponentPointer(window) ?: return false
            if (Pointer.nativeValue(hwnd) == 0L) return false
            val api = dwm
            fun set(attr: Int, value: Int): Int = api.DwmSetWindowAttribute(hwnd, attr, Memory(4).apply { setInt(0, value) }, 4)
            val dark20 = set(20, if (dark) 1 else 0)          // DWMWA_USE_IMMERSIVE_DARK_MODE
            set(33, 2)                                         // DWMWA_WINDOW_CORNER_PREFERENCE = round
            set(35, colorRef(base))                            // DWMWA_CAPTION_COLOR
            set(34, colorRef(base))                            // DWMWA_BORDER_COLOR
            set(36, colorRef(if (dark) Color(0xFFF2F5FF) else Color(0xFF121C33))) // DWMWA_TEXT_COLOR
            dark20 == 0
        }.getOrDefault(false)
    }
}
