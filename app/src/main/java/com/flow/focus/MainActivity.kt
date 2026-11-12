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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import com.flow.focus.ui.screens.MusicShelfRestoreButton
import com.flow.focus.ui.screens.MusicPlayerDialog
import com.flow.focus.ui.screens.PlanningScreen
import com.flow.focus.ui.screens.RoutinesScreen
import com.flow.focus.ui.screens.SettingsScreen
import com.flow.focus.ui.screens.StatsScreen
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
    DockItem("خانه", FlowIcons.Home),
    DockItem("برنامه", FlowIcons.Calendar),
    DockItem("روتین‌ها", FlowIcons.Repeat),
    DockItem("آمار", FlowIcons.Stats),
    DockItem("تنظیمات", FlowIcons.Settings),
)

@Composable
fun FlowRoot(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    FlowTheme(data.settings) {
        SystemBars(Flow.colors.isDark)
        RequestNotificationPermission(data.settings.notifications)
        GlassBackdropHost(data.settings) {
            AppScaffold(vm, data)
        }
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
private fun RequestNotificationPermission(wanted: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(wanted) {
        if (wanted && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun AppScaffold(vm: AppViewModel, data: AppData) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var musicOpen by rememberSaveable { mutableStateOf(false) }
    var musicShelfExpanded by rememberSaveable { mutableStateOf(false) }
    var dockAreaHeightPx by rememberSaveable { mutableStateOf(0) }
    val contentBottomPadding = with(LocalDensity.current) {
        if (dockAreaHeightPx > 0) dockAreaHeightPx.toDp() + 20.dp else 140.dp
    }
    val music by vm.musicSnapshot.collectAsStateWithLifecycle()
    val animations = data.settings.animations
    LaunchedEffect(music.currentTrackId, music.isPlaying) {
        if (music.currentTrackId != null && music.isPlaying) musicShelfExpanded = true
    }
    val hasSelectedTrack = music.connected && music.currentTrackId != null
    BackHandler(enabled = musicOpen) { musicOpen = false }
    BackHandler(enabled = tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalDockContentPadding provides contentBottomPadding) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (animations) {
                    val direction = if (targetState > initialState) -1 else 1
                    (fadeIn(tween(210)) + slideInHorizontally(tween(260)) { it * direction / 18 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { -it * direction / 18 })
                } else {
                    ContentTransform(EnterTransition.None, ExitTransition.None)
                }
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> HomeScreen(vm, data, onNavigate = { tab = it })
                1 -> PlanningScreen(vm, data)
                2 -> RoutinesScreen(vm, data)
                3 -> StatsScreen(data)
                else -> SettingsScreen(vm, data)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .onSizeChanged { dockAreaHeightPx = it.height }
                .navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)
                .pointerInput(musicShelfExpanded) {
                    var dragDistance = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, amount -> dragDistance += amount },
                        onDragEnd = {
                            if (dragDistance > 42f) musicShelfExpanded = false
                            if (dragDistance < -42f) musicShelfExpanded = true
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                },
            verticalArrangement = Arrangement.spacedBy(9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = hasSelectedTrack && musicShelfExpanded,
                enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 2 },
                exit = fadeOut(tween(110)) + slideOutVertically(tween(170)) { it / 2 },
            ) {
                MiniMusicPlayer(
                    vm,
                    music,
                    onOpen = { musicOpen = true },
                    onCollapse = { musicShelfExpanded = false },
                )
            }
            AnimatedVisibility(
                visible = hasSelectedTrack && !musicShelfExpanded,
                enter = fadeIn(tween(140)) + slideInVertically(tween(180)) { it / 2 },
                exit = fadeOut(tween(100)) + slideOutVertically(tween(140)) { it / 2 },
            ) {
                MusicShelfRestoreButton { musicShelfExpanded = true }
            }
            FlowDock(dockItems, tab, { tab = it }, Modifier.fillMaxWidth())
        }
        if (musicOpen) MusicPlayerDialog(vm) { musicOpen = false }
        }
    }
}
