package com.flow.focus

import com.flow.focus.core.JalaliDate
import com.flow.focus.data.AppData
import com.flow.focus.data.Priority
import com.flow.focus.data.Routine
import com.flow.focus.data.Task
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import java.io.File
import com.flow.focus.data.JsonStore
import com.flow.focus.data.FlowRepository
import com.flow.focus.data.Settings
import com.flow.focus.domain.Planner
import com.flow.focus.domain.RoutineEntry
import com.flow.focus.domain.TaskEntry
import com.flow.focus.domain.TaskFilter
import com.flow.focus.domain.TaskQuery
import com.flow.focus.domain.TaskSort
import com.flow.focus.platform.AmbientSynth
import com.flow.focus.platform.AmbientLibrary
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
    private val today = LocalDate.of(2026, 11, 10) // سه‌شنبه، ۱۹ آبان ۱۴۰۵
    private val e = today.toEpochDay()

    @Test fun entriesAreSortedAndIncludeRoutines() {
        val data = AppData(
            tasks = listOf(
                Task("a", "بدون ساعت", dueDay = e),
                Task("b", "صبح", dueDay = e, dueMinute = 9 * 60),
                Task("c", "فردا", dueDay = e + 1),
            ),
            routines = listOf(Routine("r", "ورزش", weekdays = listOf(3), minute = 7 * 60)),
        )
        val list = Planner.entriesFor(data, today)
        assertEquals(listOf("ورزش", "صبح", "بدون ساعت"), list.map { it.title })
        assertTrue(list[0] is RoutineEntry && list[1] is TaskEntry)
        // روتین فقط در روز هفته‌ی خودش دیده می‌شود
        assertEquals(1, Planner.entriesFor(data, today.plusDays(1)).size)
    }

    @Test fun routineCheckIsPerDay() {
        val data = AppData(
            routines = listOf(Routine("r", "مطالعه", weekdays = listOf(3, 4))),
            routineChecks = listOf("r|$e"),
        )
        assertTrue(Planner.entriesFor(data, today).single().done)
        assertTrue(!Planner.entriesFor(data, today.plusDays(1)).single().done)
    }

    @Test fun nextSkipsPastAndDoneItems() {
        val data = AppData(
            tasks = listOf(
                Task("past", "گذشته", dueDay = e, dueMinute = 8 * 60),
                Task("done", "انجام‌شده", dueDay = e, dueMinute = 12 * 60, done = true),
                Task("soon", "مرور شیمی", dueDay = e, dueMinute = 15 * 60),
                Task("later", "بعدتر", dueDay = e + 2),
            ),
        )
        val (entry, day) = Planner.next(data, LocalDateTime.of(2026, 11, 10, 10, 0))!!
        assertEquals("مرور شیمی", entry.title)
        assertEquals(today, day)
        val (e2, d2) = Planner.next(data, LocalDateTime.of(2026, 11, 10, 16, 0))!!
        assertEquals("بعدتر", e2.title)
        assertEquals(today.plusDays(2), d2)
        assertNull(Planner.next(AppData(), LocalDateTime.of(2026, 11, 10, 16, 0)))
    }

    private val tasks = listOf(
        Task("1", "خرید کتاب", dueDay = e + 3, priority = Priority.LOW, categoryId = "personal", createdAt = 1),
        Task("2", "مطالعه فيزيك", dueDay = e, priority = Priority.HIGH, categoryId = "study", createdAt = 2),
        Task("3", "ورزش صبحگاهی", dueDay = e - 1, priority = Priority.MEDIUM, categoryId = "sport", createdAt = 3),
        Task("4", "پروژه", dueDay = null, categoryId = "project", createdAt = 4),
        Task("5", "انجام شده", dueDay = e, done = true, categoryId = "study", createdAt = 5),
    )

    @Test fun filters() {
        fun ids(f: TaskFilter) = TaskQuery.apply(tasks, f, TaskSort.DUE, null, "", e).map { it.id }
        assertEquals(listOf("3", "2"), ids(TaskFilter.TODAY)) // عقب‌افتاده هم دیده می‌شود
        assertEquals(listOf("1"), ids(TaskFilter.UPCOMING))
        assertEquals(listOf("5"), ids(TaskFilter.DONE))
        assertEquals(5, ids(TaskFilter.ALL).size)
        assertEquals("5", ids(TaskFilter.ALL).last()) // انجام‌شده‌ها پایین
    }

    @Test fun sortAndCategoryAndSearch() {
        val byPri = TaskQuery.apply(tasks, TaskFilter.ALL, TaskSort.PRIORITY, null, "", e).map { it.id }
        assertEquals("2", byPri.first())
        val newest = TaskQuery.apply(tasks, TaskFilter.ALL, TaskSort.NEWEST, null, "", e).map { it.id }
        assertEquals("4", newest.first())
        assertEquals(listOf("2"), TaskQuery.apply(tasks, TaskFilter.ALL, TaskSort.DUE, "study", "", e).map { it.id }.filter { it != "5" })
        // جست‌وجو با «ی/ک» فارسی و عربی یکسان است
        assertEquals(listOf("2"), TaskQuery.apply(tasks, TaskFilter.ALL, TaskSort.DUE, null, "فیزیک", e).map { it.id })
    }

    @Test fun todayProgress() {
        assertEquals(1 to 2, TaskQuery.todayProgress(tasks, e))
        assertEquals(0 to 0, TaskQuery.todayProgress(emptyList(), e))
    }

    @Test fun weekStartIsSaturday() {
        assertEquals(LocalDate.of(2026, 11, 7), JalaliDate.startOfWeek(today))
    }

    @Test fun ambientCatalogContainsRealLicensedRecordings() {
        val recordings = AmbientLibrary.recordings
        assertTrue("کتابخانه‌ی ضبط‌ها خالی نباشد", recordings.size >= 5)
        assertEquals("شناسه‌ها باید یکتا باشند", recordings.size, recordings.map { it.id }.toSet().size)
        recordings.forEach { recording ->
            assertTrue("${recording.id} لینک امن", recording.url.startsWith("https://"))
            assertTrue("${recording.id} صفحه‌ی منبع", recording.sourcePage.startsWith("https://"))
            assertTrue("${recording.id} مجوز", recording.licenseUrl.startsWith("https://"))
            assertTrue("${recording.id} خالق اثر", recording.creator.isNotBlank())
        }
        assertEquals(recordings.map { it.id }.toSet(), AmbientSynth.sounds.map { it.id }.toSet())
        assertNull("صدای مصنوعی کافه حذف شده", AmbientLibrary.byId("cafe"))
        assertNull("صدای مصنوعی کتابخانه حذف شده", AmbientLibrary.byId("library"))
    }

    @Test fun legacyDataMigratesWithoutLosingUserContent() {
        val directory = createTempDir(prefix = "matfocus-migration-")
        val file = File(directory, "flow_data.json")
        val legacy = AppData(
            version = 1,
            tasks = tasks,
            settings = Settings(ambientSound = "cafe"),
        )
        val store = JsonStore(file)
        store.save(legacy)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repository = FlowRepository(JsonStore(file), scope)
        assertEquals(2, repository.current.version)
        assertEquals("none", repository.current.settings.ambientSound)
        assertEquals(legacy.tasks, repository.current.tasks)
        assertEquals(legacy.sessions, repository.current.sessions)
        scope.cancel()
        directory.deleteRecursively()
    }

}
