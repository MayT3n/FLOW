package com.flow.focus.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.BuildConfig
import com.flow.focus.data.AppData
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.ThemeMode
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowSlider
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.ScreenHeader
import com.flow.focus.ui.components.SectionTitle
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.SettingRow
import com.flow.focus.ui.components.Stepper
import com.flow.focus.ui.components.SwitchRow
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.theme.Flow
import java.time.LocalDate

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
        }
    }
}

private fun readText(ctx: Context, uri: Uri): String? =
    runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } }.getOrNull()

private fun writeText(ctx: Context, uri: Uri, text: String): Boolean =
    runCatching { ctx.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) } != null }.getOrDefault(false)

@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData, modifier: Modifier = Modifier) {
    val st = data.settings
    val ctx = LocalContext.current
    val playing by vm.ambientPlaying.collectAsStateWithLifecycle()
    var confirmHistory by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }

    // پیش‌شنوایی با خروج از صفحه قطع شود
    DisposableEffect(Unit) { onDispose { vm.stopAmbientPreview() } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = writeText(ctx, uri, vm.exportJson())
            Toast.makeText(ctx, if (ok) "نسخه‌ی پشتیبان ذخیره شد" else "ذخیره‌ی فایل ناموفق بود", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = readText(ctx, uri)
            val ok = text != null && vm.importJson(text)
            Toast.makeText(ctx, if (ok) "اطلاعات بازیابی شد" else "فایل پشتیبان معتبر نیست؛ چیزی تغییر نکرد", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("تنظیمات")

        Section("ظاهر") {
            Text("تم", style = Flow.type.body, color = Flow.colors.textPrimary)
            SegmentedControl(listOf("روشن", "تاریک", "سیستم"), listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).indexOf(st.theme), {
                vm.updateSettings { s -> s.copy(theme = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[it]) }
            })
            Text("اعداد", style = Flow.type.body, color = Flow.colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
            SegmentedControl(listOf("فارسی ۱۲۳", "انگلیسی 123"), if (st.persianDigits) 0 else 1, { vm.updateSettings { s -> s.copy(persianDigits = it == 0) } })
            SwitchRow("انیمیشن‌ها", st.animations, { v -> vm.updateSettings { it.copy(animations = v) } }, subtitle = "خاموش کردن، مصرف باتری را کم می‌کند")
        }

        Section("کیفیت Liquid Glass") {
            SegmentedControl(listOf("کم", "متوسط", "بالا"), st.glassQuality.ordinal, { vm.updateSettings { s -> s.copy(glassQuality = GlassQuality.entries[it]) } })
            Text(
                when (st.glassQuality) {
                    GlassQuality.LOW -> "سطح ساده و کم‌مصرف؛ مناسب دستگاه‌های ضعیف."
                    GlassQuality.MEDIUM -> "شیشه‌ی نیمه‌شفاف با درخشش و لبه‌ی نورانی."
                    GlassQuality.HIGH -> "شامل تاری واقعی پس‌زمینه (اندروید ۱۲ به بالا)؛ مصرف بیشتر."
                },
                style = Flow.type.caption, color = Flow.colors.textSecondary,
            )
            if (st.glassQuality != GlassQuality.LOW) {
                SettingRow("شفافیت") { Text(num("${(st.glassOpacity * 100).toInt()}%"), style = Flow.type.caption, color = Flow.colors.textSecondary) }
                FlowSlider(st.glassOpacity, { v -> vm.updateSettings { it.copy(glassOpacity = v) } }, 0.4f..1.6f)
                SettingRow("درخشش نور") { Text(num("${(st.glassShine * 100).toInt()}%"), style = Flow.type.caption, color = Flow.colors.textSecondary) }
                FlowSlider(st.glassShine, { v -> vm.updateSettings { it.copy(glassShine = v) } }, 0f..1f)
                if (st.glassQuality == GlassQuality.HIGH) {
                    SettingRow("شدت تاری") { Text(num("${st.glassBlurDp}"), style = Flow.type.caption, color = Flow.colors.textSecondary) }
                    FlowSlider(st.glassBlurDp.toFloat(), { v -> vm.updateSettings { it.copy(glassBlurDp = v.toInt()) } }, 4f..48f)
                }
            }
        }

        Section("پومودورو") {
            SettingRow("مدت تمرکز") { Stepper(st.focusMin, { v -> vm.updateSettings { it.copy(focusMin = v) } }, 1..180, suffix = "دقیقه") }
            SettingRow("استراحت کوتاه") { Stepper(st.shortBreakMin, { v -> vm.updateSettings { it.copy(shortBreakMin = v) } }, 1..60, suffix = "دقیقه") }
            SettingRow("استراحت بلند") { Stepper(st.longBreakMin, { v -> vm.updateSettings { it.copy(longBreakMin = v) } }, 1..90, suffix = "دقیقه") }
            SettingRow("تعداد دور هر چرخه") { Stepper(st.rounds, { v -> vm.updateSettings { it.copy(rounds = v) } }, 1..12) }
            SettingRow("هدف روزانه") { Stepper(st.dailyGoalMin, { v -> vm.updateSettings { it.copy(dailyGoalMin = v) } }, 10..720, step = 10, suffix = "دقیقه") }
            SwitchRow("شروع خودکار استراحت", st.autoStartBreak, { v -> vm.updateSettings { it.copy(autoStartBreak = v) } })
            SwitchRow("شروع خودکار جلسه‌ی بعد", st.autoStartFocus, { v -> vm.updateSettings { it.copy(autoStartFocus = v) } })
        }

        Section("اعلان‌ها") {
            SwitchRow("اعلان‌ها", st.notifications, { v -> vm.updateSettings { it.copy(notifications = v) } }, subtitle = "پایان جلسه، تایمر در حال اجرا و یادآوری وظایف")
            SwitchRow("صدا", st.sound, { v -> vm.updateSettings { it.copy(sound = v) } })
            SwitchRow("لرزش", st.vibration, { v -> vm.updateSettings { it.copy(vibration = v) } })
        }

        Section("صدای محیط") {
            AmbientPicker(
                st, playing,
                onSelect = { id -> vm.updateSettings { it.copy(ambientSound = id) } },
                onPreview = { vm.previewAmbient(it) },
                onVolume = { v -> vm.updateSettings { it.copy(ambientVolume = v) } },
                onAuto = { v -> vm.updateSettings { it.copy(ambientAuto = v) } },
            )
        }

        Section("مدیریت داده‌ها") {
            Text(
                num("${data.tasks.size} وظیفه · ${data.sessions.size} جلسه‌ی ثبت‌شده · ${data.routines.size} روتین"),
                style = Flow.type.caption, color = Flow.colors.textSecondary,
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton("خروجی پشتیبان", { exportLauncher.launch("flow-backup-${LocalDate.now()}.json") }, Modifier.weight(1f), icon = FlowIcons.Export)
                GlassButton("بازیابی", { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.weight(1f), icon = FlowIcons.Import)
            }
            GlassButton("پاک کردن تاریخچه‌ی جلسه‌ها", { confirmHistory = true }, Modifier.fillMaxWidth().padding(top = 6.dp), icon = FlowIcons.Trash, danger = true)
            GlassButton("حذف همه‌ی اطلاعات", { confirmAll = true }, Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp), icon = FlowIcons.Trash, danger = true)
        }

        Section("درباره") {
            SettingRow("FLOW", subtitle = "تمرکز کن، جریان بساز.") { Text(num("نسخه ${BuildConfig.VERSION_NAME}"), style = Flow.type.caption, color = Flow.colors.textSecondary) }
        }
    }

    if (confirmHistory) {
        FlowDialog({ confirmHistory = false }, "پاک کردن تاریخچه؟") {
            Text("همه‌ی جلسه‌های ثبت‌شده و آمار آن‌ها حذف می‌شود. وظایف و تنظیمات دست‌نخورده می‌مانند.", style = Flow.type.body, color = Flow.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("پاک کن", { vm.clearHistory(); confirmHistory = false }, Modifier.weight(1f))
                GlassButton("انصراف", { confirmHistory = false }, Modifier.weight(1f))
            }
        }
    }
    if (confirmAll) {
        FlowDialog({ confirmAll = false }, "حذف همه‌ی اطلاعات؟") {
            Text("وظایف، روتین‌ها، اهداف و تاریخچه حذف می‌شوند (تنظیمات می‌مانند). این کار قابل بازگشت نیست؛ پیشنهاد می‌شود اول پشتیبان بگیری.", style = Flow.type.body, color = Flow.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("حذف کن", { vm.clearAll(); confirmAll = false }, Modifier.weight(1f))
                GlassButton("انصراف", { confirmAll = false }, Modifier.weight(1f))
            }
        }
    }
}
