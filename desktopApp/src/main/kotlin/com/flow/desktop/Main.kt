package com.flow.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.DpSize
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.PhaseType
import com.flow.focus.data.Priority
import com.flow.focus.data.ThemeMode
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.WallpaperFit
import com.flow.focus.data.GoalPeriod
import com.flow.focus.data.Task
import com.flow.focus.data.TimerStatus
import com.flow.focus.domain.Planner
import com.flow.focus.domain.PomodoroEngine
import com.flow.focus.domain.Stats
import com.flow.focus.domain.StatsRange
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val Accent = MedFlowDesign.darkAccent
private val MutedDark = Color(0xFFA5AEC0)
private val MutedLight = Color(0xFF667085)
private val LocalDesktopSettings = androidx.compose.runtime.staticCompositionLocalOf { com.flow.focus.data.Settings() }

// Segoe UI is available on Windows 11 and provides a native-feeling Persian-capable UI fallback.
@OptIn(ExperimentalTextApi::class)
private val WindowsUiFont = FontFamily("Segoe UI")
private val BaseDesktopTypography = Typography()
private val MedFlowTypography = Typography(
    displayLarge = BaseDesktopTypography.displayLarge.copy(fontFamily = WindowsUiFont),
    displayMedium = BaseDesktopTypography.displayMedium.copy(fontFamily = WindowsUiFont),
    displaySmall = BaseDesktopTypography.displaySmall.copy(fontFamily = WindowsUiFont),
    headlineLarge = BaseDesktopTypography.headlineLarge.copy(fontFamily = WindowsUiFont),
    headlineMedium = BaseDesktopTypography.headlineMedium.copy(fontFamily = WindowsUiFont),
    headlineSmall = BaseDesktopTypography.headlineSmall.copy(fontFamily = WindowsUiFont),
    titleLarge = BaseDesktopTypography.titleLarge.copy(fontFamily = WindowsUiFont),
    titleMedium = BaseDesktopTypography.titleMedium.copy(fontFamily = WindowsUiFont),
    titleSmall = BaseDesktopTypography.titleSmall.copy(fontFamily = WindowsUiFont),
    bodyLarge = BaseDesktopTypography.bodyLarge.copy(fontFamily = WindowsUiFont),
    bodyMedium = BaseDesktopTypography.bodyMedium.copy(fontFamily = WindowsUiFont),
    bodySmall = BaseDesktopTypography.bodySmall.copy(fontFamily = WindowsUiFont),
    labelLarge = BaseDesktopTypography.labelLarge.copy(fontFamily = WindowsUiFont),
    labelMedium = BaseDesktopTypography.labelMedium.copy(fontFamily = WindowsUiFont),
    labelSmall = BaseDesktopTypography.labelSmall.copy(fontFamily = WindowsUiFont)
)

private val MedFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

fun main() = application {
    val runtime = remember { DesktopRuntime() }
    var trayInstalled by remember { mutableStateOf(false) }
    var windowRef by remember { mutableStateOf<java.awt.Window?>(null) }
    Window(onCloseRequest = {
        if (trayInstalled) windowRef?.isVisible = false
        else { runtime.close(); exitApplication() }
    }, title = "MedFlow", state = WindowState(size = DpSize(1440.dp, 900.dp))) {
        val desktopWindow = window
        val tray = remember(desktopWindow) {
            DesktopTray(desktopWindow) {
                runtime.close()
                exitApplication()
            }
        }
        SideEffect { windowRef = desktopWindow }
        DisposableEffect(tray) {
            trayInstalled = tray.install()
            runtime.notificationSink = tray::notify
            onDispose {
                runtime.notificationSink = null
                tray.close()
            }
        }
        MedFlowDesktop(runtime, desktopWindow)
    }
}

@Composable
private fun MedFlowDesktop(runtime: DesktopRuntime, desktopWindow: java.awt.Window) {
    var selected by remember { mutableStateOf("خانه") }
    var compactSidebar by remember { mutableStateOf(false) }
    val data by runtime.data.collectAsState()
    val dark = data.settings.theme != ThemeMode.LIGHT
    DisposableEffect(desktopWindow, dark) {
        WindowsBackdrop.apply(desktopWindow, dark)
        onDispose { }
    }
    val now by runtime.now.collectAsState()
    val scheme = if (dark) darkColorScheme(
        primary = MedFlowDesign.darkAccent,
        onPrimary = Color(0xFF101A2A),
        background = MedFlowDesign.darkBackground,
        surface = MedFlowDesign.darkSurface,
        surfaceVariant = MedFlowDesign.darkVariant,
        onBackground = MedFlowDesign.darkText,
        onSurface = MedFlowDesign.darkText,
        onSurfaceVariant = MedFlowDesign.darkMuted,
        outline = Color(0xFF344253)
    ) else lightColorScheme(
        primary = MedFlowDesign.lightAccent,
        onPrimary = Color.White,
        background = MedFlowDesign.lightBackground,
        surface = MedFlowDesign.lightSurface,
        surfaceVariant = MedFlowDesign.lightVariant,
        onBackground = MedFlowDesign.lightText,
        onSurface = MedFlowDesign.lightText,
        onSurfaceVariant = MedFlowDesign.lightMuted,
        outline = MedFlowDesign.lightBorder
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, typography = MedFlowTypography, shapes = MedFlowShapes) {
            CompositionLocalProvider(LocalDesktopSettings provides data.settings) {
                val wallpaper = remember(data.settings.wallpaperPath) {
                    data.settings.wallpaperPath?.let { path ->
                        runCatching { File(path).inputStream().use { loadImageBitmap(it) } }.getOrNull()
                    }
                }
                Box(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                        val page = when (event.key) {
                            Key.One -> "خانه"
                            Key.Two -> "تمرکز"
                            Key.Three -> "وظایف"
                            Key.Four -> "روتین‌ها"
                            Key.Five -> "اهداف"
                            Key.Six -> "برنامه‌ریزی"
                            Key.Seven -> "آمار"
                            Key.Eight -> "موسیقی"
                            Key.Nine -> "تنظیمات"
                            else -> null
                        }
                        if (page != null) { selected = page; true } else false
                    } else false
                }) {
                    Canvas(Modifier.fillMaxSize()) {
                        if (data.settings.wallpaperId == "mica" && WindowsBackdrop.isSupported() && wallpaper == null) {
                            // The native Windows Mica surface remains visible beneath the Compose content.
                        } else if (wallpaper != null) {
                            val scale = if (data.settings.wallpaperFit == WallpaperFit.CROP)
                                maxOf(size.width / wallpaper.width, size.height / wallpaper.height)
                            else minOf(size.width / wallpaper.width, size.height / wallpaper.height)
                            val w = (wallpaper.width * scale).toInt().coerceAtLeast(1)
                            val h = (wallpaper.height * scale).toInt().coerceAtLeast(1)
                            drawImage(
                                wallpaper,
                                dstOffset = IntOffset((size.width.toInt() - w) / 2, (size.height.toInt() - h) / 2),
                                dstSize = IntSize(w, h)
                            )
                        } else {
                            val colors = when (data.settings.wallpaperId) {
                                "aurora" -> listOf(Color(0xFF07172B), Color(0xFF173B59), Color(0xFF382B65))
                                "midnight" -> listOf(Color(0xFF070D1B), Color(0xFF172A42), Color(0xFF26364B))
                                "dusk" -> listOf(Color(0xFF241B37), Color(0xFF69435E), Color(0xFF273D5C))
                                else -> if (dark)
                                    listOf(MedFlowDesign.darkBackground, Color(0xFF142131), Color(0xFF182332))
                                else
                                    listOf(MedFlowDesign.lightBackground, Color(0xFFE8EDF4), Color(0xFFF8FAFC))
                            }
                            drawRect(Brush.linearGradient(colors))
                        }
                        if (data.settings.wallpaperDim > 0f) {
                            drawRect(Color.Black.copy(alpha = data.settings.wallpaperDim.coerceIn(0f, .65f)))
                        }
                    }

                    BoxWithConstraints(Modifier.fillMaxSize().padding(MedFlowDesign.windowInset)) {
                        val useCompactSidebar = compactSidebar || maxWidth < 1120.dp
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                        // RTL places the navigation rail on the right, while native window controls stay untouched.
                        Sidebar(
                            selected = selected,
                            onSelect = { selected = it },
                            dark = dark,
                            onThemeChange = {
                                runtime.updateSettings {
                                    it.copy(theme = if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
                                }
                            },
                            compact = useCompactSidebar,
                            onToggleCompact = { compactSidebar = !compactSidebar }
                        )

                        Box(
                            Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                Modifier.fillMaxHeight()
                                    .widthIn(max = MedFlowDesign.contentMaxWidth)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(
                                            selected,
                                            fontSize = 27.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.35).sp
                                        )
                                        Text(
                                            DateFormats.full(java.time.LocalDate.now()),
                                            color = muted(),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "MedFlow Workspace",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .82f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = .4.sp
                                        )
                                        Text(
                                            "فضای شخصی مدیریت زمان",
                                            color = muted(),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Crossfade(
                                    targetState = selected,
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    animationSpec = if (data.settings.animations)
                                        androidx.compose.animation.core.tween(durationMillis = 180)
                                    else androidx.compose.animation.core.snap(),
                                    label = "desktop-page-transition"
                                ) { page ->
                                    when (page) {
                                        "خانه" -> Dashboard(data, now, runtime)
                                        "تمرکز" -> FocusPage(data, now, runtime)
                                        "وظایف" -> TasksPage(data, runtime)
                                        "روتین‌ها" -> RoutinesPage(data, runtime)
                                        "اهداف" -> GoalsPage(data, runtime)
                                        "برنامه‌ریزی" -> PlanningPage(data)
                                        "آمار" -> StatisticsPage(data)
                                        "موسیقی" -> MusicPage(runtime)
                                        "تنظیمات" -> SettingsPage(
                                            data, runtime, dark,
                                            { runtime.updateSettings { it.copy(theme = if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    selected: String,
    onSelect: (String) -> Unit,
    dark: Boolean,
    onThemeChange: () -> Unit,
    compact: Boolean,
    onToggleCompact: () -> Unit
) {
    val items = listOf(
        "خانه" to "home",
        "تمرکز" to "timer",
        "وظایف" to "tasks",
        "روتین‌ها" to "routine",
        "اهداف" to "target",
        "برنامه‌ریزی" to "calendar",
        "آمار" to "chart",
        "موسیقی" to "music",
        "تنظیمات" to "settings"
    )
    val shape = RoundedCornerShape(MedFlowDesign.sidebarRadius)
    val width = if (compact) MedFlowDesign.sidebarCollapsed else MedFlowDesign.sidebarExpanded

    Column(
        Modifier.width(width)
            .fillMaxHeight()
            .shadow(16.dp, shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), shape)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) .09f else .72f),
                shape
            )
            .padding(horizontal = if (compact) 9.dp else 13.dp, vertical = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (compact) 1.dp else 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start
        ) {
            Box(
                Modifier.size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = .24f),
                            MaterialTheme.colorScheme.primary.copy(alpha = .09f)
                        )),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("M", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
            if (!compact) {
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("MedFlow", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("DESKTOP WORKSPACE", color = muted(), fontSize = 8.sp, letterSpacing = 1.45.sp)
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        items.forEach { (item, glyph) ->
            val active = selected == item
            Surface(
                onClick = { onSelect(item) },
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = if (dark) .17f else .10f)
                    else Color.Transparent,
                shape = RoundedCornerShape(MedFlowDesign.controlRadius),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start
                ) {
                    Box(
                        Modifier.size(36.dp)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = .13f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = .035f),
                                RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        SidebarGlyph(
                            icon = glyph,
                            tint = if (active) MaterialTheme.colorScheme.primary else muted()
                        )
                    }
                    if (!compact) {
                        Spacer(Modifier.width(11.dp))
                        Text(
                            item,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .88f),
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        TextButton(onClick = onThemeChange, modifier = Modifier.fillMaxWidth()) {
            Text(if (dark) "☼" else "☾", fontSize = 16.sp)
            if (!compact) {
                Spacer(Modifier.width(7.dp))
                Text(if (dark) "حالت روشن" else "حالت تیره", color = muted())
            }
        }
        TextButton(onClick = onToggleCompact, modifier = Modifier.fillMaxWidth()) {
            Text(if (compact) "›" else "‹", fontSize = 18.sp)
            if (!compact) {
                Spacer(Modifier.width(7.dp))
                Text("جمع‌کردن منو", color = muted(), fontSize = 11.sp)
            }
        }
        if (!compact) {
            Text(
                "نسخه‌ی دسکتاپ",
                color = muted().copy(alpha = .75f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SidebarGlyph(icon: String, tint: Color) {
    Canvas(Modifier.size(19.dp)) {
        val w = size.width
        val h = size.height
        val sw = 1.65.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(block: Path.() -> Unit) {
            drawPath(Path().apply(block), tint, style = stroke)
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, androidx.compose.ui.geometry.Offset(x1, y1), androidx.compose.ui.geometry.Offset(x2, y2), sw, cap = StrokeCap.Round)
        }
        when (icon) {
            "home" -> path {
                moveTo(w * .13f, h * .48f); lineTo(w * .5f, h * .16f); lineTo(w * .87f, h * .48f)
                moveTo(w * .23f, h * .43f); lineTo(w * .23f, h * .86f); lineTo(w * .77f, h * .86f)
                lineTo(w * .77f, h * .43f)
                moveTo(w * .43f, h * .86f); lineTo(w * .43f, h * .62f); lineTo(w * .57f, h * .62f); lineTo(w * .57f, h * .86f)
            }
            "timer" -> {
                drawCircle(tint, radius = w * .36f, center = androidx.compose.ui.geometry.Offset(w * .5f, h * .55f), style = stroke)
                line(w * .5f, h * .04f, w * .5f, h * .16f)
                line(w * .38f, h * .04f, w * .62f, h * .04f)
                line(w * .5f, h * .55f, w * .5f, h * .31f)
                line(w * .5f, h * .55f, w * .68f, h * .64f)
            }
            "tasks" -> path {
                moveTo(w * .42f, h * .2f); lineTo(w * .86f, h * .2f)
                moveTo(w * .42f, h * .5f); lineTo(w * .86f, h * .5f)
                moveTo(w * .42f, h * .8f); lineTo(w * .86f, h * .8f)
                moveTo(w * .12f, h * .2f); lineTo(w * .2f, h * .28f); lineTo(w * .32f, h * .12f)
                moveTo(w * .12f, h * .5f); lineTo(w * .2f, h * .58f); lineTo(w * .32f, h * .42f)
                moveTo(w * .16f, h * .8f); lineTo(w * .3f, h * .8f)
            }
            "routine" -> {
                path {
                    moveTo(w * .77f, h * .34f); lineTo(w * .77f, h * .13f); lineTo(w * .56f, h * .13f)
                    moveTo(w * .77f, h * .13f); lineTo(w * .62f, h * .29f)
                    moveTo(w * .23f, h * .66f); lineTo(w * .23f, h * .87f); lineTo(w * .44f, h * .87f)
                    moveTo(w * .23f, h * .87f); lineTo(w * .38f, h * .71f)
                }
                drawArc(tint, 35f, 145f, false, androidx.compose.ui.geometry.Offset(w * .13f, h * .22f), androidx.compose.ui.geometry.Size(w * .74f, h * .62f), style = stroke)
                drawArc(tint, 215f, 145f, false, androidx.compose.ui.geometry.Offset(w * .13f, h * .16f), androidx.compose.ui.geometry.Size(w * .74f, h * .62f), style = stroke)
            }
            "target" -> {
                drawCircle(tint, radius = w * .38f, style = stroke)
                drawCircle(tint, radius = w * .23f, style = stroke)
                drawCircle(tint, radius = w * .07f)
            }
            "calendar" -> path {
                moveTo(w * .17f, h * .25f); lineTo(w * .83f, h * .25f); lineTo(w * .83f, h * .86f)
                lineTo(w * .17f, h * .86f); close()
                moveTo(w * .17f, h * .42f); lineTo(w * .83f, h * .42f)
                moveTo(w * .34f, h * .12f); lineTo(w * .34f, h * .34f)
                moveTo(w * .66f, h * .12f); lineTo(w * .66f, h * .34f)
                moveTo(w * .34f, h * .56f); lineTo(w * .34f, h * .63f)
                moveTo(w * .5f, h * .56f); lineTo(w * .5f, h * .63f)
                moveTo(w * .66f, h * .56f); lineTo(w * .66f, h * .63f)
                moveTo(w * .34f, h * .73f); lineTo(w * .34f, h * .8f)
            }
            "chart" -> {
                path {
                    moveTo(w * .14f, h * .12f); lineTo(w * .14f, h * .86f); lineTo(w * .9f, h * .86f)
                }
                drawRoundRect(tint, androidx.compose.ui.geometry.Offset(w * .25f, h * .56f), androidx.compose.ui.geometry.Size(w * .13f, h * .3f), androidx.compose.ui.geometry.CornerRadius(w * .035f))
                drawRoundRect(tint, androidx.compose.ui.geometry.Offset(w * .46f, h * .36f), androidx.compose.ui.geometry.Size(w * .13f, h * .5f), androidx.compose.ui.geometry.CornerRadius(w * .035f))
                drawRoundRect(tint, androidx.compose.ui.geometry.Offset(w * .67f, h * .19f), androidx.compose.ui.geometry.Size(w * .13f, h * .67f), androidx.compose.ui.geometry.CornerRadius(w * .035f))
            }
            "music" -> {
                path {
                    moveTo(w * .55f, h * .18f); lineTo(w * .55f, h * .68f)
                    lineTo(w * .84f, h * .61f); lineTo(w * .84f, h * .1f)
                    lineTo(w * .55f, h * .18f)
                }
                drawCircle(tint, radius = w * .12f, center = androidx.compose.ui.geometry.Offset(w * .39f, h * .75f))
                drawCircle(tint, radius = w * .12f, center = androidx.compose.ui.geometry.Offset(w * .69f, h * .67f))
            }
            "settings" -> {
                drawCircle(tint, radius = w * .31f, style = stroke)
                drawCircle(tint, radius = w * .11f, style = stroke)
                for (i in 0 until 8) {
                    val angle = i * (Math.PI / 4.0)
                    val x1 = w * .5f + kotlin.math.cos(angle).toFloat() * w * .35f
                    val y1 = h * .5f + kotlin.math.sin(angle).toFloat() * h * .35f
                    val x2 = w * .5f + kotlin.math.cos(angle).toFloat() * w * .46f
                    val y2 = h * .5f + kotlin.math.sin(angle).toFloat() * h * .46f
                    line(x1, y1, x2, y2)
                }
            }
        }
    }
}

@Composable
private fun Dashboard(data: AppData, now: Long, runtime: DesktopRuntime) {
    val todayStats = Stats.summarize(data, StatsRange.DAY, java.time.LocalDate.now())
    val weekStats = Stats.summarize(data, StatsRange.WEEK, java.time.LocalDate.now())
    val today = java.time.LocalDate.now()
    val maxWeekMinutes = (weekStats.buckets.maxOfOrNull { it.minutes } ?: 1L).coerceAtLeast(1L)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                "تمرکز امروز",
                "${fa(todayStats.focusMinutes)} دقیقه",
                "زمان ثبت‌شده",
                Modifier.weight(1f)
            )
            MetricCard(
                "وظایف باز",
                fa(data.tasks.count { !it.done }),
                "کارهای باقی‌مانده",
                Modifier.weight(1f)
            )
            MetricCard(
                "جلسه‌های کامل",
                fa(todayStats.completedSessions),
                "جلسه‌ی امروز",
                Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FocusCard(data, now, runtime, Modifier.weight(1.3f).height(370.dp))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Panel("برنامه‌ی امروز", Modifier.fillMaxWidth()) {
                    val entries = Planner.entriesFor(data, today)
                    if (entries.isEmpty()) {
                        EmptyText("برای امروز برنامه‌ای ثبت نشده است.")
                    } else {
                        entries.take(5).forEach { entry ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(7.dp).background(
                                        if (entry.done) Color(0xFF55B88A) else MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(entry.title, Modifier.weight(1f), maxLines = 1, fontSize = 12.sp)
                                entry.minute?.let {
                                    Text(DateFormats.clock(it), color = muted(), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Panel("کارهای بعدی", Modifier.fillMaxWidth()) {
                    val tasks = data.tasks.filterNot { it.done }
                        .sortedWith(compareBy<Task> { it.dueDay ?: Long.MAX_VALUE }.thenByDescending { it.priority.ordinal })
                        .take(4)
                    if (tasks.isEmpty()) {
                        EmptyText("همه‌ی وظایف فعلی انجام شده‌اند.")
                    } else {
                        tasks.forEachIndexed { index, task ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(task.title, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
                                    val due = task.dueDay?.let {
                                        DateFormats.full(java.time.LocalDate.ofEpochDay(it))
                                    } ?: task.priority.label
                                    Text(due, color = muted(), fontSize = 10.sp, maxLines = 1)
                                }
                                Checkbox(
                                    checked = task.done,
                                    onCheckedChange = { runtime.setTaskDone(task.id, it) }
                                )
                            }
                            if (index != tasks.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .055f))
                            }
                        }
                    }
                }
            }
        }

        Panel("نمای هفتگی تمرکز", Modifier.fillMaxWidth()) {
            Text("بر اساس جلسه‌های واقعی ثبت‌شده در این هفته", color = muted(), fontSize = 11.sp)
            if (weekStats.buckets.all { it.minutes == 0L }) {
                EmptyText("با ثبت اولین جلسه، روند هفتگی اینجا نمایش داده می‌شود.")
            } else {
                Row(
                    Modifier.fillMaxWidth().height(94.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekStats.buckets.forEach { bucket ->
                        val ratio = (bucket.minutes.toFloat() / maxWeekMinutes).coerceIn(0f, 1f)
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(fa(bucket.minutes), color = muted(), fontSize = 10.sp, maxLines = 1)
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier.fillMaxWidth(.56f).height(54.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight(ratio)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .82f), RoundedCornerShape(7.dp))
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(bucket.label, color = muted(), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusPage(data: AppData, now: Long, runtime: DesktopRuntime) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FocusCard(data, now, runtime, Modifier.fillMaxWidth().height(420.dp))
        Panel("نکته", Modifier.fillMaxWidth()) {
            Text("زمان‌سنج بر اساس زمان پایان ذخیره‌شده کار می‌کند؛ بستن و بازکردن برنامه شمارش معکوس را از صفر شروع نمی‌کند.", color = muted())
        }
    }
}

@Composable
private fun FocusCard(data: AppData, now: Long, runtime: DesktopRuntime, modifier: Modifier = Modifier) {
    var taskMenuExpanded by remember { mutableStateOf(false) }
    val timer = data.timer
    val selectedTask = data.tasks.firstOrNull { it.id == timer.taskId }
    val remaining = PomodoroEngine.remaining(timer, now)
    val progress = PomodoroEngine.progress(timer, now)
    val animatedProgress by androidx.compose.animation.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (LocalDesktopSettings.current.animations)
            androidx.compose.animation.core.tween(durationMillis = 220)
        else androidx.compose.animation.core.snap(),
        label = "focus-progress"
    )
    val phaseName = when (timer.phase) {
        PhaseType.FOCUS -> "زمان تمرکز"
        PhaseType.SHORT_BREAK -> "استراحت کوتاه"
        PhaseType.LONG_BREAK -> "استراحت بلند"
    }
    Panel(phaseName, modifier) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box {
                OutlinedButton(onClick = { taskMenuExpanded = true }) {
                    Text(selectedTask?.title ?: "بدون وظیفه‌ی مرتبط", maxLines = 1)
                }
                DropdownMenu(expanded = taskMenuExpanded, onDismissRequest = { taskMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("بدون وظیفه‌ی مرتبط") }, onClick = {
                        runtime.selectTask(null); taskMenuExpanded = false
                    })
                    data.tasks.filterNot { it.done }.forEach { task ->
                        DropdownMenuItem(text = { Text(task.title) }, onClick = {
                            runtime.selectTask(task.id); taskMenuExpanded = false
                        })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhaseType.entries.forEach { phase ->
                    val label = when (phase) {
                        PhaseType.FOCUS -> "تمرکز"
                        PhaseType.SHORT_BREAK -> "استراحت کوتاه"
                        PhaseType.LONG_BREAK -> "استراحت بلند"
                    }
                    OutlinedButton(onClick = { runtime.selectPhase(phase) }) { Text(label, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(timerText(remaining), fontSize = 66.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = animatedProgress, modifier = Modifier.fillMaxWidth(.78f).height(7.dp),
                color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(14.dp))
            Text("دور ${fa(timer.round)} از ${fa(data.settings.rounds)}", color = muted())
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (timer.status == TimerStatus.RUNNING) runtime.pause() else runtime.start() }) {
                    Text(if (timer.status == TimerStatus.RUNNING) "مکث" else if (timer.status == TimerStatus.PAUSED) "ادامه" else "شروع")
                }
                OutlinedButton(onClick = runtime::reset) { Text("بازنشانی") }
                OutlinedButton(onClick = runtime::skip) { Text("رد کردن") }
            }
        }
    }
}

@Composable
private fun TasksPage(data: AppData, runtime: DesktopRuntime) {
    var editing by remember { mutableStateOf<Task?>(null) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var dueTime by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var category by remember { mutableStateOf("personal") }
    var search by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val categories = listOf("study" to "درس", "work" to "کار", "sport" to "ورزش", "personal" to "شخصی", "project" to "پروژه")
    fun clearEditor() {
        editing = null; title = ""; notes = ""; dueDate = ""; dueTime = ""
        priority = Priority.MEDIUM; category = "personal"; error = ""
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(if (editing == null) "افزودن وظیفه" else "ویرایش وظیفه", Modifier.fillMaxWidth()) {
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("عنوان وظیفه") }, shape = RoundedCornerShape(14.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(),
                minLines = 2, label = { Text("یادداشت (اختیاری)") }, shape = RoundedCornerShape(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, modifier = Modifier.weight(1f),
                    singleLine = true, label = { Text("تاریخ جلالی: ۱۴۰۵/۰۸/۲۳") }, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = dueTime, onValueChange = { dueTime = it }, modifier = Modifier.weight(1f),
                    singleLine = true, label = { Text("ساعت: ۱۸:۳۰") }, shape = RoundedCornerShape(14.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("اولویت:")
                Priority.entries.forEach { p ->
                    if (priority == p) Button(onClick = { priority = p }) { Text(p.label) }
                    else OutlinedButton(onClick = { priority = p }) { Text(p.label) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("دسته:")
                categories.forEach { (id, label) ->
                    if (category == id) Button(onClick = { category = id }) { Text(label) }
                    else OutlinedButton(onClick = { category = id }) { Text(label) }
                }
            }
            if (error.isNotBlank()) Text(error, color = Color(0xFFE07B7B), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    val parsedDate = if (dueDate.isBlank()) null else parseJalaliDate(dueDate)
                    val parsedTime = if (dueTime.isBlank()) null else parseClock(dueTime)
                    if (title.isBlank()) error = "عنوان وظیفه را وارد کن."
                    else if (dueDate.isNotBlank() && parsedDate == null) error = "تاریخ را به شکل سال/ماه/روز جلالی وارد کن."
                    else if (dueTime.isNotBlank() && parsedTime == null) error = "ساعت را به شکل ساعت:دقیقه وارد کن."
                    else {
                        val old = editing
                        if (old == null) runtime.addTask(title, notes, parsedDate, parsedTime, priority, category)
                        else runtime.saveTask(old.copy(title = title.trim(), notes = notes.trim(),
                            dueDay = parsedDate?.toEpochDay(), dueMinute = parsedTime, priority = priority, categoryId = category))
                        clearEditor()
                    }
                }, enabled = title.isNotBlank()) { Text(if (editing == null) "ثبت وظیفه" else "ذخیره تغییرات") }
                if (editing != null) OutlinedButton(onClick = { clearEditor() }) { Text("انصراف") }
            }
        }
        Panel("فهرست وظایف", Modifier.fillMaxWidth()) {
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("جست‌وجوی عنوان و یادداشت") }, shape = RoundedCornerShape(14.dp))
            val normalizedSearch = com.flow.focus.domain.TaskQuery.normalize(search)
            val tasks = data.tasks.filter { normalizedSearch.isBlank() || com.flow.focus.domain.TaskQuery.normalize(it.title).contains(normalizedSearch) || com.flow.focus.domain.TaskQuery.normalize(it.notes).contains(normalizedSearch) }
                .sortedWith(compareBy<Task> { it.done }.thenByDescending { it.createdAt })
            if (tasks.isEmpty()) EmptyText("وظیفه‌ای پیدا نشد.")
            tasks.forEach { task ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.done, onCheckedChange = { runtime.setTaskDone(task.id, it) })
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(task.title, fontWeight = FontWeight.Medium,
                            color = if (task.done) muted() else MaterialTheme.colorScheme.onSurface)
                        if (task.notes.isNotBlank()) Text(task.notes, color = muted(), fontSize = 11.sp, maxLines = 2)
                        val due = task.dueDay?.let { DateFormats.full(java.time.LocalDate.ofEpochDay(it)) } ?: "بدون سررسید"
                        val time = task.dueMinute?.let { " · ${DateFormats.clock(it)}" } ?: ""
                        Text("${due}${time} · ${task.priority.label} · ${categories.firstOrNull { it.first == task.categoryId }?.second ?: "شخصی"}",
                            color = muted(), fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        editing = task; title = task.title; notes = task.notes
                        dueDate = task.dueDay?.let { epoch ->
                            val j = JalaliDate.of(java.time.LocalDate.ofEpochDay(epoch))
                            "${j.year}/${j.month.toString().padStart(2,'0')}/${j.day.toString().padStart(2,'0')}"
                        } ?: ""
                        dueTime = task.dueMinute?.let { DateFormats.clock(it) } ?: ""
                        priority = task.priority; category = task.categoryId; error = ""
                    }) { Text("ویرایش") }
                    TextButton(onClick = { runtime.deleteTask(task.id) }) { Text("حذف", color = Color(0xFFE07B7B)) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))
            }
        }
    }
}

@Composable
private fun RoutinesPage(data: AppData, runtime: DesktopRuntime) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val weekdays = remember { androidx.compose.runtime.mutableStateListOf(0,1,2,3,4,5,6) }
    val labels = JalaliDate.weekdayNames
    val today = java.time.LocalDate.now()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel("روتین تکرارشونده", Modifier.fillMaxWidth()) {
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("عنوان روتین") })
            OutlinedTextField(value = time, onValueChange = { time = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("ساعت اختیاری، مثل ۰۷:۳۰") })
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                labels.forEachIndexed { index, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Checkbox(checked = index in weekdays, onCheckedChange = { checked ->
                            if (checked && index !in weekdays) weekdays.add(index)
                            if (!checked) weekdays.remove(index)
                        })
                        Text(day.take(1), fontSize = 11.sp, color = muted())
                    }
                }
            }
            Button(onClick = {
                val minute = if (time.isBlank()) null else parseClock(time)
                if (title.isNotBlank() && (time.isBlank() || minute != null))
                    runtime.addRoutine(title, weekdays.toList(), minute)
                title = ""; time = ""
            }, enabled = title.isNotBlank() && weekdays.isNotEmpty() && (time.isBlank() || parseClock(time) != null)) {
                Text("افزودن روتین")
            }
        }
        Panel("روتین‌های من", Modifier.fillMaxWidth()) {
            if (data.routines.isEmpty()) EmptyText("هنوز روتینی ثبت نشده است.")
            data.routines.forEach { routine ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(routine.title, fontWeight = FontWeight.Medium)
                        Text(routine.weekdays.sorted().joinToString("، ") { labels[it] } +
                            (routine.minute?.let { " · ${DateFormats.clock(it)}" } ?: ""),
                            color = muted(), fontSize = 11.sp)
                    }
                    val doneToday = "${routine.id}|${today.toEpochDay()}" in data.routineChecks
                    Checkbox(checked = doneToday, onCheckedChange = { runtime.toggleRoutine(routine.id, today) })
                    TextButton(onClick = { runtime.deleteRoutine(routine.id) }) { Text("حذف", color = Color(0xFFE07B7B)) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))
            }
        }
    }
}

@Composable
private fun GoalsPage(data: AppData, runtime: DesktopRuntime) {
    var title by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(GoalPeriod.DAILY) }
    val today = java.time.LocalDate.now()
    val weekStart = JalaliDate.startOfWeek(today).toEpochDay()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Panel("هدف جدید", Modifier.fillMaxWidth()) {
        OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, label = { Text("عنوان هدف") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (period == GoalPeriod.DAILY) Button(onClick = { period = GoalPeriod.DAILY }) { Text("روزانه") }
            else OutlinedButton(onClick = { period = GoalPeriod.DAILY }) { Text("روزانه") }
            if (period == GoalPeriod.WEEKLY) Button(onClick = { period = GoalPeriod.WEEKLY }) { Text("هفتگی") }
            else OutlinedButton(onClick = { period = GoalPeriod.WEEKLY }) { Text("هفتگی") }
        }
        Button(onClick = {
            runtime.addGoal(title, period, if (period == GoalPeriod.DAILY) today.toEpochDay() else weekStart)
            title = ""
        }, enabled = title.isNotBlank()) { Text("ثبت هدف") }
    }
    Panel("هدف‌های ثبت‌شده", Modifier.fillMaxWidth()) {
        val goals = data.goals.filter { it.periodStartDay == if (it.period == GoalPeriod.DAILY) today.toEpochDay() else weekStart }
        if (goals.isEmpty()) EmptyText("برای این بازه هدفی ثبت نشده است.")
        goals.forEach { goal ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = goal.done, onCheckedChange = { runtime.toggleGoal(goal.id) })
                Text(goal.title, Modifier.weight(1f), color = if (goal.done) muted() else MaterialTheme.colorScheme.onSurface)
                Text(if (goal.period == GoalPeriod.DAILY) "روزانه" else "هفتگی", color = muted(), fontSize = 11.sp)
                TextButton(onClick = { runtime.deleteGoal(goal.id) }) { Text("حذف", color = Color(0xFFE07B7B)) }
            }
        }
        }
    }
}

@Composable
private fun PlanningPage(data: AppData) {
    var selectedDay by remember { mutableStateOf(java.time.LocalDate.now()) }
    var displayedMonth by remember { mutableStateOf(JalaliDate.of(selectedDay).withDay(1)) }
    val firstDay = displayedMonth.toLocalDate()
    val gridStart = JalaliDate.startOfWeek(firstDay)
    val dates = (0 until 42).map { gridStart.plusDays(it.toLong()) }
    val entries = Planner.entriesFor(data, selectedDay)
    val occasions = DesktopIranianCalendar.on(selectedDay)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel("تقویم جلالی", Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = {
                    displayedMonth = displayedMonth.plusMonths(-1)
                    selectedDay = displayedMonth.toLocalDate()
                }) { Text("ماه قبل") }
                Text(DateFormats.monthYear(displayedMonth), fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                OutlinedButton(onClick = {
                    displayedMonth = displayedMonth.plusMonths(1)
                    selectedDay = displayedMonth.toLocalDate()
                }) { Text("ماه بعد") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                JalaliDate.weekdayInitials.forEach { Text(it, color = muted(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
            }
            dates.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.forEach { date ->
                        val dateJalali = JalaliDate.of(date)
                        val inMonth = dateJalali.month == displayedMonth.month && dateJalali.year == displayedMonth.year
                        val active = date == selectedDay
                        val marked = Planner.countOn(data, date) > 0 || DesktopIranianCalendar.on(date).isNotEmpty()
                        Surface(
                            onClick = { selectedDay = date },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(fa(dateJalali.day), color = when {
                                    active -> MaterialTheme.colorScheme.onPrimary
                                    !inMonth -> muted().copy(alpha = .45f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                if (marked) Box(Modifier.size(4.dp).background(
                                    if (active) MaterialTheme.colorScheme.onPrimary else Accent, CircleShape
                                ))
                            }
                        }
                    }
                }
            }
            Text("مناسبت‌های آفلاین فعلاً برای سال‌های ${fa(DesktopIranianCalendar.supportedJalaliYears().joinToString("، "))} در دسترس‌اند.", color = muted(), fontSize = 11.sp)
        }
        Panel(DateFormats.full(selectedDay), Modifier.fillMaxWidth()) {
            if (occasions.isNotEmpty()) {
                occasions.forEach { event ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (event.isHoliday) "تعطیل" else "مناسبت", color = if (event.isHoliday) Color(0xFFE07B7B) else Accent, fontSize = 11.sp)
                        Text(event.titleFa, modifier = Modifier.weight(1f))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
            }
            if (entries.isEmpty()) EmptyText("برای این روز وظیفه یا روتینی ثبت نشده است.")
            else entries.forEach { entry ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = entry.done, onCheckedChange = null, enabled = false)
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, fontWeight = FontWeight.Medium)
                        Text(if (entry.done) "انجام شده" else "در انتظار انجام", color = muted(), fontSize = 12.sp)
                    }
                    entry.minute?.let { Text(DateFormats.clock(it), color = muted()) }
                }
            }
        }
    }
}

@Composable
private fun StatisticsPage(data: AppData) {
    var range by remember { mutableStateOf(StatsRange.WEEK) }
    val summary = Stats.summarize(data, range, java.time.LocalDate.now())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(StatsRange.DAY to "روز", StatsRange.WEEK to "هفته", StatsRange.MONTH to "ماه").forEach { (r, label) ->
                if (range == r) Button(onClick = { range = r }) { Text(label) }
                else OutlinedButton(onClick = { range = r }) { Text(label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricCard("زمان تمرکز", "${fa(summary.focusMinutes)} دقیقه", "در بازه‌ی انتخابی", Modifier.weight(1f))
            MetricCard("جلسه‌ی کامل", fa(summary.completedSessions), "جلسه", Modifier.weight(1f))
            MetricCard("روزهای فعال", fa(summary.activeDays), "روز", Modifier.weight(1f))
        }
        Panel("روند تمرکز", Modifier.fillMaxWidth()) {
            if (summary.buckets.all { it.minutes == 0L }) EmptyText("هنوز داده‌ی کافی برای نمودار وجود ندارد.")
            else summary.buckets.forEach { bucket ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(bucket.label, modifier = Modifier.width(48.dp), color = muted())
                    val max = (summary.buckets.maxOfOrNull { it.minutes } ?: 1L).coerceAtLeast(1L)
                    Box(Modifier.weight(1f).height(9.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                        Box(Modifier.fillMaxWidth((bucket.minutes.toFloat() / max).coerceIn(0f, 1f)).fillMaxHeight()
                            .background(Accent, RoundedCornerShape(8.dp)))
                    }
                    Text("${fa(bucket.minutes)} دقیقه", modifier = Modifier.width(82.dp), textAlign = TextAlign.End, fontSize = 11.sp)
                }
            }
        }
    }
}


@Composable
private fun MusicPage(runtime: DesktopRuntime) {
    val state by runtime.music.state.collectAsState()
    val downloads by runtime.music.downloads.collectAsState()
    val favorites by runtime.music.favorites.collectAsState()
    val playlists by runtime.music.playlists.collectAsState()
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel("پخش‌کننده‌ی موسیقی", Modifier.fillMaxWidth()) {
            val current = DesktopMusicCatalog.tracks.firstOrNull { it.id == state.trackId }
            Text(current?.title ?: "هنوز آهنگی پخش نشده", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text(current?.mood ?: "موسیقی بی‌کلام برای زمان تمرکز", color = muted())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { runtime.music.previous() }, enabled = current != null) { Text("قبلی") }
                Button(onClick = { runtime.music.togglePlayback() }, enabled = current != null) {
                    Text(if (state.playing) "مکث" else "ادامه")
                }
                OutlinedButton(onClick = { runtime.music.next() }, enabled = current != null) { Text("بعدی") }
                OutlinedButton(onClick = { runtime.music.stop() }, enabled = current != null) { Text("توقف") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { runtime.music.toggleShuffle() }) {
                    Text(if (state.shuffle) "تصادفی: روشن" else "تصادفی: خاموش")
                }
                OutlinedButton(onClick = { runtime.music.cycleRepeatMode() }) {
                    Text(when (state.repeatMode) { 1 -> "تکرار فهرست"; 2 -> "تکرار آهنگ"; else -> "تکرار: خاموش" })
                }
                Text("${fa(state.positionMs / 1000)} / ${fa(state.durationMs / 1000)} ثانیه", color = muted(), fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("صدا", color = muted())
                Slider(value = state.volume, onValueChange = { runtime.music.setVolume(it) }, modifier = Modifier.weight(1f))
                Text("${fa((state.volume * 100).toInt())}٪", color = muted(), fontSize = 12.sp)
            }
            if (!state.error.isNullOrBlank()) Text(state.error ?: "", color = Color(0xFFE07B7B), fontSize = 12.sp)
        }
        Panel("پلی‌لیست‌های من", Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it },
                    modifier = Modifier.weight(1f), singleLine = true, label = { Text("نام پلی‌لیست") })
                Button(onClick = {
                    selectedPlaylistId = runtime.music.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                }, enabled = newPlaylistName.isNotBlank()) { Text("ساخت") }
            }
            if (playlists.isEmpty()) EmptyText("هنوز پلی‌لیستی نساخته‌ای.")
            playlists.forEach { playlist ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(playlist.name, fontWeight = FontWeight.Medium)
                        Text("${fa(playlist.trackIds.size)} آهنگ", color = muted(), fontSize = 11.sp)
                    }
                    if (selectedPlaylistId == playlist.id) Button(onClick = { selectedPlaylistId = null }) { Text("انتخاب‌شده") }
                    else OutlinedButton(onClick = { selectedPlaylistId = playlist.id }) { Text("انتخاب") }
                    TextButton(onClick = { runtime.music.playPlaylist(playlist.id) }, enabled = playlist.trackIds.isNotEmpty()) { Text("پخش") }
                    TextButton(onClick = {
                        runtime.music.deletePlaylist(playlist.id)
                        if (selectedPlaylistId == playlist.id) selectedPlaylistId = null
                    }) { Text("حذف", color = Color(0xFFE07B7B)) }
                }
            }
            val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }
            if (selectedPlaylist != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                Text("افزودن آهنگ به «${selectedPlaylist.name}»", color = muted(), fontSize = 12.sp)
                selectedPlaylist.trackIds.forEach { id ->
                    val track = DesktopMusicCatalog.tracks.firstOrNull { it.id == id } ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(track.title, Modifier.weight(1f))
                        TextButton(onClick = { runtime.music.removeFromPlaylist(selectedPlaylist.id, id) }) { Text("حذف از فهرست") }
                    }
                }
            }
        }
        Panel("کتابخانه‌ی آهنگ‌ها", Modifier.fillMaxWidth()) {
            Text("فهرست سازگار با نسخه‌ی اندروید، با مجوز CC0 1.0. برای پخش، آهنگ ابتدا به‌صورت امن دانلود و سپس به‌صورت محلی اجرا می‌شود.", color = muted(), fontSize = 12.sp)
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("جست‌وجوی آهنگ") })
            val tracks = DesktopMusicCatalog.tracks.filter { query.isBlank() ||
                it.title.contains(query, true) || it.mood.contains(query, true) }
            tracks.forEach { track ->
                val downloaded = runtime.music.isDownloaded(track)
                val progress = downloads[track.id]
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(track.title, fontWeight = FontWeight.Medium)
                        Text("${track.mood} · ${track.durationLabel} · ${DesktopMusicCatalog.artist}", color = muted(), fontSize = 11.sp)
                        if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        if (state.trackId == track.id) Text(if (state.playing) "در حال پخش" else "آماده‌ی ادامه", color = Accent, fontSize = 11.sp)
                    }
                    TextButton(onClick = { runtime.music.toggleFavorite(track.id) }) {
                        Text(if (track.id in favorites) "★" else "☆", color = if (track.id in favorites) Accent else muted(), fontSize = 20.sp)
                    }
                    if (selectedPlaylistId != null) {
                        val playlist = playlists.firstOrNull { it.id == selectedPlaylistId }
                        if (playlist != null) {
                            TextButton(onClick = { runtime.music.addToPlaylist(playlist.id, track.id) },
                                enabled = track.id !in playlist.trackIds) { Text(if (track.id in playlist.trackIds) "افزوده شد" else "+ فهرست") }
                        }
                    }
                    if (downloaded) {
                        Button(onClick = { runtime.music.play(track) }, enabled = state.trackId != track.id || !state.playing) { Text(if (state.trackId == track.id && state.playing) "در حال پخش" else "پخش") }
                    } else {
                        OutlinedButton(onClick = { runtime.music.download(track) }, enabled = progress == null) {
                            Text(if (progress == null) "دانلود" else "${fa((progress * 100).toInt())}٪")
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))
            }
            Text("فایل‌های دانلودشده در پوشه‌ی AppData/Roaming/MedFlow/music نگهداری می‌شوند.", color = muted(), fontSize = 11.sp)
            Text("منبع موسیقی: ${DesktopMusicCatalog.tracks.firstOrNull()?.sourceUrl ?: ""}", color = muted(), fontSize = 10.sp)
        }
    }
}

@Composable
private fun SettingsPage(data: AppData, runtime: DesktopRuntime, dark: Boolean, onThemeChange: () -> Unit) {
    var startupEnabled by remember { mutableStateOf(WindowsStartup.isEnabled()) }
    var startupMessage by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Panel("ظاهر و داده‌ها", Modifier.fillMaxWidth()) {
        Text("پوسته‌ی برنامه", fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onThemeChange) { Text(if (dark) "تغییر به روشن" else "تغییر به تیره") }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = data.settings.animations,
                onCheckedChange = { enabled ->
                    runtime.updateSettings { it.copy(animations = enabled) }
                }
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("انیمیشن‌های رابط کاربری")
                Text("برای کاهش حرکت، ترنزیشن‌ها را خاموش کن", color = muted(), fontSize = 11.sp)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        Text("پس‌زمینه", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("gradient" to "گرادیان", "aurora" to "شفق قطبی", "midnight" to "نیمه‌شب", "dusk" to "غروب", "mica" to "Mica ویندوز").forEach { (id, label) ->
                val supported = id != "mica" || WindowsBackdrop.isSupported()
                if (data.settings.wallpaperId == id && data.settings.wallpaperPath == null) Button(onClick = {
                    runtime.updateSettings { it.copy(wallpaperId = id, wallpaperPath = null) }
                }, enabled = supported) { Text(label) }
                else OutlinedButton(onClick = { runtime.updateSettings { it.copy(wallpaperId = id, wallpaperPath = null) } }, enabled = supported) { Text(label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { chooseWallpaper(runtime) }) { Text("انتخاب تصویر شخصی") }
            if (data.settings.wallpaperPath != null) OutlinedButton(onClick = {
                runtime.updateSettings { it.copy(wallpaperId = "gradient", wallpaperPath = null) }
            }) { Text("حذف تصویر شخصی") }
        }
        Text("تیره‌سازی پس‌زمینه", color = muted(), fontSize = 12.sp)
        Slider(value = data.settings.wallpaperDim, onValueChange = { v ->
            runtime.updateSettings { it.copy(wallpaperDim = v.coerceIn(0f, .65f)) }
        }, valueRange = 0f..0.65f)
        Text("کیفیت افکت شیشه‌ای", color = muted(), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(
                GlassQuality.LOW to "عملکرد",
                GlassQuality.MEDIUM to "متعادل",
                GlassQuality.HIGH to "کیفیت",
                GlassQuality.ULTRA to "اولترا"
            ).forEach { (quality, label) ->
                val selected = data.settings.glassQuality == quality
                if (selected) Button(onClick = {
                    runtime.updateSettings {
                        it.copy(
                            glassQuality = quality,
                            glassBlurDp = when (quality) {
                                GlassQuality.LOW -> 0
                                GlassQuality.MEDIUM -> 12
                                GlassQuality.HIGH -> 20
                                GlassQuality.ULTRA -> 28
                            },
                            glassShine = when (quality) {
                                GlassQuality.LOW -> .12f
                                GlassQuality.MEDIUM -> .42f
                                GlassQuality.HIGH -> .72f
                                GlassQuality.ULTRA -> .92f
                            }
                        )
                    }
                }) { Text(label) }
                else OutlinedButton(onClick = {
                    runtime.updateSettings {
                        it.copy(
                            glassQuality = quality,
                            glassBlurDp = when (quality) {
                                GlassQuality.LOW -> 0
                                GlassQuality.MEDIUM -> 12
                                GlassQuality.HIGH -> 20
                                GlassQuality.ULTRA -> 28
                            },
                            glassShine = when (quality) {
                                GlassQuality.LOW -> .12f
                                GlassQuality.MEDIUM -> .42f
                                GlassQuality.HIGH -> .72f
                                GlassQuality.ULTRA -> .92f
                            }
                        )
                    }
                }) { Text(label) }
            }
        }
        Text("شفافیت پنل‌ها", color = muted(), fontSize = 12.sp)
        Slider(value = data.settings.glassOpacity, onValueChange = { v ->
            runtime.updateSettings { it.copy(glassOpacity = v.coerceIn(.55f, 1f)) }
        }, valueRange = .55f..1f)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        Text("رفتار ویندوز", fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = startupEnabled,
                enabled = WindowsStartup.isSupported() && (!startupEnabled || WindowsStartup.executablePath() != null),
                onCheckedChange = { enabled ->
                    val success = WindowsStartup.setEnabled(enabled)
                    startupEnabled = WindowsStartup.isEnabled()
                    startupMessage = if (success) "تنظیم شروع خودکار ذخیره شد." else "این گزینه فقط در نسخه‌ی نصب‌شده‌ی ویندوز قابل فعال‌سازی است."
                }
            )
            Column(Modifier.weight(1f)) {
                Text("اجرای خودکار هنگام ورود به ویندوز")
                Text("فقط برای حساب کاربری فعلی ویندوز", color = muted(), fontSize = 11.sp)
            }
        }
        if (startupMessage.isNotBlank()) Text(startupMessage, color = muted(), fontSize = 11.sp)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        Text("پشتیبان‌گیری", fontWeight = FontWeight.SemiBold)
        Text("فایل پشتیبان شامل وظایف، تنظیمات، جلسات و وضعیت تایمر است.", color = muted(), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { exportBackup(runtime) }) { Text("خروجی JSON") }
            OutlinedButton(onClick = { importBackup(runtime) }) { Text("بازیابی JSON") }
        }
        Text("ذخیره‌سازی خودکار در پوشه‌ی اختصاصی MedFlow در پروفایل ویندوز انجام می‌شود.", color = muted(), fontSize = 12.sp)
        Text("تعداد وظایف: ${fa(data.tasks.size)}  •  جلسات: ${fa(data.sessions.size)}", color = muted(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlaceholderPage(title: String, message: String) {
    Panel(title, Modifier.fillMaxWidth().height(240.dp)) { Text(message, color = muted()) }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Panel(title, modifier.height(132.dp)) {
        Text(value, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = muted(), fontSize = 12.sp)
    }
}

@Composable
private fun Panel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val settings = LocalDesktopSettings.current
    val quality = settings.glassQuality
    val surfaceAlpha = when (quality) {
        GlassQuality.LOW -> .97f
        GlassQuality.MEDIUM -> .93f
        GlassQuality.HIGH -> .88f
        GlassQuality.ULTRA -> .82f
    }
    val edgeAlpha = when (quality) {
        GlassQuality.LOW -> .035f
        GlassQuality.MEDIUM -> .06f
        GlassQuality.HIGH -> .095f
        GlassQuality.ULTRA -> .14f
    }
    val depth = when (quality) {
        GlassQuality.LOW -> 2.dp
        GlassQuality.MEDIUM -> 5.dp
        GlassQuality.HIGH -> 9.dp
        GlassQuality.ULTRA -> 13.dp
    }
    val opacity = (surfaceAlpha * (.72f + settings.glassOpacity.coerceIn(.55f, 1f) * .28f)).coerceIn(.72f, .98f)
    val shape = RoundedCornerShape(MedFlowDesign.panelRadius)
    val darkSurface = MaterialTheme.colorScheme.background == MedFlowDesign.darkBackground
    val edgeHighlight = if (darkSurface) Color.White else Color(0xFF9EADBF)
    val edge = Brush.linearGradient(
        listOf(
            edgeHighlight.copy(
                alpha = (edgeAlpha + settings.glassShine.coerceIn(0f, 1f) * .075f).coerceAtMost(.22f)
            ),
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (darkSurface) .025f else .018f),
            MaterialTheme.colorScheme.primary.copy(alpha = edgeAlpha * if (darkSurface) 1f else .68f)
        )
    )

    Card(
        modifier = modifier
            .then(if (settings.animations) Modifier.animateContentSize() else Modifier)
            .shadow(depth, shape)
            .border(1.dp, edge, shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = opacity)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(
                horizontal = MedFlowDesign.panelPaddingHorizontal,
                vertical = MedFlowDesign.panelPaddingVertical
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = .05.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.width(22.dp).height(3.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .72f), RoundedCornerShape(8.dp))
                )
            }
            content(this)
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(text, color = muted(), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
}

@Composable
private fun muted(): Color =
    if (MaterialTheme.colorScheme.background == MedFlowDesign.darkBackground) MedFlowDesign.darkMuted else MedFlowDesign.lightMuted


private fun normalizeDigits(input: String): String = input.map { ch ->
    when (ch) {
        in '۰'..'۹' -> '0' + (ch - '۰')
        in '٠'..'٩' -> '0' + (ch - '٠')
        else -> ch
    }
}.joinToString("")

private fun parseJalaliDate(input: String): java.time.LocalDate? {
    val parts = normalizeDigits(input).trim().split('/', '-', '.')
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (year !in 1200..1600 || month !in 1..12 || day !in 1..JalaliDate.monthLength(year, month)) return null
    return runCatching { JalaliDate(year, month, day).toLocalDate() }.getOrNull()
}

private fun parseClock(input: String): Int? {
    val parts = normalizeDigits(input).trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun timerText(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0L)
    return fa("%02d:%02d".format(seconds / 60, seconds % 60))
}

private fun fa(value: Any): String = value.toString().map { ch ->
    if (ch in '0'..'9') ('۰'.code + (ch.code - '0'.code)).toChar() else ch
}.joinToString("")


private fun chooseWallpaper(runtime: DesktopRuntime) {
    val chooser = JFileChooser().apply {
        dialogTitle = "انتخاب تصویر پس‌زمینه"
        fileFilter = FileNameExtensionFilter("Images (PNG, JPG, WEBP)", "png", "jpg", "jpeg", "webp")
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val source = chooser.selectedFile
        if (source.extension.lowercase() !in setOf("png", "jpg", "jpeg", "webp")) return
        val destination = File(appDataDirectory(), "wallpaper.${source.extension.lowercase()}")
        runCatching {
            source.copyTo(destination, overwrite = true)
            runtime.updateSettings { it.copy(wallpaperId = "custom", wallpaperPath = destination.absolutePath) }
        }
    }
}

private fun exportBackup(runtime: DesktopRuntime) {
    val chooser = JFileChooser().apply {
        dialogTitle = "ذخیره‌ی پشتیبان MedFlow"
        selectedFile = File("MedFlow-backup.json")
        fileFilter = FileNameExtensionFilter("JSON", "json")
    }
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        var file = chooser.selectedFile
        if (!file.name.endsWith(".json", ignoreCase = true)) file = File(file.parentFile, file.name + ".json")
        file.writeText(runtime.exportJson(), Charsets.UTF_8)
    }
}

private fun importBackup(runtime: DesktopRuntime) {
    val chooser = JFileChooser().apply {
        dialogTitle = "انتخاب پشتیبان MedFlow"
        fileFilter = FileNameExtensionFilter("JSON", "json")
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        runCatching { runtime.importJson(chooser.selectedFile.readText(Charsets.UTF_8)) }
    }
}
