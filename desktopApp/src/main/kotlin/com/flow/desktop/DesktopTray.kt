package com.flow.desktop

import java.awt.Color
import java.awt.Font
import java.awt.Frame
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Window
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

/** Windows notification-area integration. Gracefully disables itself when the OS has no tray. */
class DesktopTray(
    private val window: Window,
    private val onExit: () -> Unit
) {
    private var icon: TrayIcon? = null
    val installed: Boolean get() = icon != null

    fun install(): Boolean {
        if (!SystemTray.isSupported()) return false
        return runCatching {
            val menu = PopupMenu()
            val open = MenuItem("نمایش MedFlow")
            open.addActionListener { showWindow() }
            val exit = MenuItem("خروج")
            exit.addActionListener { onExit() }
            menu.add(open)
            menu.addSeparator()
            menu.add(exit)
            val trayIcon = TrayIcon(createIcon(), "MedFlow", menu).apply {
                isImageAutoSize = true
                addActionListener { showWindow() }
            }
            SystemTray.getSystemTray().add(trayIcon)
            icon = trayIcon
            true
        }.getOrDefault(false)
    }

    fun notify(title: String, message: String) {
        runCatching { icon?.displayMessage(title, message, TrayIcon.MessageType.INFO) }
    }

    fun close() {
        val current = icon ?: return
        runCatching { SystemTray.getSystemTray().remove(current) }
        icon = null
    }

    private fun showWindow() {
        SwingUtilities.invokeLater {
            window.isVisible = true
            if (window is Frame) window.extendedState = Frame.NORMAL
            window.toFront()
            window.requestFocus()
        }
    }

    private fun createIcon(): BufferedImage {
        val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color(0x52, 0x68, 0xD8)
            g.fillRoundRect(3, 3, 58, 58, 19, 19)
            g.color = Color.WHITE
            g.font = Font("Segoe UI", Font.BOLD, 39)
            val metrics = g.fontMetrics
            val x = (64 - metrics.stringWidth("M")) / 2
            val y = (64 - metrics.height) / 2 + metrics.ascent
            g.drawString("M", x, y)
        } finally {
            g.dispose()
        }
        return image
    }
}
