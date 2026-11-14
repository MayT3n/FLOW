package com.flow.desktop

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Color
import java.awt.Window
import javax.swing.RootPaneContainer

/** Native Windows 11 Mica backdrop. Falls back cleanly on older Windows and other operating systems. */
object WindowsBackdrop {
    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: Pointer, valueSize: Int): Int
    }

    private val dwm by lazy { Native.load("dwmapi", DwmApi::class.java) }

    fun isSupported(): Boolean = System.getProperty("os.name").lowercase().contains("windows")

    fun apply(window: Window, dark: Boolean): Boolean {
        if (!isSupported()) return false
        return runCatching {
            val hwnd = Native.getComponentPointer(window)
            if (hwnd == null || Pointer.nativeValue(hwnd) == 0L) return false
            if (window is RootPaneContainer) {
                window.rootPane.isOpaque = false
                (window.contentPane as? javax.swing.JComponent)?.isOpaque = false
            }
            window.setBackground(Color(0, 0, 0, 0))
            val mica = Memory(4).apply { setInt(0, 2) } // DWMSBT_MAINWINDOW
            val immersiveDark = Memory(4).apply { setInt(0, if (dark) 1 else 0) }
            val rounded = Memory(4).apply { setInt(0, 2) } // DWMWCP_ROUND
            val api = dwm
            val backdropResult = api.DwmSetWindowAttribute(hwnd, 38, mica, 4)
            api.DwmSetWindowAttribute(hwnd, 20, immersiveDark, 4)
            api.DwmSetWindowAttribute(hwnd, 33, rounded, 4)
            backdropResult == 0
        }.getOrDefault(false)
    }
}
