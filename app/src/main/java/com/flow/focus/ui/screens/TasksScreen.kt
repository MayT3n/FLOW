package com.flow.focus.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flow.focus.data.AppData
import com.flow.focus.data.Categories
import com.flow.focus.data.Priority
import com.flow.focus.data.Task
import com.flow.focus.domain.Stats
import com.flow.focus.domain.TaskFilter
import com.flow.focus.domain.TaskQuery
import com.flow.focus.domain.TaskSort
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.CheckCircle
import com.flow.focus.ui.components.Chip
import com.flow.focus.ui.components.EmptyState
import com.flow.focus.ui.components.Fab
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowProgressBar
import com.flow.focus.ui.components.FlowTextField
import com.flow.focus.ui.components.GlassIconButton
import com.flow.focus.ui.components.ScreenHeader
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.num
import com.flow.focus.ui.dueLabel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.glass.floatSpring
import com.flow.focus.ui.isOverdue
import com.flow.focus.ui.rememberToday
import com.flow.focus.ui.theme.Flow

@Composable
fun TasksScreen(vm: AppViewModel, data: AppData, onStartFocus: () -> Unit, modifier: Modifier = Modifier) {
    val today = rememberToday()
    var query by rememberSaveable { mutableStateOf("") }
    var filterIdx by rememberSaveable { mutableStateOf(0) }
    var sortIdx by rememberSaveable { mutableStateOf(0) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }

    val filter = TaskFilter.entries[filterIdx]
    val sort = TaskSort.entries[sortIdx]
    val list = remember(data.tasks, filter, sort, category, query, today) {
        TaskQuery.apply(data.tasks, filter, sort, category, query, today.toEpochDay())
    }
    val (doneToday, totalToday) = remember(data.tasks, today) { TaskQuery.todayProgress(data.tasks, today.toEpochDay()) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { ScreenHeader("وظایف") }
            item(key = "search") { FlowTextField(query, { query = it }, "جست‌وجو در وظایف", leading = FlowIcons.Search) }
            item(key = "progress") { TodayProgressCard(doneToday, totalToday) }
            item(key = "filters") {
                SegmentedControl(listOf("همه", "امروز", "آینده", "انجام‌شده"), filterIdx, { filterIdx = it })
            }
            item(key = "sort") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("سررسید", "اولویت", "جدیدترین").forEachIndexed { i, l -> Chip(l, sortIdx == i, { sortIdx = i }) }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("همه‌ی دسته‌ها", category == null, { category = null })
                        Categories.all.forEach { cat -> Chip(cat.name, category == cat.id, { category = cat.id }, dot = Color(cat.color)) }
                    }
                }
            }
            if (list.isEmpty()) {
                item(key = "empty") {
                    if (data.tasks.isEmpty()) EmptyState(FlowIcons.Tasks, "هنوز وظیفه‌ای نداری", "با دکمه‌ی + اولین وظیفه‌ات را بساز.")
                    else EmptyState(FlowIcons.Search, "موردی پیدا نشد", "فیلتر یا عبارت جست‌وجو را تغییر بده.")
                }
            }
            items(list, key = { it.id }) { t ->
                TaskRow(
                    t, today, Stats.pomodorosDone(data.sessions, t.id),
                    active = data.timer.taskId == t.id,
                    onToggle = { vm.setTaskDone(t.id, !t.done) },
                    onEdit = { editing = t },
                    onFocus = { vm.selectTask(t.id); onStartFocus() },
                )
            }
        }
        Fab({ creating = true }, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 108.dp).navigationBarsPadding())
    }

    if (creating) {
        TaskEditorDialog(null, Task("", "", dueDay = today.toEpochDay()), { vm.saveTask(it); creating = false }, null, { creating = false })
    }
    editing?.let { t ->
        TaskEditorDialog(t, null, { vm.saveTask(it); editing = null }, { vm.deleteTask(it.id); editing = null }, { editing = null })
    }
}

@Composable
private fun TodayProgressCard(done: Int, total: Int) {
    val c = Flow.colors
    val target = if (total == 0) 0f else done.toFloat() / total
    val p by animateFloatAsState(target, floatSpring(1f, 200f), label = "todayProgress")
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("پیشرفت امروز", style = Flow.type.heading, color = c.textPrimary)
                Text(
                    if (total == 0) "برای امروز وظیفه‌ای نداری" else num("$done از $total وظیفه انجام شد"),
                    style = Flow.type.caption, color = c.textSecondary,
                )
            }
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 8.dp.toPx()
                    val inset = sw / 2
                    val arc = Size(size.width - sw, size.height - sw)
                    drawArc(c.track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(sw))
                    if (p > 0f) {
                        rotate(-90f, center) {
                            drawArc(
                                Brush.sweepGradient(listOf(c.success, c.accent, c.success), center), 0f, 360f * p, false,
                                Offset(inset, inset), arc, style = Stroke(sw, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
                Text(num("${(p * 100).toInt()}%"), style = Flow.type.caption, color = c.textPrimary)
            }
        }
    }
}

@Composable
private fun TaskRow(
    t: Task,
    today: java.time.LocalDate,
    pomodorosDone: Int,
    active: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onFocus: () -> Unit,
) {
    val c = Flow.colors
    val cat = Categories.byId(t.categoryId)
    val due = dueLabel(t, today)
    val overdue = isOverdue(t, today)
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) c.accentSoft.copy(alpha = 0.55f) else Color.Transparent)
            .springClick(scaleDown = 0.99f, onClick = onEdit)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CheckCircle(t.done, onToggle, size = 28.dp, color = Color(cat.color))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        t.title, style = Flow.type.bodyStrong,
                        color = if (t.done) c.textSecondary else c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(cat.color)))
                        Text(cat.name, style = Flow.type.caption, color = c.textSecondary)
                        if (t.priority == Priority.HIGH) Text("· اولویت زیاد", style = Flow.type.caption, color = c.warning)
                        if (due != null) Text(num("· $due"), style = Flow.type.caption, color = if (overdue) c.danger else c.textSecondary)
                    }
                }
                if (!t.done) {
                    GlassIconButton(FlowIcons.Play, onFocus, size = 38.dp, iconSize = 18.dp, contentDescription = "شروع تمرکز روی این وظیفه")
                }
            }
            if (t.estimatedPomodoros > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowProgressBar(pomodorosDone.toFloat() / t.estimatedPomodoros, Modifier.weight(1f), color = Color(cat.color), height = 5.dp)
                    Text(num("$pomodorosDone/${t.estimatedPomodoros} جلسه"), style = Flow.type.caption, color = c.textSecondary)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.track.copy(alpha = 0.8f)))
    }
}
