package com.flow.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.flow.desktop.design.*
import com.flow.desktop.ui.*
import com.flow.focus.data.ThemeMode
import com.flow.focus.data.TimerStatus

fun main(args: Array<String>) {
    // Headless entry points used by CI (render screenshots / installed-app self-test).
    val renderIdx = args.indexOf("--render-screens")
    if (renderIdx >= 0) { ScreenshotRenderer.run(java.io.File(args.getOrElse(renderIdx + 1) { "screens" })); System.exit(0); return }
    val selfIdx = args.indexOf("--selftest")
    if (selfIdx >= 0) { System.exit(SelfTest.run(args.getOrNull(selfIdx + 1))); return }

    application {
        val runtime = remember { DesktopRuntime() }
        var trayInstalled by remember { mutableStateOf(false) }
        var windowRef by remember { mutableStateOf<java.awt.Window?>(null) }
        Window(
            onCloseRequest = {
                if (trayInstalled) windowRef?.isVisible = false
                else { runtime.close(); exitApplication() }
            },
            title = "FLOW",
            state = WindowState(size = DpSize(1440.dp, 900.dp)),
        ) {
            val desktopWindow = window
            SideEffect { desktopWindow.minimumSize = java.awt.Dimension(980, 620) }
            val tray = remember(desktopWindow) { DesktopTray(desktopWindow) { runtime.close(); exitApplication() } }
            SideEffect { windowRef = desktopWindow }
            DisposableEffect(tray) {
                trayInstalled = tray.install()
                runtime.notificationSink = tray::notify
                onDispose { runtime.notificationSink = null; tray.close() }
            }
            FlowApp(runtime, desktopWindow)
        }
    }
}

/**
 * Root composable. Everything below it reads the palette / glass spec / motion flag from
 * CompositionLocals, so a settings change is reflected on the very next frame.
 */
@Composable
internal fun FlowApp(
    runtime: DesktopRuntime,
    desktopWindow: java.awt.Window? = null,
    initialPage: Page = Page.Home,
    initialCompact: Boolean = false,
    backdrop: BackdropState = remember { BackdropState() },
) {
    var page by remember { mutableStateOf(initialPage) }
    var compactSidebar by remember { mutableStateOf(initialCompact) }
    var searchOpen by remember { mutableStateOf(false) }
    val data by runtime.data.collectAsState()
    val now by runtime.now.collectAsState()
    val settings = data.settings

    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.theme) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> systemDark }
    val palette = if (dark) FlowColors.dark else FlowColors.light
    val baseSpec = GlassProfiles.of(settings.glassQuality)
    val spec = remember(baseSpec, settings.glassOpacity) {
        // glassOpacity is 0.55..1 where 1 is the profile as designed; lower values make panels more see-through.
        val k = 1f + (settings.glassOpacity.coerceIn(.55f, 1f) - 1f) * 1.2f
        baseSpec.copy(fillAlphaLight = baseSpec.fillAlphaLight * k, fillAlphaDark = baseSpec.fillAlphaDark * k)
    }
    val reduceMotion = !settings.animations

    // Window chrome (title bar colour, dark mode, rounded corners) follows the theme.
    if (desktopWindow != null) {
        DisposableEffect(desktopWindow, dark) {
            WindowsBackdrop.applyChrome(desktopWindow, dark, palette.wallBase)
            onDispose { }
        }
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(page) { runCatching { focus.requestFocus() } }

    CompositionLocalProvider(
        LocalFlowPalette provides palette,
        LocalGlass provides spec,
        LocalReduceMotion provides reduceMotion,
        LocalPersianDigits provides settings.persianDigits,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        BackdropHost(
            state = backdrop, dark = dark,
            wallpaperId = if (settings.wallpaperId in WallpaperArt.ids) settings.wallpaperId else "gradient",
            wallpaperPath = settings.wallpaperPath, fit = settings.wallpaperFit,
            dim = settings.wallpaperDim, wallpaperBlur = settings.wallpaperBlurDp,
            spec = spec, useNativeBackdrop = false,
        ) {
            Box(
                Modifier.fillMaxSize().focusRequester(focus).focusTarget().onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    if (e.isCtrlPressed) {
                        val target = when (e.key) {
                            Key.One -> Page.Home; Key.Two -> Page.Focus; Key.Three -> Page.Tasks; Key.Four -> Page.Routines
                            Key.Five -> Page.Goals; Key.Six -> Page.Planning; Key.Seven -> Page.Stats; Key.Eight -> Page.Music
                            Key.Nine -> Page.Settings; else -> null
                        }
                        when {
                            e.key == Key.K -> { searchOpen = true; true }
                            e.key == Key.R && page == Page.Focus -> { runtime.reset(); true }
                            target != null -> { page = target; true }
                            else -> false
                        }
                    } else if (e.key == Key.Spacebar && page == Page.Focus) {
                        if (runtime.repository.current.timer.status == TimerStatus.RUNNING) runtime.pause() else runtime.start()
                        true
                    } else if (e.key == Key.Escape && searchOpen) { searchOpen = false; true } else false
                },
            ) {
                BoxWithConstraints(Modifier.fillMaxSize().padding(FlowDims.windowInset)) {
                    val autoCompact = maxWidth < 1180.dp
                    val narrow = maxWidth < 1020.dp
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                        // First child sits at the right edge in RTL, matching the design.
                        Sidebar(
                            selected = page, onSelect = { page = it },
                            compact = compactSidebar || autoCompact, onToggleCompact = { compactSidebar = !compactSidebar },
                            dark = dark,
                            onToggleTheme = { runtime.updateSettings { it.copy(theme = if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } },
                        )
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            Column(Modifier.fillMaxHeight().widthIn(max = FlowDims.contentMaxWidth).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
                                val greeting = if (page == Page.Home) greetingFor(java.time.LocalTime.now().hour).first else null
                                Toolbar(page, greeting, now, runtime, { searchOpen = true }, { page = Page.Focus }, narrow = narrow)
                                AnimatedContent(
                                    targetState = page,
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    transitionSpec = {
                                        if (reduceMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                        else (fadeIn(tween(FlowMotion.page, easing = FlowMotion.emphasized)) + slideInVertically(tween(FlowMotion.page, easing = FlowMotion.emphasized)) { it / 40 }) togetherWith fadeOut(tween(110))
                                    },
                                    label = "page",
                                ) { p ->
                                    Box(Modifier.fillMaxSize()) {
                                        when (p) {
                                            Page.Home -> DashboardPage(data, now, runtime) { page = it }
                                            Page.Focus -> FocusPage(data, now, runtime)
                                            Page.Tasks -> TasksPage(data, runtime)
                                            Page.Routines -> RoutinesPage(data, runtime)
                                            Page.Goals -> GoalsPage(data, runtime)
                                            Page.Planning -> PlanningPage(data, runtime)
                                            Page.Stats -> StatsPage(data)
                                            Page.Music -> MusicPage(runtime, data)
                                            Page.Settings -> SettingsPage(data, runtime)
                                        }
                                    }
                                }
                                if (page != Page.Music) MiniPlayer(runtime, { page = Page.Music })
                            }
                        }
                    }
                }
                if (searchOpen) CommandPalette(onSelect = { page = it; searchOpen = false }, onDismiss = { searchOpen = false })
            }
        }
    }
}
