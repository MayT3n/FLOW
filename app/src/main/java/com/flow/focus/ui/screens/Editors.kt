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
import com.flow.focus.data.ActivityKind
import com.flow.focus.data.Categories
import com.flow.focus.data.Goal
import com.flow.focus.data.Priority
import com.flow.focus.data.Routine
import com.flow.focus.data.RoutineStep
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
import com.flow.focus.ui.components.FlowSheet
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
    var dueDay by remember {
        mutableStateOf(
            base?.dueDay?.takeIf { epoch -> runCatching { LocalDate.ofEpochDay(epoch) }.isSuccess },
        )
    }
    var minute by remember { mutableStateOf(base?.dueMinute?.takeIf { it in 0 until 24 * 60 }) }
    var priority by remember { mutableStateOf(base?.priority ?: Priority.MEDIUM) }
    var category by remember { mutableStateOf(base?.categoryId ?: "study") }
    var pomodoros by remember { mutableStateOf(base?.estimatedPomodoros ?: 1) }
    var done by remember { mutableStateOf(base?.done ?: false) }
    var kind by remember { mutableStateOf(base?.kind ?: ActivityKind.TASK) }
    var duration by remember { mutableStateOf(base?.durationMin) }
    var pickDate by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    FlowSheet(onDismiss, if (initial == null) "افزودن به برنامه" else "ویرایش") {
        FlowTextField(title, { title = it }, "عنوان")
        SegmentedControl(ActivityKind.entries.map { it.label }, kind.ordinal, { kind = ActivityKind.entries[it] })
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
            if (minute != null) {
                SwitchRow("مدت‌زمان", duration != null, { duration = if (it) (duration ?: 30) else null }, subtitle = "برای نمایش بازه‌ی زمانی در جدول روز")
                duration?.let { d -> Stepper(d, { duration = it }, 5..720, step = 5, suffix = "دقیقه") }
            }
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
                        kind = kind,
                        durationMin = if (dueDay != null && minute != null) duration else null,
                        done = done,
                        doneAt = if (done) initial?.doneAt ?: System.currentTimeMillis() else null,
                    )
                    onSave(t)
                },
                modifier = Modifier.weight(1f), enabled = title.isNotBlank(),
            )
            GlassButton("انصراف", onDismiss, Modifier.weight(1f))
        }
        if (initial != null && onDelete != null) {
            GlassButton("حذف", { onDelete(initial) }, Modifier.fillMaxWidth(), icon = FlowIcons.Trash, danger = true)
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
            EmptyState(FlowIcons.Tasks, "وظیفه‌ای برای انتخاب وجود ندارد", "یک وظیفه بساز تا جلسه‌های تمرکز به آن ثبت شوند.")
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
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowIcon(FlowIcons.Flag, tint = Flow.colors.accent, size = 20.dp)
                Text(
                    title,
                    Modifier.weight(1f),
                    style = Flow.type.heading,
                    color = Flow.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (goals.isNotEmpty()) {
                    Text(
                        num("${goals.count { it.done }}/${goals.size}"),
                        style = Flow.type.caption,
                        color = Flow.colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
            goals.forEach { g ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckCircle(
                        g.done,
                        { onToggle(g.id) },
                        size = 26.dp,
                        color = Flow.colors.success,
                    )
                    Text(
                        g.title,
                        Modifier.weight(1f),
                        style = Flow.type.body,
                        color = if (g.done) Flow.colors.textSecondary else Flow.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    GlassIconButton(
                        FlowIcons.Close,
                        { onDelete(g.id) },
                        size = 30.dp,
                        iconSize = 16.dp,
                        contentDescription = "حذف هدف",
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowTextField(
                    text,
                    { text = it },
                    "هدف جدید…",
                    modifier = Modifier.weight(1f),
                )
                GlassIconButton(
                    FlowIcons.Plus,
                    { if (text.isNotBlank()) { onAdd(text); text = "" } },
                    size = 38.dp,
                    enabled = text.isNotBlank(),
                    contentDescription = "افزودن هدف",
                )
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
        val downloaded = isDownloaded(sound.id)
        val progress = downloadProgress[sound.id]
        val isNone = sound.id == AmbientSynth.NONE
        GlassSurface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            level = GlassLevel.Subtle,
            tint = if (selected) Flow.colors.accent else null,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth().springClick(scaleDown = 0.985f, onClick = { onSelect(sound.id) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    if (isNone) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Flow.colors.track.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                            FlowIcon(FlowIcons.Volume, tint = Flow.colors.textSecondary, size = 24.dp)
                        }
                    } else {
                        CoverArt("ambient-" + sound.id, Modifier.size(54.dp), corner = 16.dp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(sound.label, style = Flow.type.bodyStrong, color = if (selected) Flow.colors.accent else Flow.colors.textPrimary, maxLines = 2)
                        Text(
                            if (recording != null) "${recording.duration} · ${sound.hint}" else sound.hint,
                            style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 2,
                        )
                        Text(
                            when {
                                selected -> "انتخاب‌شده برای جلسه‌های تمرکز"
                                recording != null && progress != null -> "در حال دانلود…"
                                recording != null && downloaded -> "دانلودشده · آماده‌ی پخش آفلاین"
                                recording != null -> "برای پخش اولیه به اینترنت نیاز دارد"
                                else -> ""
                            },
                            style = Flow.type.caption,
                            color = if (selected || downloaded) Flow.colors.success else Flow.colors.textSecondary,
                            maxLines = 2,
                        )
                    }
                    when {
                        progress != null -> Text(num("${(progress * 100).toInt()}٪"), style = Flow.type.label, color = Flow.colors.accent)
                        !isNone -> GlassIconButton(
                            if (playingId == sound.id) FlowIcons.Pause else FlowIcons.Play,
                            { onPreview(sound.id) },
                            size = 42.dp, iconSize = 19.dp, active = playingId == sound.id,
                            contentDescription = "پیش‌شنوایی",
                        )
                    }
                }
                if (!isNone) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TrackChip(FlowIcons.Check, if (selected) "انتخاب‌شده" else "انتخاب", selected, Modifier.weight(1f)) { onSelect(sound.id) }
                        if (recording != null) {
                            if (downloaded) TrackChip(FlowIcons.Trash, "حذف فایل", false, Modifier.weight(1f)) { onDeleteDownload(sound.id) }
                            else TrackChip(FlowIcons.Download, "دانلود", false, Modifier.weight(1f), enabled = progress == null) { onDownload(sound.id) }
                            TrackChip(FlowIcons.Info, "منبع و مجوز", false, Modifier.weight(1f)) { runCatching { uriHandler.openUri(recording.sourcePage) } }
                        }
                    }
                    recording?.attribution?.let { Text(it, style = Flow.type.caption, color = Flow.colors.textSecondary) }
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
