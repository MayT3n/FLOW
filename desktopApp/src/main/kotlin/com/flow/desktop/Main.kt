package com.flow.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.DpSize

private val Ink = Color(0xFF10131B)
private val Panel = Color(0xFF1A1F2B)
private val Muted = Color(0xFF9AA4B5)
private val Accent = Color(0xFF8B9CFF)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MedFlow",
        state = WindowState(size = DpSize(1440.dp, 900.dp))
    ) {
        MedFlowDesktop()
    }
}

@Composable
private fun MedFlowDesktop() {
    var selected by remember { mutableStateOf("خانه") }
    var dark by remember { mutableStateOf(true) }
    val scheme = if (dark) darkColorScheme(
        background = Ink,
        surface = Panel,
        primary = Accent,
        onBackground = Color(0xFFF4F6FB),
        onSurface = Color(0xFFF4F6FB)
    ) else lightColorScheme()

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Sidebar(
                        selected = selected,
                        onSelect = { selected = it },
                        dark = dark,
                        onThemeChange = { dark = !dark }
                    )
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(selected, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                Text("فضای متمرکز برای مدیریت زمان و کارها", color = Muted, fontSize = 13.sp)
                            }
                            Text("نسخه دسکتاپ در حال آماده‌سازی", color = Muted, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        when (selected) {
                            "خانه" -> Dashboard()
                            "تمرکز" -> InfoPanel("تمرکز", "موتور پومودورو در مرحله اتصال به منطق مشترک قرار دارد.")
                            "وظایف" -> InfoPanel("وظایف", "مدیریت وظایف پس از انتقال امن مدل‌ها و مخزن داده فعال می‌شود.")
                            "برنامه‌ریزی" -> InfoPanel("برنامه‌ریزی", "تقویم و برنامه‌ریزی جلالی در مرحله مهاجرت قرار دارند.")
                            "آمار" -> InfoPanel("آمار", "نمودارها پس از اتصال داده‌های واقعی جلسات نمایش داده می‌شوند.")
                            "موسیقی" -> InfoPanel("موسیقی", "پخش‌کننده دسکتاپ باید مستقل از Android Media3 پیاده‌سازی شود.")
                            "تنظیمات" -> InfoPanel("تنظیمات", "تنظیمات دسکتاپ و موتور گرافیکی در حال طراحی هستند.")
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
    onThemeChange: () -> Unit
) {
    val items = listOf("خانه", "تمرکز", "وظایف", "برنامه‌ریزی", "آمار", "موسیقی", "تنظیمات")
    Column(
        Modifier.width(224.dp).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), RoundedCornerShape(26.dp))
            .border(1.dp, Color.White.copy(alpha = if (dark) 0.09f else 0.45f), RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(38.dp).height(38.dp)
                    .background(Accent.copy(alpha = 0.20f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { Text("M", color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("MedFlow", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("DESKTOP", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
        items.forEach { item ->
            val active = selected == item
            Surface(
                onClick = { onSelect(item) },
                color = if (active) Accent.copy(alpha = 0.16f) else Color.Transparent,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Text(
                    item,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    color = if (active) Accent else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(onClick = onThemeChange, color = Color.Transparent, shape = RoundedCornerShape(12.dp)) {
            Text(
                if (dark) "☀  تغییر به حالت روشن" else "☾  تغییر به حالت تیره",
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun Dashboard() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassPanel(Modifier.weight(1f), "وضعیت امروز", "هنوز جلسه‌ای برای امروز ثبت نشده است.", "۰ جلسه")
            GlassPanel(Modifier.weight(1f), "وظایف باز", "وظایف پس از اتصال مخزن داده نمایش داده می‌شوند.", "—")
            GlassPanel(Modifier.weight(1f), "زمان تمرکز", "آمار بر اساس جلسات واقعی محاسبه خواهد شد.", "۰ دقیقه")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1.5f)) {
                GlassPanel(Modifier.fillMaxWidth().height(300.dp), "شروع تمرکز", "رابط تایمر دسکتاپ در گام بعدی به PomodoroEngine متصل می‌شود.", "آماده")
            }
            Column(Modifier.weight(1f)) {
                GlassPanel(Modifier.fillMaxWidth().height(142.dp), "برنامه امروز", "هنوز داده‌ای بارگذاری نشده است.", "تقویم جلالی")
                GlassPanel(Modifier.fillMaxWidth().height(142.dp), "روتین‌ها", "روتین‌های ذخیره‌شده پس از مهاجرت نمایش داده می‌شوند.", "—")
            }
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    value: String
) {
    Column(
        modifier.background(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            RoundedCornerShape(22.dp)
        ).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Muted, fontSize = 13.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun InfoPanel(title: String, description: String) {
    GlassPanel(Modifier.fillMaxWidth().height(240.dp), title, description, "در حال توسعه")
}
