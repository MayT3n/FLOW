package com.flow.focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import com.flow.focus.core.DateFormats
import com.flow.focus.core.JalaliDate
import com.flow.focus.data.Categories
import com.flow.focus.data.Goal
import com.flow.focus.data.Priority
import com.flow.focus.data.Routine
import com.flow.focus.data.Settings
import com.flow.focus.data.Task
import com.flow.focus.data.FlowRepository
import com.flow.focus.platform.AmbientSynth
import com.flow.focus.platform.AmbientLibrary
import com.flow.focus.ui.components.CheckCircle
import com.flow.focus.ui.components.Chip
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowSlider
import com.flow.focus.ui.components.FlowTextField
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.JalaliDatePickerDialog
import com.flow.focus.ui.components.PrimaryButton
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.Stepper
import com.flow.focus.ui.components.SwitchRow
import com.flow.focus.ui.components.TimeEditor
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.relativeDay
import com.flow.focus.ui.theme.Flow
import java.time.LocalDate

@Composable
private fun Label(text: String) = Text(text, style = Flow.type.label, color = Flow.colors.textSecondary)

/** ایجاد/ویرایش وظیفه. [initial] برای ویرایش، [template] برای مقادیر پیش‌فرض یک وظیفه‌ی جدید. */
@Composable
fun TaskEditorDialog(
    initial: Task?,
    template: Task? = null,
    onSave: (Task) -> Unit,
    onDelete: ((Task) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val base = initial ?: template
    var title by remember { mutableStateOf(base?.title ?: "") }
    var notes by remember { mutableStateOf(base?.notes ?: "") }
    var dueDay by remember { mutableStateOf(base?.dueDay) }
    var minute by remember { mutableStateOf(base?.dueMinute) }
    var priority by remember { mutableStateOf(base?.priority ?: Priority.MEDIUM) }
    var category by remember { mutableStateOf(base?.categoryId ?: "study") }
    var pomodoros by remember { mutableStateOf(base?.estimatedPomodoros ?: 1) }
    var done by remember { mutableStateOf(base?.done ?: false) }
    var pickDate by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    FlowDialog(onDismiss, if (initial == null) "وظیفه‌ی جدید" else "ویرایش وظیفه") {
        FlowTextField(title, { title = it }, "عنوان وظیفه")
        FlowTextField(notes, { notes = it }, "توضیحات (اختیاری)", singleLine = false, minLines = 2)

        Label("تاریخ")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("امروز", dueDay == today.toEpochDay(), { dueDay = today.toEpochDay() })
            Chip("فردا", dueDay == today.toEpochDay() + 1, { dueDay = today.toEpochDay() + 1 })
            val custom = dueDay != null && dueDay != today.toEpochDay() && dueDay != today.toEpochDay() + 1
            Chip(if (custom) num(DateFormats.short(LocalDate.ofEpochDay(dueDay!!))) else "انتخاب تاریخ", custom, { pickDate = true })
            Chip("بدون تاریخ", dueDay == null, { dueDay = null; minute = null })
        }
        if (dueDay != null) {
            SwitchRow("ساعت مشخص", minute != null, { minute = if (it) (minute ?: 9 * 60) else null }, subtitle = "برای یادآوری در همان ساعت")
            minute?.let { m -> TimeEditor(m, { minute = it }) }
        }
        if (initial != null) {
            SwitchRow(
                "انجام‌شده",
                done,
                { done = it },
                subtitle = if (done) "این وظیفه در فهرست انجام‌شده‌ها قرار می‌گیرد" else "وظیفه هنوز در حال انجام است",
            )
        }

        Label("اولویت")
        SegmentedControl(Priority.entries.map { it.label }, priority.ordinal, { priority = Priority.entries[it] })

        Label("دسته‌بندی")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Categories.all.forEach { cat ->
                Chip(cat.name, category == cat.id, { category = cat.id }, dot = Color(cat.color))
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("تعداد جلسه‌ی پومودورو", style = Flow.type.body, color = Flow.colors.textPrimary)
            }
            Stepper(pomodoros, { pomodoros = it }, 1..20)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                "ذخیره",
                onClick = {
                    val t = (initial ?: Task(FlowRepository.newId(), "", createdAt = System.currentTimeMillis())).copy(
                        title = title.trim(), notes = notes.trim(), dueDay = dueDay,
                        dueMinute = if (dueDay != null) minute else null,
                        priority = priority, categoryId = category, estimatedPomodoros = pomodoros,
                    )
                    onSave(t)
                },
                modifier = Modifier.weight(1f), enabled = title.isNotBlank(),
            )
            GlassButton("انصراف", onDismiss, Modifier.weight(1f))
        }
        if (initial != null && onDelete != null) {
            GlassButton("حذف وظیفه", { onDelete(initial) }, Modifier.fillMaxWidth(), icon = FlowIcons.Trash, danger = true)
        }
    }
    if (pickDate) {
        JalaliDatePickerDialog(dueDay, onPick = { dueDay = it; pickDate = false }, onDismiss = { pickDate = false })
    }
}

/** انتخاب وظیفه‌ی فعال تایمر */
@Composable
fun TaskPickerDialog(
    tasks: List<Task>,
    selectedId: String?,
    onPick: (String?) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    val open = tasks.filter { !it.done }.sortedWith(compareBy({ it.dueDay ?: Long.MAX_VALUE }, { it.dueMinute ?: Int.MAX_VALUE }))
    FlowDialog(onDismiss, "تغییر وظیفه") {
        if (open.isEmpty()) {
            EmptyState(FlowIcons.Tasks, "وظیفه‌ی بازی نیست", "یک وظیفه بساز تا جلسه‌های تمرکز به آن ثبت شوند.")
        }
        open.forEach { t ->
            GlassSurface(
                Modifier.fillMaxWidth().springClick(scaleDown = 0.98f, onClick = { onPick(t.id) }),
                shape = RoundedCornerShape(18.dp), level = GlassLevel.Subtle,
                tint = if (t.id == selectedId) Flow.colors.accent else null,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(Categories.byId(t.categoryId).color)))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        t.dueDay?.let { Text(num(relativeDay(it, today)), style = Flow.type.caption, color = Flow.colors.textSecondary) }
                    }
                    if (t.id == selectedId) FlowIcon(FlowIcons.Check, tint = Flow.colors.accent, size = 20.dp)
                }
            }
        }
        GlassButton("بدون وظیفه", { onPick(null) }, Modifier.fillMaxWidth())
        PrimaryButton("وظیفه‌ی جدید", onCreate, Modifier.fillMaxWidth(), icon = FlowIcons.Plus)
    }
}

@Composable
fun RoutineEditorDialog(initial: Routine?, onSave: (Routine) -> Unit, onDelete: ((Routine) -> Unit)?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var days by remember { mutableStateOf(initial?.weekdays?.toSet() ?: setOf(0, 1, 2, 3, 4, 5, 6)) }
    var minute by remember { mutableStateOf(initial?.minute) }
    var category by remember { mutableStateOf(initial?.categoryId ?: "personal") }
    FlowDialog(onDismiss, if (initial == null) "روتین جدید" else "ویرایش روتین") {
        FlowTextField(title, { title = it }, "نام روتین (مثلاً ورزش صبحگاهی)")
        Label("روزهای تکرار")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            JalaliDate.weekdayNames.forEachIndexed { i, name ->
                Chip(name.take(2), i in days, { days = if (i in days) days - i else days + i }, Modifier.weight(1f))
            }
        }
        SwitchRow("ساعت مشخص", minute != null, { minute = if (it) (minute ?: 7 * 60) else null })
        minute?.let { m -> TimeEditor(m, { minute = it }) }
        Label("دسته‌بندی")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Categories.all.forEach { cat -> Chip(cat.name, category == cat.id, { category = cat.id }, dot = Color(cat.color)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                "ذخیره",
                {
                    onSave(
                        Routine(
                            id = initial?.id ?: FlowRepository.newId(), title = title.trim(),
                            weekdays = days.sorted(), minute = minute, categoryId = category,
                        ),
                    )
                },
                Modifier.weight(1f), enabled = title.isNotBlank() && days.isNotEmpty(),
            )
            GlassButton("انصراف", onDismiss, Modifier.weight(1f))
        }
        if (initial != null && onDelete != null) {
            GlassButton("حذف روتین", { onDelete(initial) }, Modifier.fillMaxWidth(), icon = FlowIcons.Trash, danger = true)
        }
    }
}

/** کارت اهداف روزانه/هفتگی */
@Composable
fun GoalsCard(
    title: String,
    goals: List<Goal>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowIcon(FlowIcons.Flag, tint = Flow.colors.accent, size = 20.dp)
                Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), style = Flow.type.heading, color = Flow.colors.textPrimary)
                if (goals.isNotEmpty()) Text(num("${goals.count { it.done }}/${goals.size}"), style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            goals.forEach { g ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CheckCircle(g.done, { onToggle(g.id) }, size = 26.dp, color = Flow.colors.success)
                    Text(
                        g.title, Modifier.weight(1f), style = Flow.type.body,
                        color = if (g.done) Flow.colors.textSecondary else Flow.colors.textPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    GlassIconButton(FlowIcons.Close, { onDelete(g.id) }, size = 32.dp, iconSize = 16.dp, contentDescription = "حذف هدف")
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowTextField(text, { text = it }, "هدف جدید…", Modifier.weight(1f))
                GlassIconButton(FlowIcons.Plus, { if (text.isNotBlank()) { onAdd(text); text = "" } }, size = 44.dp, enabled = text.isNotBlank(), contentDescription = "افزودن هدف")
            }
        }
    }
}

/** انتخاب ضبط‌های واقعی محیط؛ منبع و مجوز هر فایل از کاتالوگ قابل مشاهده است. */
@Composable
fun AmbientPicker(
    settings: Settings,
    playingId: String?,
    onSelect: (String) -> Unit,
    onPreview: (String) -> Unit,
    onVolume: (Float) -> Unit,
    onAuto: (Boolean) -> Unit,
    isDownloaded: (String) -> Boolean = { false },
    downloadProgress: Map<String, Float> = emptyMap(),
    downloadedBytes: Long = 0L,
    onDownload: (String) -> Unit = {},
    onDeleteDownload: (String) -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val none = listOf(com.flow.focus.platform.AmbientSoundInfo(AmbientSynth.NONE, "خاموش", "بدون صدای محیط"))
    (none + AmbientSynth.sounds).forEach { sound ->
        val selected = settings.ambientSound == sound.id
        val recording = AmbientLibrary.byId(sound.id)
        GlassSurface(
            Modifier.fillMaxWidth().springClick(scaleDown = 0.98f, onClick = { onSelect(sound.id) }),
            shape = RoundedCornerShape(18.dp),
            level = GlassLevel.Subtle,
            tint = if (selected) Flow.colors.accent else null,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FlowIcon(ambientIcon(sound.id), tint = if (selected) Flow.colors.accent else Flow.colors.textSecondary, size = 22.dp)
                    Column(Modifier.weight(1f)) {
                        Text(sound.label, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary)
                        Text(sound.hint, style = Flow.type.caption, color = Flow.colors.textSecondary)
                    }
                    if (sound.id != AmbientSynth.NONE) {
                        GlassIconButton(
                            if (playingId == sound.id) FlowIcons.Pause else FlowIcons.Play,
                            { onPreview(sound.id) },
                            size = 36.dp,
                            iconSize = 16.dp,
                            contentDescription = "پیش‌شنوایی",
                        )
                    }
                }
                if (recording != null) {
                    Text(
                        "${recording.duration} · ${recording.license} · ${recording.creator}",
                        style = Flow.type.caption,
                        color = Flow.colors.textSecondary,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "منبع و مجوز",
                            Modifier.springClick(onClick = { runCatching { uriHandler.openUri(recording.sourcePage) } }).padding(vertical = 4.dp),
                            style = Flow.type.caption,
                            color = Flow.colors.accent,
                        )
                        Spacer(Modifier.weight(1f))
                        val progress = downloadProgress[sound.id]
                        when {
                            progress != null -> Text(
                                num("${(progress * 100).toInt()}٪"),
                                style = Flow.type.caption,
                                color = Flow.colors.accent,
                            )
                            isDownloaded(sound.id) -> Text(
                                "ذخیره‌شده",
                                Modifier.springClick(onClick = { onDeleteDownload(sound.id) }).padding(vertical = 4.dp),
                                style = Flow.type.caption,
                                color = Flow.colors.success,
                            )
                            else -> Text(
                                "دانلود آفلاین",
                                Modifier.springClick(onClick = { onDownload(sound.id) }).padding(vertical = 4.dp),
                                style = Flow.type.caption,
                                color = Flow.colors.accent,
                            )
                        }
                    }
                    recording.attribution?.let {
                        Text(it, style = Flow.type.caption, color = Flow.colors.textSecondary)
                    }
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowIcon(FlowIcons.Volume, tint = Flow.colors.textSecondary, size = 20.dp)
        FlowSlider(settings.ambientVolume, onVolume, 0f..1f, Modifier.weight(1f))
        Text(num("${(settings.ambientVolume * 100).toInt()}%"), style = Flow.type.caption, color = Flow.colors.textSecondary)
    }
    SwitchRow("پخش خودکار با شروع تمرکز", settings.ambientAuto, onAuto, subtitle = "صدای محیط در Focus خودکار فعال می‌شود")
    Text(
        "حجم صداهای ذخیره‌شده: ${formatAmbientBytes(downloadedBytes)}",
        style = Flow.type.caption,
        color = Flow.colors.textSecondary,
    )
}

private fun formatAmbientBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}

fun ambientIcon(id: String) = when (id) {
    "rain" -> FlowIcons.Rain
    "wind" -> FlowIcons.Wind
    "forest" -> FlowIcons.Leaf
    "fire" -> FlowIcons.Flame
    AmbientSynth.NONE -> FlowIcons.Close
    else -> FlowIcons.Wave
}
