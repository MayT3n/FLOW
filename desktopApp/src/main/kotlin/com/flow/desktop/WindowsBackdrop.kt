package com.flow.desktop

import com.flow.focus.data.GlassQuality
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Color
import java.awt.Window
import javax.swing.RootPaneContainer

/** Native Windows 11 system backdrop with a safe fallback outside Windows. */
object WindowsBackdrop {
    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: Pointer, valueSize: Int): Int
    }

    private val dwm by lazy { Native.load("dwmapi", DwmApi::class.java) }

    fun isSupported(): Boolean = System.getProperty("os.name").lowercase().contains("windows")

    fun apply(window: Window, dark: Boolean, quality: GlassQuality): Boolean {
        if (!isSupported()) return false
        return runCatching {
            val hwnd = Native.getComponentPointer(window)
            if (hwnd == null || Pointer.nativeValue(hwnd) == 0L) return false

            if (window is RootPaneContainer) {
                window.rootPane.isOpaque = false
                (window.contentPane as? javax.swing.JComponent)?.isOpaque = false
            }
            window.setBackground(Color(0, 0, 0, 0))

            // DWM system backdrop types: MAINWINDOW = Mica, TRANSIENTWINDOW = Acrylic.
            // Acrylic is reserved for quality profiles; performance modes keep Mica.
            val backdropType = when (quality) {
                GlassQuality.LOW, GlassQuality.MEDIUM -> 2
                GlassQuality.HIGH, GlassQuality.ULTRA -> 3
            }
            val requestedBackdrop = Memory(4).apply { setInt(0, backdropType) }
            val immersiveDark = Memory(4).apply { setInt(0, if (dark) 1 else 0) }
            val rounded = Memory(4).apply { setInt(0, 2) }
            val api = dwm

            val requestedResult = api.DwmSetWindowAttribute(hwnd, 38, requestedBackdrop, 4)
            val result = if (requestedResult != 0 && backdropType == 3) {
                // Older Windows builds can reject Acrylic. Fall back to Mica.
                val mica = Memory(4).apply { setInt(0, 2) }
                api.DwmSetWindowAttribute(hwnd, 38, mica, 4)
            } else requestedResult

            api.DwmSetWindowAttribute(hwnd, 20, immersiveDark, 4)
            api.DwmSetWindowAttribute(hwnd, 33, rounded, 4)
            result == 0
        }.getOrDefault(false)
    }
}
