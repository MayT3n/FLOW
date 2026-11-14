package com.flow.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.focus.data.AppData
import com.flow.focus.ui.AppViewModel
import com.flow.focus.ui.components.DockItem
import com.flow.focus.ui.components.FlowDock
import com.flow.focus.ui.components.FlowIcons
import com.flow.focus.ui.glass.GlassBackdropHost
import com.flow.focus.ui.screens.HomeScreen
import com.flow.focus.ui.screens.MiniMusicPlayer
import com.flow.focus.ui.screens.MusicChip
import com.flow.focus.ui.screens.MusicPlayerDialog
import com.flow.focus.ui.screens.PlanningScreen
import com.flow.focus.ui.screens.RoutinesScreen
import com.flow.focus.ui.screens.SettingsScreen
import com.flow.focus.ui.screens.StatsScreen
import com.flow.focus.ui.motion.Motion
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.FlowTheme
import com.flow.focus.ui.theme.LocalDockContentPadding

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FlowRoot(vm) }
    }

    override fun onResume() {
        super.onResume()
        // اگر زمان فاز در پس‌زمینه تمام شده، همین‌جا بسته و فاز بعد آماده می‌شود.
        vm.onResume()
        vm.refreshRemoteContent()
    }
}

private val dockItems = listOf(
    DockItem("خانه", FlowIcons.DockHome, FlowIcons.DockHomeActive),
    DockItem("برنامه‌ریزی", FlowIcons.DockPlan, FlowIcons.DockPlanActive),
    DockItem("روتین‌ها", FlowIcons.DockRoutine, FlowIcons.DockRoutineActive),
    DockItem("آمار", FlowIcons.DockStats, FlowIcons.DockStatsActive),
    DockItem("تنظیمات", FlowIcons.DockSettings, FlowIcons.DockSettingsActive),
)

@Composable
fun FlowRoot(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    FlowTheme(data.settings) {
        SystemBars(Flow.colors.isDark)
        RequestNotificationPermission(data.settings.notifications) { vm.maybeWelcome() }
        AppScaffold(vm, data)
    }
}

@Composable
private fun SystemBars(dark: Boolean) {
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
        )
        onDispose { }
    }
}

@Composable
private fun RequestNotificationPermission(wanted: Boolean, onReady: () -> Unit) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) onReady() }
    LaunchedEffect(wanted) {
        if (!wanted) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            kotlinx.coroutines.delay(1200)
            onReady()
        }
    }
}

@Composable
private fun AppScaffold(vm: AppViewModel, data: AppData) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var musicOpen by rememberSaveable { mutableStateOf(false) }
    var miniCollapsed by rememberSaveable { mutableStateOf(false) }
    val music by vm.musicState.collectAsStateWithLifecycle()
    var dockAreaHeightPx by rememberSaveable { mutableStateOf(0) }
    val contentBottomPadding = with(LocalDensity.current) {
        if (dockAreaHeightPx > 0) dockAreaHeightPx.toDp() + 20.dp else 140.dp
    }
    val animations = data.settings.animations
    val tabStateHolder = rememberSaveableStateHolder()
    BackHandler(enabled = musicOpen || tab != 0) {
        if (musicOpen) musicOpen = false else tab = 0
    }

    GlassBackdropHost(
        data.settings,
        source = {
        CompositionLocalProvider(LocalDockContentPadding provides contentBottomPadding) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (animations) {
                    val direction = if (targetState > initialState) -1 else 1
                    (fadeIn(Motion.enter(Motion.Medium)) + slideInHorizontally(Motion.enter(Motion.Long)) { it * direction / 18 }) togetherWith
                        (fadeOut(Motion.exit(Motion.Short)) + slideOutHorizontally(Motion.exit(Motion.Medium)) { -it * direction / 18 })
                } else {
                    ContentTransform(EnterTransition.None, ExitTransition.None)
                }
            },
            label = "tabs",
        ) { t ->
            // هر تب state خودش (از جمله موقعیت اسکرول) را هنگام خروج ذخیره و هنگام بازگشت بازمی‌گرداند.
            tabStateHolder.SaveableStateProvider(t) {
                when (t) {
                    0 -> HomeScreen(vm, data, onNavigate = { tab = it }, onOpenMusic = { musicOpen = true })
                    1 -> PlanningScreen(vm, data, onStartFocus = { tab = 0 })
                    2 -> RoutinesScreen(vm, data)
                    3 -> StatsScreen(data)
                    else -> SettingsScreen(vm, data)
                }
            }
        }
        if (musicOpen) {
            MusicPlayerDialog(vm, onMinimize = { musicOpen = false; miniCollapsed = true }) { musicOpen = false }
        }
        }
        },
        chrome = {
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { dockAreaHeightPx = it.height }
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val hasTrack = music.connected && music.currentTrackId != null
                // سه حالت: نوار مینی (پیش‌فرض با آهنگ فعال)، دایره‌ی کوچک‌شده، و پلیر کامل.
                AnimatedContent(
                    targetState = if (hasTrack && !miniCollapsed) 0 else 1,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        (fadeIn(Motion.enter(Motion.Medium)) + scaleIn(Motion.enter(Motion.Long), initialScale = 0.88f)) togetherWith
                            (fadeOut(Motion.exit(Motion.Short)) + scaleOut(Motion.exit(Motion.Short), targetScale = 0.9f))
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "mini-music",
                ) { mode ->
                    if (mode == 0) {
                        MiniMusicPlayer(vm, music, onOpen = { musicOpen = true }, onCollapse = { miniCollapsed = true })
                    } else {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            MusicChip(vm, music, onClick = { if (hasTrack) miniCollapsed = false else musicOpen = true })
                        }
                    }
                }
                FlowDock(dockItems, tab, { tab = it }, Modifier.fillMaxWidth())
            }
        },
    )
}
