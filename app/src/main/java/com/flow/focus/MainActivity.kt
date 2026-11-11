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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.flow.focus.ui.screens.PlanningScreen
import com.flow.focus.ui.screens.SettingsScreen
import com.flow.focus.ui.screens.StatsScreen
import com.flow.focus.ui.screens.TasksScreen
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.FlowTheme

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
    }
}

private val dockItems = listOf(
    DockItem("خانه", FlowIcons.Home),
    DockItem("برنامه", FlowIcons.Calendar),
    DockItem("وظایف", FlowIcons.Tasks),
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
    val animations = data.settings.animations
    BackHandler(enabled = tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (animations) {
                    (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 24 }) togetherWith fadeOut(tween(140))
                } else {
                    ContentTransform(EnterTransition.None, ExitTransition.None)
                }
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> HomeScreen(vm, data, onNavigate = { tab = it })
                1 -> PlanningScreen(vm, data)
                2 -> TasksScreen(vm, data, onStartFocus = { tab = 0 })
                3 -> StatsScreen(data)
                else -> SettingsScreen(vm, data)
            }
        }
        FlowDock(
            dockItems, tab, { tab = it },
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}
