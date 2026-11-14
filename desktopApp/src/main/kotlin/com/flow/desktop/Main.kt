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
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
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

private val Accent = Color(0xFF91A4FF)
private val MutedDark = Color(0xFFA5AEC0)
private val MutedLight = Color(0xFF667085)
private val LocalDesktopSettings = androidx.compose.runtime.staticCompositionLocalOf { com.flow.focus.data.Settings() }

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
        MedFlowDesktop(runtime)
    }
}

@Composable
private fun MedFlowDesktop(runtime: DesktopRuntime) {
    var selected by remember { mutableStateOf("خانه") }
    val data by runtime.data.collectAsState()
    val dark = data.settings.theme != ThemeMode.LIGHT
    val now by runtime.now.collectAsState()
    val scheme = if (dark) darkColorScheme(
        primary = Accent, background = Color(0xFF10131B), surface = Color(0xFF1A1F2B),
        surfaceVariant = Color(0xFF242B39), onBackground = Color(0xFFF4F6FB),
        onSurface = Color(0xFFF4F6FB), onSurfaceVariant = Color(0xFFB8C0D0)
    ) else lightColorScheme(
        primary = Color(0xFF5268D8), background = Color(0xFFF2F4F8), surface = Color.White,
        surfaceVariant = Color(0xFFE9EDF5), onBackground = Color(0xFF202635),
        onSurface = Color(0xFF202635), onSurfaceVariant = Color(0xFF667085)
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme) {
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
                        if (wallpaper != null) {
                            val scale = if (data.settings.wallpaperFit == WallpaperFit.CROP)
                                maxOf(size.width / wallpaper.width, size.height / wallpaper.height)
                            else minOf(size.width / wallpaper.width, size.height / wallpaper.height)
                            val w = (wallpaper.width * scale).toInt().coerceAtLeast(1)
                            val h = (wallpaper.height * scale).toInt().coerceAtLeast(1)
                            drawImage(wallpaper, dstOffset = IntOffset((size.width.toInt() - w) / 2, (size.height.toInt() - h) / 2), dstSize = IntSize(w, h))
                        } else {
                            val colors = when (data.settings.wallpaperId) {
                                "aurora" -> listOf(Color(0xFF07172B), Color(0xFF173B59), Color(0xFF382B65))
                                "midnight" -> listOf(Color(0xFF070D1B), Color(0xFF172A42), Color(0xFF26364B))
                                "dusk" -> listOf(Color(0xFF241B37), Color(0xFF69435E), Color(0xFF273D5C))
                                else -> if (dark) listOf(Color(0xFF111722), Color(0xFF171D2B), Color(0xFF111722))
                                    else listOf(Color(0xFFF4F6FA), Color(0xFFE9EDF5), Color(0xFFF7F8FB))
                            }
                            drawRect(Brush.linearGradient(colors))
                        }
                        if (data.settings.wallpaperDim > 0f) drawRect(Color.Black.copy(alpha = data.settings.wallpaperDim.coerceIn(0f, .65f)))
                    }
                    Box(Modifier.fillMaxSize().padding(18.dp)) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Sidebar(selected, { selected = it }, dark, { runtime.updateSettings { it.copy(theme = if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } })
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(selected, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text(DateFormats.full(java.time.LocalDate.now()), color = muted(), fontSize = 13.sp)
                            }
                            Text("فضای شخصی مدیریت زمان", color = muted(), fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Crossfade(targetState = selected, label = "desktop-page-transition") { page ->
                            when (page) {
                                "خانه" -> Dashboard(data, now, runtime)
                                "تمرکز" -> FocusPage(data, now, runtime)
                                "وظایف" -> TasksPage(data, runtime)
                                "روتین‌ها" -> RoutinesPage(data, runtime)
                                "اهداف" -> GoalsPage(data, runtime)
                                "برنامه‌ریزی" -> PlanningPage(data)
                                "آمار" -> StatisticsPage(data)
                                "موسیقی" -> MusicPage(runtime)
                                "تنظیمات" -> SettingsPage(data, runtime, dark, { runtime.updateSettings { it.copy(theme = if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } })
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
private fun Sidebar(selected: String, onSelect: (String) -> Unit, dark: Boolean, onThemeChange: () -> Unit) {
    val items = listOf("خانه", "تمرکز", "وظایف", "روتین‌ها", "اهداف", "برنامه‌ریزی", "آمار", "موسیقی", "تنظیمات")
    Column(Modifier.width(224.dp).fillMaxHeight()
        .background(MaterialTheme.colorScheme.surface.copy(alpha = .92f), RoundedCornerShape(26.dp))
        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .07f), RoundedCornerShape(26.dp))
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Accent.copy(alpha = .20f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center) { Text("M", color = Accent, fontWeight = FontWeight.Bold, fontSize = 21.sp) }
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("MedFlow", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("DESKTOP", color = muted(), fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        items.forEach { item ->
            val active = selected == item
            Surface(onClick = { onSelect(item) },
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else Color.Transparent,
                shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(item, Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        TextButton(onClick = onThemeChange, modifier = Modifier.fillMaxWidth()) {
            Text(if (dark) "☀  حالت روشن" else "☾  حالت تیره", color = muted())
        }
        Text("نسخه‌ی دسکتاپ", color = muted(), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun Dashboard(data: AppData, now: Long, runtime: DesktopRuntime) {
    val stats = Stats.summarize(data, StatsRange.DAY, java.time.LocalDate.now())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricCard("تمرکز امروز", "${fa(stats.focusMinutes)} دقیقه", "بر اساس جلسات ثبت‌شده", Modifier.weight(1f))
            MetricCard("وظایف باز", fa(data.tasks.count { !it.done }), "کارهای باقی‌مانده", Modifier.weight(1f))
            MetricCard("جلسه‌های کامل", fa(stats.completedSessions), "امروز", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FocusCard(data, now, runtime, Modifier.weight(1.35f).height(390.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel("برنامه‌ی امروز", Modifier.fillMaxWidth()) {
                    val entries = Planner.entriesFor(data, java.time.LocalDate.now())
                    if (entries.isEmpty()) EmptyText("برای امروز برنامه‌ای ثبت نشده است.")
                    else entries.take(5).forEach { entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (entry.done) "✓" else "•", color = Accent, modifier = Modifier.width(24.dp))
                            Text(entry.title, modifier = Modifier.weight(1f), maxLines = 1)
                            entry.minute?.let { Text(DateFormats.clock(it), color = muted()) }
                        }
                    }
                }
                Panel("کارهای بعدی", Modifier.fillMaxWidth()) {
                    val tasks = data.tasks.filterNot { it.done }.take(4)
                    if (tasks.isEmpty()) EmptyText("فعلاً وظیفه‌ی بازی وجود ندارد.")
                    else tasks.forEach { Text("•  ${it.title}") }
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
            Text(timerText(remaining), fontSize = 64.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(.78f).height(7.dp),
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { runtime.music.togglePlayback() }, enabled = current != null) {
                    Text(if (state.playing) "مکث" else "ادامه")
                }
                OutlinedButton(onClick = { runtime.music.stop() }, enabled = current != null) { Text("توقف") }
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
    Panel("ظاهر و داده‌ها", Modifier.fillMaxWidth()) {
        Text("پوسته‌ی برنامه", fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onThemeChange) { Text(if (dark) "تغییر به روشن" else "تغییر به تیره") }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        Text("پس‌زمینه", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("gradient" to "گرادیان", "aurora" to "شفق قطبی", "midnight" to "نیمه‌شب", "dusk" to "غروب").forEach { (id, label) ->
                if (data.settings.wallpaperId == id && data.settings.wallpaperPath == null) Button(onClick = {
                    runtime.updateSettings { it.copy(wallpaperId = id, wallpaperPath = null) }
                }) { Text(label) }
                else OutlinedButton(onClick = { runtime.updateSettings { it.copy(wallpaperId = id, wallpaperPath = null) } }) { Text(label) }
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
    Card(modifier = modifier.animateContentSize().border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .18f), MaterialTheme.colorScheme.onSurface.copy(alpha = .035f), Color.White.copy(alpha = .08f))), RoundedCornerShape(23.dp)), shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = LocalDesktopSettings.current.glassOpacity.coerceIn(.55f, 1f) * .91f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
    if (MaterialTheme.colorScheme.background == Color(0xFF10131B)) MutedDark else MutedLight


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
