package com.flow.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flow.desktop.DesktopRuntime
import com.flow.desktop.WindowsBackdrop
import com.flow.desktop.WindowsStartup
import com.flow.desktop.appDataDirectory
import com.flow.desktop.design.*
import com.flow.focus.data.AppData
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.Settings
import com.flow.focus.data.ThemeMode
import com.flow.focus.data.WallpaperFit
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private enum class SettingsTab(val title: String, val icon: FlowIcon) {
    Appearance("ظاهر و متریال", FlowIcon.Palette),
    Timer("تایمر و اعلان‌ها", FlowIcon.Timer),
    System("ویندوز و داده‌ها", FlowIcon.Database),
}

@Composable
internal fun SettingsPage(data: AppData, runtime: DesktopRuntime) {
    var tab by remember { mutableStateOf(SettingsTab.Appearance) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
        Segmented(SettingsTab.entries.map { it to it.title }, tab, { tab = it })
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FlowDims.gap)) {
            when (tab) {
                SettingsTab.Appearance -> AppearanceTab(data, runtime)
                SettingsTab.Timer -> TimerTab(data, runtime)
                SettingsTab.System -> SystemTab(data, runtime)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, hint: String? = null, modifier: Modifier = Modifier, control: @Composable () -> Unit) {
    val pal = Flow.colors
    Row(modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            FlowText(title, style = FlowType.bodyMedium)
            if (hint != null) FlowText(hint, style = FlowType.caption, color = pal.textSecondary)
        }
        control()
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit, suffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowIconButton(FlowIcon.Minus, "کمتر", { onChange((value - 1).coerceIn(range)) }, size = 30.dp, iconSize = 15.dp, enabled = value > range.first)
        FlowText(num(value) + suffix, Modifier.widthIn(min = 64.dp), FlowType.headline, textAlign = TextAlign.Center, maxLines = 1)
        FlowIconButton(FlowIcon.Plus, "بیشتر", { onChange((value + 1).coerceIn(range)) }, size = 30.dp, iconSize = 15.dp, enabled = value < range.last)
    }
}

// ───────────────────────── Appearance ─────────────────────────

@Composable
private fun AppearanceTab(data: AppData, runtime: DesktopRuntime) {
    val pal = Flow.colors
    val st = data.settings
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 6.dp) {
        PanelHeader("ظاهر", FlowIcon.Palette)
        SettingRow("پوسته", "با «سیستم»، برنامه از حالت روشن/تیره‌ی ویندوز پیروی می‌کند.") {
            Segmented(listOf(ThemeMode.SYSTEM to "سیستم", ThemeMode.LIGHT to "روشن", ThemeMode.DARK to "تیره"), st.theme, { m -> runtime.updateSettings { it.copy(theme = m) } })
        }
        Hairline()
        SettingRow("اعداد فارسی", "نمایش ۰۱۲۳ به‌جای 0123 در همه‌ی صفحه‌ها") {
            FlowSwitch(st.persianDigits, { v -> runtime.updateSettings { it.copy(persianDigits = v) } })
        }
        Hairline()
        SettingRow("انیمیشن‌های رابط", "خاموش کردن = کاهش حرکت: همه‌ی انتقال‌ها فوری می‌شوند.") {
            FlowSwitch(st.animations, { v -> runtime.updateSettings { it.copy(animations = v) } })
        }
    }
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 12.dp) {
        PanelHeader("کیفیت متریال شیشه‌ای", FlowIcon.Layers)
        FlowText(
            "تفاوت‌ها واقعی‌اند: بلور از روی تصویر پس‌زمینه یک‌بار ساخته و کش می‌شود (نه بلور لحظه‌ای تمام‌صفحه)؛ سطح بالاتر یعنی بلور بزرگ‌تر با وضوح بیشتر و لایه‌های نور بیشتر.",
            style = FlowType.caption, color = pal.textSecondary,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val perRow = if (maxWidth > 900.dp) 4 else 2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassQuality.entries.chunked(perRow).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { q -> QualityCard(q, st.glassQuality == q, Modifier.weight(1f)) { setQuality(runtime, q) } }
                        repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        SettingRow("شفافیت پنل‌ها", "کمتر = شیشه‌ای‌تر، بیشتر = مات‌تر") {
            Row(Modifier.width(240.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowSlider(st.glassOpacity, { v -> runtime.updateSettings { it.copy(glassOpacity = v.coerceIn(.55f, 1f)) } }, Modifier.weight(1f), range = .55f..1f)
                FlowText(num(((st.glassOpacity - .55f) / .45f * 100).toInt()) + "٪", Modifier.width(38.dp), FlowType.caption, pal.textSecondary)
            }
        }
    }
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 12.dp) {
        PanelHeader("پس‌زمینه", FlowIcon.Image)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WallpaperArtIds.forEach { (id, label) ->
                WallpaperTile(id, label, st.wallpaperPath == null && st.wallpaperId == id, Modifier.weight(1f)) {
                    runtime.updateSettings { it.copy(wallpaperId = id, wallpaperPath = null) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FlowButton("انتخاب تصویر شخصی", { chooseWallpaper(runtime) }, icon = FlowIcon.Image)
            if (st.wallpaperPath != null) FlowButton("حذف تصویر شخصی", { runtime.updateSettings { it.copy(wallpaperId = "gradient", wallpaperPath = null) } }, kind = ButtonKind.Ghost, icon = FlowIcon.Close)
            Spacer(Modifier.weight(1f))
            if (st.wallpaperPath != null) Segmented(listOf(WallpaperFit.CROP to "پر کردن", WallpaperFit.FIT to "جا دادن"), st.wallpaperFit, { f -> runtime.updateSettings { it.copy(wallpaperFit = f) } })
        }
        SettingRow("تیره‌سازی پس‌زمینه") {
            Row(Modifier.width(240.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowSlider(st.wallpaperDim, { v -> runtime.updateSettings { it.copy(wallpaperDim = v.coerceIn(0f, .65f)) } }, Modifier.weight(1f), range = 0f..0.65f)
                FlowText(num((st.wallpaperDim / .65f * 100).toInt()) + "٪", Modifier.width(38.dp), FlowType.caption, pal.textSecondary)
            }
        }
        SettingRow("محو کردن پس‌زمینه", "پیش از ساخت شیشه، خود تصویر را نرم می‌کند") {
            Row(Modifier.width(240.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowSlider(st.wallpaperBlurDp.toFloat(), { v -> runtime.updateSettings { it.copy(wallpaperBlurDp = v.toInt().coerceIn(0, 24)) } }, Modifier.weight(1f), range = 0f..24f)
                FlowText(num(st.wallpaperBlurDp), Modifier.width(38.dp), FlowType.caption, pal.textSecondary)
            }
        }
    }
}

private val WallpaperArtIds = listOf("gradient" to "FLOW", "aurora" to "شفق", "midnight" to "نیمه‌شب", "dusk" to "غروب")

private fun setQuality(runtime: DesktopRuntime, q: GlassQuality) = runtime.updateSettings {
    it.copy(
        glassQuality = q,
        glassBlurDp = when (q) { GlassQuality.LOW -> 0; GlassQuality.MEDIUM -> 8; GlassQuality.HIGH -> 16; GlassQuality.ULTRA -> 24 },
        glassShine = when (q) { GlassQuality.LOW -> .12f; GlassQuality.MEDIUM -> .38f; GlassQuality.HIGH -> .68f; GlassQuality.ULTRA -> .9f },
    )
}

@Composable
private fun QualityCard(q: GlassQuality, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val pal = Flow.colors
    val spec = GlassProfiles.of(q)
    val ph = rememberPressHover()
    val (name, desc) = when (q) {
        GlassQuality.LOW -> "عملکرد" to "سطح ساده و مات؛ کمترین مصرف CPU/GPU"
        GlassQuality.MEDIUM -> "متعادل" to "بلور نرم و لبه‌ی ظریف"
        GlassQuality.HIGH -> "کیفیت" to "بلور قوی‌تر، درخشش لبه و سایه‌ی عمیق"
        GlassQuality.ULTRA -> "اولترا" to "بالاترین وضوح بلور، دانه‌ی فیلم و بازتاب"
    }
    Column(
        modifier.clip(RoundedCornerShape(FlowDims.cardRadius + 2.dp))
            .background(if (selected) pal.accent.copy(alpha = .13f) else if (ph.hovered) pal.fillHover else pal.fillSubtle)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) pal.accent else pal.hairline, RoundedCornerShape(FlowDims.cardRadius + 2.dp))
            .flowClickable(ph, true, onClick).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowText(name, Modifier.weight(1f), FlowType.title2, maxLines = 1)
            if (selected) FlowIconView(FlowIcon.CheckCircle, pal.accent, size = 18.dp)
        }
        FlowText(desc, style = FlowType.caption, color = pal.textSecondary, maxLines = 3)
        Hairline()
        Metric("بلور", if (spec.samplesBackdrop) "≈ ${num(spec.effectiveBlurPx.toInt())} px" else "ندارد")
        Metric("وضوح نمونه", if (spec.samplesBackdrop) "${num((spec.bakeScale * 100).toInt())}٪" else "—")
        Metric("لایه‌ی رسم", num(spec.passesPerSurface))
    }
}

@Composable
private fun Metric(label: String, value: String) {
    val pal = Flow.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        FlowText(label, style = FlowType.caption, color = pal.textTertiary)
        FlowText(value, style = FlowType.captionMedium, color = pal.textSecondary)
    }
}

@Composable
private fun WallpaperTile(id: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val pal = Flow.colors
    val ph = rememberPressHover()
    val shape = RoundedCornerShape(FlowDims.cardRadius)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(78.dp).clip(shape)
                .border(if (selected) 2.dp else 1.dp, if (selected) pal.accent else pal.hairline, shape)
                .flowClickable(ph, true, onClick),
        ) { WallpaperArt.draw(this, id, pal.dark) }
        FlowText(label, Modifier.fillMaxWidth(), FlowType.captionMedium, if (selected) pal.accent else pal.textSecondary, textAlign = TextAlign.Center)
    }
}

// ───────────────────────── Timer ─────────────────────────

@Composable
private fun TimerTab(data: AppData, runtime: DesktopRuntime) {
    val st = data.settings
    fun upd(block: (Settings) -> Settings) = runtime.updateSettingsSyncTimer(block)
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 6.dp) {
        PanelHeader("مدت‌ها", FlowIcon.Timer)
        SettingRow("تمرکز") { Stepper(st.focusMin, 1..180, { v -> upd { it.copy(focusMin = v) } }, " دقیقه") }
        Hairline()
        SettingRow("استراحت کوتاه") { Stepper(st.shortBreakMin, 1..60, { v -> upd { it.copy(shortBreakMin = v) } }, " دقیقه") }
        Hairline()
        SettingRow("استراحت بلند") { Stepper(st.longBreakMin, 1..90, { v -> upd { it.copy(longBreakMin = v) } }, " دقیقه") }
        Hairline()
        SettingRow("دورهای تمرکز تا استراحت بلند") { Stepper(st.rounds, 1..12, { v -> upd { it.copy(rounds = v) } }) }
        Hairline()
        SettingRow("هدف روزانه‌ی تمرکز") { Stepper(st.dailyGoalMin, 15..720, { v -> upd { it.copy(dailyGoalMin = (v / 5) * 5) } }, " دقیقه") }
    }
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 6.dp) {
        PanelHeader("رفتار", FlowIcon.Bell)
        SettingRow("شروع خودکار استراحت", "پس از پایان تمرکز، استراحت بدون کلیک شروع می‌شود") { FlowSwitch(st.autoStartBreak, { v -> upd { it.copy(autoStartBreak = v) } }) }
        Hairline()
        SettingRow("شروع خودکار تمرکز", "پس از پایان استراحت، تمرکز بعدی شروع می‌شود") { FlowSwitch(st.autoStartFocus, { v -> upd { it.copy(autoStartFocus = v) } }) }
        Hairline()
        SettingRow("اعلان پایان جلسه", "از طریق Tray ویندوز نمایش داده می‌شود") { FlowSwitch(st.notifications, { v -> upd { it.copy(notifications = v) } }) }
    }
}

// ───────────────────────── Windows & data ─────────────────────────

@Composable
private fun SystemTab(data: AppData, runtime: DesktopRuntime) {
    val pal = Flow.colors
    var startup by remember { mutableStateOf(WindowsStartup.isEnabled()) }
    var message by remember { mutableStateOf("") }
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 6.dp) {
        PanelHeader("ویندوز", FlowIcon.Power)
        SettingRow("اجرای خودکار هنگام ورود به ویندوز", "فقط برای حساب کاربری فعلی؛ فقط در نسخه‌ی نصب‌شده فعال می‌شود") {
            FlowSwitch(startup, { v ->
                val ok = WindowsStartup.isSupported() && (!v || WindowsStartup.executablePath() != null) && WindowsStartup.setEnabled(v)
                startup = WindowsStartup.isEnabled()
                message = if (ok) "تنظیم شروع خودکار ذخیره شد." else "این گزینه فقط در نسخه‌ی نصب‌شده‌ی ویندوز قابل فعال‌سازی است."
            })
        }
        Hairline()
        SettingRow("وضعیت پنجره", if (WindowsBackdrop.isSupported()) "رنگ نوار عنوان و گوشه‌های پنجره با پوسته هماهنگ می‌شود (Windows 11)" else "این قابلیت فقط در ویندوز در دسترس است") {}
        if (message.isNotBlank()) FlowText(message, style = FlowType.caption, color = pal.textSecondary)
    }
    GlassPanel(Modifier.fillMaxWidth(), verticalSpacing = 10.dp) {
        PanelHeader("پشتیبان‌گیری", FlowIcon.Database)
        FlowText("فایل پشتیبان شامل وظایف، روتین‌ها، هدف‌ها، جلسه‌ها، تنظیمات و وضعیت تایمر است. بازیابی، داده‌ی فعلی را با محتوای فایل جایگزین می‌کند.", style = FlowType.caption, color = pal.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowButton("خروجی JSON", { message = exportBackup(runtime) }, kind = ButtonKind.Primary, icon = FlowIcon.Upload)
            FlowButton("بازیابی JSON", { message = importBackup(runtime) }, icon = FlowIcon.Download)
        }
        if (message.isNotBlank()) FlowText(message, style = FlowType.caption, color = pal.accent)
        Hairline()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            listOf(data.tasks.size to "وظیفه", data.routines.size to "روتین", data.goals.size to "هدف", data.sessions.size to "جلسه").forEach { (n, l) ->
                Column {
                    FlowText(num(n), style = FlowType.numeral)
                    FlowText(l, style = FlowType.caption, color = pal.textSecondary)
                }
            }
        }
        FlowText("پوشه‌ی داده: " + ltr(appDataDirectory().absolutePath), style = FlowType.caption, color = pal.textTertiary, maxLines = 2)
    }
}

// ───────────────────────── File dialogs ─────────────────────────

private fun chooseWallpaper(runtime: DesktopRuntime) {
    val chooser = JFileChooser().apply {
        dialogTitle = "انتخاب تصویر پس‌زمینه"
        fileFilter = FileNameExtensionFilter("Images (PNG, JPG)", "png", "jpg", "jpeg")
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val src = chooser.selectedFile
        if (src.extension.lowercase() !in setOf("png", "jpg", "jpeg")) return
        val dst = File(appDataDirectory(), "wallpaper.${src.extension.lowercase()}")
        runCatching {
            src.copyTo(dst, overwrite = true)
            runtime.updateSettings { it.copy(wallpaperId = "custom", wallpaperPath = dst.absolutePath) }
        }
    }
}

private fun exportBackup(runtime: DesktopRuntime): String {
    val chooser = JFileChooser().apply {
        dialogTitle = "ذخیره‌ی پشتیبان FLOW"
        selectedFile = File("FLOW-backup.json")
        fileFilter = FileNameExtensionFilter("JSON", "json")
    }
    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return ""
    var file = chooser.selectedFile
    if (!file.name.endsWith(".json", ignoreCase = true)) file = File(file.parentFile, file.name + ".json")
    return runCatching { file.writeText(runtime.exportJson(), Charsets.UTF_8); "پشتیبان ذخیره شد: ${file.name}" }.getOrDefault("ذخیره‌ی پشتیبان ناموفق بود.")
}

private fun importBackup(runtime: DesktopRuntime): String {
    val chooser = JFileChooser().apply {
        dialogTitle = "انتخاب پشتیبان FLOW"
        fileFilter = FileNameExtensionFilter("JSON", "json")
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return ""
    val ok = runCatching { runtime.importJson(chooser.selectedFile.readText(Charsets.UTF_8)) }.getOrDefault(false)
    return if (ok) "پشتیبان با موفقیت بازیابی شد." else "فایل پشتیبان معتبر نبود؛ چیزی تغییر نکرد."
}
