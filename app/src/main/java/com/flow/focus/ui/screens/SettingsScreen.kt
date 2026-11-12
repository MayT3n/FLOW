package com.flow.focus.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import android.graphics.BitmapFactory
import android.os.Build
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.BuildConfig
import com.flow.focus.data.AppData
import com.flow.focus.data.GlassQuality
import com.flow.focus.data.ThemeMode
import com.flow.focus.data.WallpaperFit
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.FlowDialog
import com.flow.focus.ui.components.FlowIcon
import com.flow.focus.ui.components.GlassIconButton
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
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.LocalRootBackdrop
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.springClick
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalDockContentPadding
import com.flow.focus.ui.theme.LocalGlass
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun AnimatedSettingsSection(
    visible: Boolean,
    title: String,
    content: @Composable () -> Unit,
) {
    val animations = LocalGlass.current.animations
    AnimatedVisibility(
        visible = visible,
        enter = if (animations) fadeIn(tween(175)) + slideInVertically(tween(210)) { it / 16 } else EnterTransition.None,
        exit = if (animations) fadeOut(tween(105)) + slideOutVertically(tween(135)) { -it / 18 } else ExitTransition.None,
    ) {
        Section(title, content)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val icon = when (title) {
        "ظاهر" -> FlowIcons.Sun
        "پس‌زمینه" -> FlowIcons.Layers
        "کیفیت Liquid Glass" -> FlowIcons.Layers
        "پومودورو" -> FlowIcons.Clock
        "اعلان‌ها" -> FlowIcons.Bell
        "صدای محیط" -> FlowIcons.Music
        "مدیریت داده‌ها" -> FlowIcons.Export
        "موسیقی" -> FlowIcons.Music
        "شخصی‌سازی" -> FlowIcons.Sun
        "تمرکز" -> FlowIcons.Clock
        "داده و برنامه" -> FlowIcons.Layers
        else -> FlowIcons.Target
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 3.dp, end = 3.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(Flow.colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                FlowIcon(icon, tint = Flow.colors.accent, size = 17.dp)
            }
            Text(title, style = Flow.type.heading, color = Flow.colors.textPrimary)
        }
        GlassSurface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(19.dp),
            level = GlassLevel.Subtle,
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) { content() }
        }
    }
}

private fun readText(ctx: Context, uri: Uri): String? =
    runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } }.getOrNull()

private fun writeText(ctx: Context, uri: Uri, text: String): Boolean =
    runCatching { ctx.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) } != null }.getOrDefault(false)

@Composable
private fun WallpaperOption(
    id: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = when (id) {
        "aurora" -> listOf(Color(0xFF101B39), Color(0xFF514080), Color(0xFF132E4E))
        "midnight" -> listOf(Color(0xFF080F20), Color(0xFF1D2D45), Color(0xFF334B62))
        "dusk" -> listOf(Color(0xFF30213F), Color(0xFF76506D), Color(0xFF293B59))
        "gradient" -> listOf(Flow.colors.bgTop, Flow.colors.bgMid, Flow.colors.bgBottom)
        else -> listOf(Color(0xFF18233F), Color(0xFF34466B), Color(0xFF242C4D))
    }
    Box(
        modifier.height(76.dp).clip(RoundedCornerShape(17.dp))
            .background(Brush.verticalGradient(palette))
            .border(if (selected) 2.dp else 1.dp, if (selected) Flow.colors.accent else Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
            .semantics { this.selected = selected }
            .springClick(role = Role.RadioButton, scaleDown = 0.97f, onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 7.dp), style = Flow.type.caption, color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
private fun WallpaperPreviewSurface(
    id: String,
    path: String?,
    fit: WallpaperFit,
    dim: Float,
    blurDp: Int,
) {
    val bitmap = remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap.value = if (id == "custom" && !path.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        } else null
    }
    val palette = when (id) {
        "aurora" -> listOf(Color(0xFF07172B), Color(0xFF173B59), Color(0xFF382B65))
        "midnight" -> listOf(Color(0xFF070D1B), Color(0xFF172A42), Color(0xFF26364B))
        "dusk" -> listOf(Color(0xFF241B37), Color(0xFF69435E), Color(0xFF273D5C))
        "gradient" -> listOf(Flow.colors.bgTop, Flow.colors.bgMid, Flow.colors.bgBottom)
        else -> listOf(Color(0xFF18233F), Color(0xFF34466B), Color(0xFF242C4D))
    }
    Box(
        Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurDp > 0) {
                    val radius = blurDp.dp.toPx()
                    BlurEffect(radius, radius)
                } else null
            },
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(palette)))
            if (id == "custom" && bitmap.value != null) {
                Image(
                    bitmap = bitmap.value!!,
                    contentDescription = "پیش‌نمایش تصویر پس‌زمینه",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (fit == WallpaperFit.CROP) ContentScale.Crop else ContentScale.Fit,
                )
            }
            if (id != "gradient" && dim > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim.coerceIn(0f, 0.65f))))
            }
        }
        if (id == "custom" && bitmap.value == null) {
            Text(
                "در حال آماده‌سازی پیش‌نمایش…",
                Modifier.align(Alignment.Center),
                style = Flow.type.body,
                color = Color.White,
            )
        }
        Text(
            "MatFocus",
            Modifier.align(Alignment.BottomStart).padding(14.dp),
            style = Flow.type.title.copy(letterSpacing = 2.sp),
            color = Color.White,
        )
    }
}


@Composable
private fun SettingsDestination(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .springClick(scaleDown = 0.985f, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Flow.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            FlowIcon(icon, tint = Flow.colors.accent, size = 19.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = Flow.type.bodyStrong, color = Flow.colors.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(subtitle, style = Flow.type.caption, color = Flow.colors.textSecondary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (!value.isNullOrBlank()) {
            Text(value, style = Flow.type.caption, color = Flow.colors.textSecondary, modifier = Modifier.widthIn(max = 84.dp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        FlowIcon(FlowIcons.ChevronNext, tint = Flow.colors.textSecondary, size = 17.dp)
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData, modifier: Modifier = Modifier) {
    val st = data.settings
    val generatedWallpaperPalette = LocalRootBackdrop.current?.palette?.value.orEmpty()
    val ctx = LocalContext.current
    val playing by vm.ambientPlaying.collectAsStateWithLifecycle()
    val ambientProgress by vm.ambientDownloadProgress.collectAsStateWithLifecycle()
    var confirmHistory by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    var wallpaperPreviewId by remember { mutableStateOf<String?>(null) }
    var pendingWallpaperPath by remember { mutableStateOf<String?>(null) }
    var selectedPage by rememberSaveable { mutableStateOf("root") }
    BackHandler(enabled = selectedPage != "root") { selectedPage = "root" }

    // پیش‌شنوایی با خروج از صفحه قطع شود
    DisposableEffect(Unit) { onDispose { vm.stopAmbientPreview() } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = writeText(ctx, uri, vm.exportJson())
            Toast.makeText(ctx, if (ok) "نسخه‌ی پشتیبان ذخیره شد" else "ذخیره‌ی فایل ناموفق بود", Toast.LENGTH_SHORT).show()
        }
    }
    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            vm.importWallpaper(uri) { path ->
                if (path != null) {
                    pendingWallpaperPath?.let(vm::discardWallpaperPreview)
                    pendingWallpaperPath = path
                    wallpaperPreviewId = "custom"
                } else {
                    Toast.makeText(ctx, "خواندن تصویر ناموفق بود", Toast.LENGTH_SHORT).show()
                }
            }
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
            .padding(horizontal = 18.dp).padding(top = 8.dp, bottom = LocalDockContentPadding.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedVisibility(
            visible = selectedPage == "root",
            enter = if (LocalGlass.current.animations) fadeIn(tween(170)) + slideInVertically(tween(205)) { it / 18 } else EnterTransition.None,
            exit = if (LocalGlass.current.animations) fadeOut(tween(110)) + slideOutVertically(tween(140)) { -it / 18 } else ExitTransition.None,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("تنظیمات")
            Section("شخصی‌سازی") {
                SettingsDestination(
                    "ظاهر و نمایش", "تم روشن، تاریک و شیوه‌ی نمایش اعداد",
                    FlowIcons.Sun,
                    when (st.theme) { ThemeMode.LIGHT -> "روشن"; ThemeMode.DARK -> "تاریک"; ThemeMode.SYSTEM -> "سیستم" },
                ) { selectedPage = "appearance" }
                SettingsDestination(
                    "پس‌زمینه", "تصویر، جای‌گیری و تنظیمات والپیپر",
                    FlowIcons.Layers, if (st.wallpaperId == "custom") "شخصی" else "آماده",
                ) { selectedPage = "wallpaper" }
                SettingsDestination(
                    "Liquid Glass", "کیفیت، شفافیت، تاری و درخشش",
                    FlowIcons.Layers, when (st.glassQuality) { GlassQuality.LOW -> "کم"; GlassQuality.MEDIUM -> "متوسط"; GlassQuality.HIGH -> "بالا" },
                ) { selectedPage = "glass" }
            }
            Section("تمرکز") {
                SettingsDestination(
                    "پومودورو", "مدت تمرکز، استراحت و هدف روزانه",
                    FlowIcons.Clock, num("${st.focusMin} دقیقه"),
                ) { selectedPage = "pomodoro" }
                SettingsDestination(
                    "اعلان‌ها", "یادآوری‌ها، صدا و لرزش",
                    FlowIcons.Bell, if (st.notifications) "روشن" else "خاموش",
                ) { selectedPage = "notifications" }
                SettingsDestination(
                    "صدای محیط", "انتخاب صدا و تنظیم پخش خودکار",
                    FlowIcons.Music,
                ) { selectedPage = "ambient" }
            }
            Section("داده و برنامه") {
                SettingsDestination(
                    "مدیریت داده‌ها", "پشتیبان‌گیری، بازیابی و پاک‌سازی",
                    FlowIcons.Export, num("${data.tasks.size} وظیفه"),
                ) { selectedPage = "data" }
                SettingsDestination(
                    "درباره‌ی MatFocus", "نسخه و اطلاعات سازنده",
                    FlowIcons.Target, "نسخه ${BuildConfig.VERSION_NAME}",
                ) { selectedPage = "about" }
            }
            }
        }
        AnimatedVisibility(
            visible = selectedPage != "root",
            enter = if (LocalGlass.current.animations) fadeIn(tween(165)) + slideInVertically(tween(200)) { it / 18 } else EnterTransition.None,
            exit = if (LocalGlass.current.animations) fadeOut(tween(105)) + slideOutVertically(tween(135)) { -it / 18 } else ExitTransition.None,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val pageTitle = when (selectedPage) {
                "appearance" -> "ظاهر و نمایش"
                "wallpaper" -> "پس‌زمینه"
                "glass" -> "Liquid Glass"
                "pomodoro" -> "پومودورو"
                "notifications" -> "اعلان‌ها"
                "ambient" -> "صدای محیط"
                "data" -> "مدیریت داده‌ها"
                else -> "درباره‌ی MatFocus"
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassIconButton(FlowIcons.ChevronPrev, { selectedPage = "root" }, size = 40.dp, iconSize = 19.dp, contentDescription = "بازگشت")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(pageTitle, style = Flow.type.title, color = Flow.colors.textPrimary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text("تنظیمات MatFocus", style = Flow.type.caption, color = Flow.colors.textSecondary)
                }
            }
            }
        }

        AnimatedSettingsSection(selectedPage == "appearance", "ظاهر") {
            Text("تم", style = Flow.type.body, color = Flow.colors.textPrimary)
            SegmentedControl(listOf("روشن", "تاریک", "سیستم"), listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).indexOf(st.theme), {
                vm.updateSettings { s -> s.copy(theme = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[it]) }
            })
            Text("اعداد", style = Flow.type.body, color = Flow.colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
            SegmentedControl(listOf("فارسی ۱۲۳", "انگلیسی 123"), if (st.persianDigits) 0 else 1, { vm.updateSettings { s -> s.copy(persianDigits = it == 0) } })
            SwitchRow("انیمیشن‌ها", st.animations, { v -> vm.updateSettings { it.copy(animations = v) } }, subtitle = "خاموش کردن، مصرف باتری را کم می‌کند")
        }

        AnimatedSettingsSection(selectedPage == "wallpaper", "پس‌زمینه") {
            Text("انتخاب پس‌زمینه", style = Flow.type.body, color = Flow.colors.textPrimary)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WallpaperOption("gradient", "اصلی", st.wallpaperId == "gradient", Modifier.width(70.dp)) { wallpaperPreviewId = "gradient" }
                WallpaperOption("aurora", "شفق", st.wallpaperId == "aurora", Modifier.width(70.dp)) { wallpaperPreviewId = "aurora" }
                WallpaperOption("midnight", "نیمه‌شب", st.wallpaperId == "midnight", Modifier.width(70.dp)) { wallpaperPreviewId = "midnight" }
                WallpaperOption("dusk", "غروب", st.wallpaperId == "dusk", Modifier.width(70.dp)) { wallpaperPreviewId = "dusk" }
                if (!st.wallpaperPath.isNullOrBlank()) {
                    WallpaperOption("custom", "شخصی", st.wallpaperId == "custom", Modifier.width(70.dp)) { wallpaperPreviewId = "custom" }
                }
            }
            GlassButton(
                "انتخاب تصویر از گالری",
                { wallpaperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                Modifier.fillMaxWidth(),
                icon = FlowIcons.Export,
            )
            if (st.wallpaperId == "custom") {
                Text(
                    if (st.wallpaperPath.isNullOrBlank()) "تصویر شخصی در دسترس نیست؛ پس‌زمینه‌ی اصلی نمایش داده می‌شود."
                    else "تصویر شخصی فعال است. نسخه‌ی بهینه‌شده در فضای خصوصی برنامه نگهداری می‌شود.",
                    style = Flow.type.caption, color = Flow.colors.textSecondary,
                )
            }
            Text("جای‌گیری تصویر", style = Flow.type.body, color = Flow.colors.textPrimary)
            SegmentedControl(listOf("برش", "جا دادن"), if (st.wallpaperFit == WallpaperFit.CROP) 0 else 1, { index ->
                vm.updateSettings { current -> current.copy(wallpaperFit = if (index == 0) WallpaperFit.CROP else WallpaperFit.FIT) }
            })
            SettingRow("تاریک‌سازی تصویر") {
                Text(num("${(st.wallpaperDim * 100).toInt()}%"), style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            FlowSlider(st.wallpaperDim, { v -> vm.updateSettings { it.copy(wallpaperDim = v) } }, 0f..0.5f)
            SettingRow("تاری پس‌زمینه") {
                Text(num("${st.wallpaperBlurDp}"), style = Flow.type.caption, color = Flow.colors.textSecondary)
            }
            FlowSlider(st.wallpaperBlurDp.toFloat(), { v -> vm.updateSettings { it.copy(wallpaperBlurDp = v.toInt()) } }, 0f..24f)
            Text("پالت رنگی استخراج‌شده از والپیپر", style = Flow.type.bodyStrong, color = Flow.colors.textPrimary)
            if (generatedWallpaperPalette.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    generatedWallpaperPalette.take(3).forEach { swatch ->
                        Box(
                            Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(12.dp))
                                .background(swatch).border(1.dp, Flow.colors.glassEdge.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }
            Text(
                "Liquid Glass از رنگ‌های تصویر برای هماهنگی استفاده می‌کند؛ در تم روشن، لایه‌ی روشن‌تر برای خوانایی متن حفظ می‌شود.",
                style = Flow.type.caption,
                color = Flow.colors.textSecondary,
            )
            GlassButton("بازنشانی پس‌زمینه", { vm.selectWallpaper("gradient") }, Modifier.fillMaxWidth())
        }

        AnimatedSettingsSection(selectedPage == "glass", "کیفیت Liquid Glass") {
            SegmentedControl(listOf("کم", "متوسط", "بالا"), st.glassQuality.ordinal, { vm.updateSettings { s -> s.copy(glassQuality = GlassQuality.entries[it]) } })
            Text(
                when (st.glassQuality) {
                    GlassQuality.LOW -> "سطح ساده و کم‌مصرف؛ مناسب دستگاه‌های ضعیف."
                    GlassQuality.MEDIUM -> "تاری ملایم پس‌زمینه همراه با شفافیت و لبه‌ی نورانی (اندروید ۱۲ به بالا)."
                    GlassQuality.HIGH -> "تاری قوی‌تر پس‌زمینه و عمق بیشتر؛ مصرف گرافیکی بالاتر (اندروید ۱۲ به بالا)."
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

        AnimatedSettingsSection(selectedPage == "pomodoro", "پومودورو") {
            SettingRow("مدت تمرکز") { Stepper(st.focusMin, { v -> vm.updateSettings { it.copy(focusMin = v) } }, 1..180, suffix = "دقیقه") }
            SettingRow("استراحت کوتاه") { Stepper(st.shortBreakMin, { v -> vm.updateSettings { it.copy(shortBreakMin = v) } }, 1..60, suffix = "دقیقه") }
            SettingRow("استراحت بلند") { Stepper(st.longBreakMin, { v -> vm.updateSettings { it.copy(longBreakMin = v) } }, 1..90, suffix = "دقیقه") }
            SettingRow("تعداد دور هر چرخه") { Stepper(st.rounds, { v -> vm.updateSettings { it.copy(rounds = v) } }, 1..12) }
            SettingRow("هدف روزانه") { Stepper(st.dailyGoalMin, { v -> vm.updateSettings { it.copy(dailyGoalMin = v) } }, 10..720, step = 10, suffix = "دقیقه") }
            SwitchRow("شروع خودکار استراحت", st.autoStartBreak, { v -> vm.updateSettings { it.copy(autoStartBreak = v) } })
            SwitchRow("شروع خودکار جلسه‌ی بعد", st.autoStartFocus, { v -> vm.updateSettings { it.copy(autoStartFocus = v) } })
        }

        AnimatedSettingsSection(selectedPage == "notifications", "اعلان‌ها") {
            SwitchRow("اعلان‌ها", st.notifications, { v -> vm.updateSettings { it.copy(notifications = v) } }, subtitle = "پایان جلسه، تایمر در حال اجرا و یادآوری وظایف")
            SwitchRow("صدا", st.sound, { v -> vm.updateSettings { it.copy(sound = v) } })
            SwitchRow("لرزش", st.vibration, { v -> vm.updateSettings { it.copy(vibration = v) } })
        }

        AnimatedSettingsSection(selectedPage == "ambient", "صدای محیط") {
            AmbientPicker(
                settings = st,
                playingId = playing,
                onSelect = { id -> vm.updateSettings { it.copy(ambientSound = id) } },
                onPreview = { vm.previewAmbient(it) },
                onVolume = { v -> vm.updateSettings { it.copy(ambientVolume = v) } },
                onAuto = { v -> vm.updateSettings { it.copy(ambientAuto = v) } },
                isDownloaded = vm::isAmbientDownloaded,
                downloadProgress = ambientProgress,
                downloadedBytes = vm.downloadedAmbientBytes(),
                onDownload = vm::downloadAmbient,
                onDeleteDownload = { id -> vm.deleteAmbientDownload(id) },
            )
        }

        AnimatedSettingsSection(selectedPage == "data", "مدیریت داده‌ها") {
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

        AnimatedSettingsSection(selectedPage == "about", "درباره") {
            SettingRow("MatFocus", subtitle = "تمرکز کن، جریان بساز.") { Text(num("نسخه ${BuildConfig.VERSION_NAME}"), style = Flow.type.caption, color = Flow.colors.textSecondary) }
            Text(
                "ساخته شده توسط مت زم",
                Modifier.fillMaxWidth().padding(top = 2.dp),
                style = Flow.type.caption,
                color = Flow.colors.textSecondary,
            )
        }
        }

    wallpaperPreviewId?.let { previewId ->
        FlowDialog(
            onDismiss = {
                if (previewId == "custom") pendingWallpaperPath?.let(vm::discardWallpaperPreview)
                pendingWallpaperPath = null
                wallpaperPreviewId = null
            },
            title = "پیش‌نمایش پس‌زمینه",
        ) {
            WallpaperPreviewSurface(
                previewId,
                if (previewId == "custom") pendingWallpaperPath ?: st.wallpaperPath else null,
                st.wallpaperFit,
                st.wallpaperDim,
                st.wallpaperBlurDp,
            )
            Text(
                when (previewId) {
                    "gradient" -> "پس‌زمینه‌ی گرادیانی اصلی MatFocus"
                    "aurora" -> "طیف آبی و بنفش با نور نرم"
                    "midnight" -> "رنگ‌های تیره و آرام"
                    "dusk" -> "ترکیب گرم غروب و آبی تیره"
                    else -> "تصویر انتخاب‌شده از گالری"
                },
                style = Flow.type.caption,
                color = Flow.colors.textSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    "اعمال پس‌زمینه",
                    {
                        val applied = if (previewId == "custom") {
                            (pendingWallpaperPath ?: st.wallpaperPath)?.let(vm::applyCustomWallpaper) == true
                        } else {
                            vm.selectWallpaper(previewId)
                            true
                        }
                        if (applied) {
                            Toast.makeText(ctx, "پس‌زمینه اعمال شد", Toast.LENGTH_SHORT).show()
                            pendingWallpaperPath = null
                            wallpaperPreviewId = null
                        } else {
                            Toast.makeText(ctx, "اعمال تصویر ناموفق بود؛ فایل در دسترس نیست", Toast.LENGTH_SHORT).show()
                        }
                    },
                    Modifier.weight(1f),
                    enabled = previewId != "custom" || pendingWallpaperPath != null || !st.wallpaperPath.isNullOrBlank(),
                )
                GlassButton(
                    "انصراف",
                    {
                        if (previewId == "custom") pendingWallpaperPath?.let(vm::discardWallpaperPreview)
                        pendingWallpaperPath = null
                        wallpaperPreviewId = null
                    },
                    Modifier.weight(1f),
                )
            }
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
