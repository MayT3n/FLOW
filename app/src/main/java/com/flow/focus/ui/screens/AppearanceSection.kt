package com.flow.focus.ui.screens

import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.flow.focus.data.GlassMaterial
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.Settings
import com.flow.focus.data.ThemeMode
import com.flow.focus.data.WallpaperFit
import com.flow.focus.data.WallpaperKind
import com.flow.focus.data.WallpaperStore
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.FlowSlider
import com.flow.focus.ui.components.GlassButton
import com.flow.focus.ui.components.SegmentedControl
import com.flow.focus.ui.components.SettingRow
import com.flow.focus.ui.components.SwitchRow
import com.flow.focus.ui.components.num
import com.flow.focus.ui.glass.BuiltinWallpaperNames
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassPreviewHost
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.drawFlowBackground
import com.flow.focus.ui.glass.renderBuiltinWallpaper
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val themeModes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)

@Composable
private fun Pct(v: Float) = Text(num("${(v * 100).toInt()}%"), style = Flow.type.caption, color = Flow.colors.textSecondary)

/** بخش‌های «ظاهر»، «پس‌زمینه» و «Liquid Glass» در تنظیمات؛ پیش‌نمایش همان موتور رندر اصلی است. */
@Composable
fun AppearanceSections(vm: AppViewModel, st: Settings) {
    val ctx = LocalContext.current
    val c = Flow.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importWallpaper(uri) { ok ->
            if (!ok) Toast.makeText(ctx, "تصویر انتخاب‌شده قابل استفاده نبود", Toast.LENGTH_SHORT).show()
        }
    }
    val hasImage = st.wallpaperKind != WallpaperKind.GRADIENT

    Section("ظاهر") {
        Text("تم", style = Flow.type.body, color = c.textPrimary)
        SegmentedControl(listOf("روشن", "تاریک", "سیستم"), themeModes.indexOf(st.theme), { vm.updateSettings { s -> s.copy(theme = themeModes[it]) } })
        Text("اعداد", style = Flow.type.body, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp))
        SegmentedControl(listOf("فارسی ۱۲۳", "انگلیسی 123"), if (st.persianDigits) 0 else 1, { vm.updateSettings { s -> s.copy(persianDigits = it == 0) } })
        SwitchRow("انیمیشن‌ها", st.animations, { v -> vm.updateSettings { it.copy(animations = v) } }, subtitle = "خاموش کردن، مصرف باتری را کم می‌کند")
    }

    Section("پس‌زمینه") {
        // پیش‌نمایش زنده: همان GlassSurface و همان بک‌دراپ برنامه، در یک قاب کوچک
        GlassPreviewHost(Modifier.fillMaxWidth().height(170.dp)) {
            Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassSurface(Modifier.size(100.dp), shape = CircleShape) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(num("25:00"), style = Flow.type.heading, color = c.textPrimary)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassSurface(Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp)) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text("کارت نمونه", style = Flow.type.bodyStrong, color = c.textPrimary)
                        }
                    }
                    GlassSurface(Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp), level = GlassLevel.Strong) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text("پنل پررنگ", style = Flow.type.bodyStrong, color = c.textPrimary)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WallThumb("پیش‌فرض", st.wallpaperKind == WallpaperKind.GRADIENT, { vm.updateSettings { it.copy(wallpaperKind = WallpaperKind.GRADIENT) } }) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawFlowBackground(c, 0f, size.width, size.height) }
            }
            BuiltinWallpaperNames.forEachIndexed { i, name ->
                val thumb = remember(i) { renderBuiltinWallpaper(i, 72, 126).asImageBitmap() }
                WallThumb(name, st.wallpaperKind == WallpaperKind.BUILTIN && st.wallpaperBuiltin == i, {
                    vm.updateSettings { it.copy(wallpaperKind = WallpaperKind.BUILTIN, wallpaperBuiltin = i) }
                }) { ImageFill(thumb) }
            }
            if (st.wallpaperRev != 0L) {
                val custom by produceState<ImageBitmap?>(null, st.wallpaperRev) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            val o = BitmapFactory.Options().apply { inSampleSize = 8 }
                            BitmapFactory.decodeFile(WallpaperStore(ctx).fileFor(st.wallpaperRev).path, o)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
                WallThumb("تصویر من", st.wallpaperKind == WallpaperKind.CUSTOM, { vm.updateSettings { it.copy(wallpaperKind = WallpaperKind.CUSTOM) } }) {
                    custom?.let { ImageFill(it) }
                }
            }
            WallThumb("از گالری", false, { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { FlowIcon(FlowIcons.Plus, tint = c.textSecondary, size = 26.dp) }
            }
        }

        if (hasImage) {
            Text("نحوه‌ی نمایش", style = Flow.type.body, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp))
            SegmentedControl(listOf("پر کردن (Crop)", "متناسب (Fit)"), st.wallpaperFit.ordinal, { vm.updateSettings { s -> s.copy(wallpaperFit = WallpaperFit.entries[it]) } })
            SettingRow("روشنایی") { Pct(st.wallpaperBrightness) }
            FlowSlider(st.wallpaperBrightness, { v -> vm.updateSettings { it.copy(wallpaperBrightness = v) } }, -0.6f..0.6f)
            SettingRow("اشباع رنگ") { Pct(st.wallpaperSaturation) }
            FlowSlider(st.wallpaperSaturation, { v -> vm.updateSettings { it.copy(wallpaperSaturation = v) } }, 0f..2f)
            SettingRow("تاری پس‌زمینه") { Pct(st.wallpaperBlur / 100f) }
            FlowSlider(st.wallpaperBlur.toFloat(), { v -> vm.updateSettings { it.copy(wallpaperBlur = v.toInt()) } }, 0f..100f)
        }
        GlassButton("بازگشت به پس‌زمینه‌ی پیش‌فرض", { vm.resetWallpaper() }, Modifier.fillMaxWidth().padding(top = 6.dp), icon = FlowIcons.Reset)
    }

    Section("Liquid Glass") {
        Text("جنس شیشه", style = Flow.type.body, color = c.textPrimary)
        SegmentedControl(listOf("کریستال", "مات", "مایع"), st.glassMaterial.ordinal, { vm.updateSettings { s -> s.copy(glassMaterial = GlassMaterial.entries[it]) } })
        Text(
            when (st.glassMaterial) {
                GlassMaterial.CRYSTAL -> "شفاف و بازتابنده."
                GlassMaterial.FROSTED -> "نرم و پخش‌شده."
                GlassMaterial.LIQUID -> "شکست نور قوی‌تر و حس ضخامت بیشتر."
            },
            style = Flow.type.caption, color = c.textSecondary,
        )
        Text("کیفیت", style = Flow.type.body, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp))
        SegmentedControl(listOf("کم", "متوسط", "بالا"), st.glassQuality.ordinal, { vm.updateSettings { s -> s.copy(glassQuality = GlassQuality.entries[it]) } })
        Text(
            when (st.glassQuality) {
                GlassQuality.LOW -> "سطح ساده و کم‌مصرف، بدون نمونه‌برداری از پس‌زمینه."
                GlassQuality.MEDIUM -> "پس‌زمینه‌ی واقعی با تاری انتخابی، لبه‌ی نورانی و سایه‌ی درونی (اندروید ۱۲ به بالا)."
                GlassQuality.HIGH ->
                    if (Build.VERSION.SDK_INT >= 33) "رندر شیدری: شکست نور، بازتاب و عمق واقعی."
                    else "شیدر نوری به اندروید ۱۳ نیاز دارد؛ در این دستگاه تاری و جلوه‌های نوری اجرا می‌شود."
            },
            style = Flow.type.caption, color = c.textSecondary,
        )
        // شفافیت = معکوس غلظت ذخیره‌شده (سازگار با داده‌ی قبلی: glassOpacity)
        val transparency = ((1.6f - st.glassOpacity) / 1.2f).coerceIn(0f, 1f)
        SettingRow("شفافیت") { Pct(transparency) }
        FlowSlider(transparency, { v -> vm.updateSettings { it.copy(glassOpacity = 1.6f - 1.2f * v) } }, 0f..1f)
        if (st.glassQuality != GlassQuality.LOW) {
            SettingRow("درخشش") { Pct(st.glassShine) }
            FlowSlider(st.glassShine, { v -> vm.updateSettings { it.copy(glassShine = v) } }, 0f..1f)
            SettingRow("بازتاب") { Pct(st.glassReflection) }
            FlowSlider(st.glassReflection, { v -> vm.updateSettings { it.copy(glassReflection = v) } }, 0f..1f)
            SettingRow("شدت تاری") { Text(num("${st.glassBlurDp}"), style = Flow.type.caption, color = c.textSecondary) }
            FlowSlider(st.glassBlurDp.toFloat(), { v -> vm.updateSettings { it.copy(glassBlurDp = v.toInt()) } }, 4f..48f)
        }
        if (st.glassQuality == GlassQuality.HIGH) {
            SettingRow("شکست نور") { Pct(st.glassRefraction) }
            FlowSlider(st.glassRefraction, { v -> vm.updateSettings { it.copy(glassRefraction = v) } }, 0f..1f)
        }
    }
}

@Composable
private fun ImageFill(img: ImageBitmap) {
    Canvas(Modifier.fillMaxSize()) {
        val s = maxOf(size.width / img.width, size.height / img.height)
        val dw = img.width * s
        val dh = img.height * s
        drawImage(
            img, srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(((size.width - dw) / 2f).toInt(), ((size.height - dh) / 2f).toInt()),
            dstSize = IntSize(dw.toInt() + 1, dh.toInt() + 1),
        )
    }
}

@Composable
private fun WallThumb(label: String, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = Flow.colors
    val shape = RoundedCornerShape(16.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(width = 62.dp, height = 108.dp)
                .springClick(scaleDown = 0.95f, onClick = onClick)
                .clip(shape)
                .border(if (selected) 2.5.dp else 1.dp, if (selected) c.accent else c.track, shape),
        ) { content() }
        Text(label, style = Flow.type.caption, color = if (selected) c.accent else c.textSecondary, maxLines = 1)
    }
}
