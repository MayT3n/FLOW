package com.flow.focus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.flow.focus.ui.glass.GlassLevel
import com.flow.focus.ui.glass.GlassSurface
import com.flow.focus.ui.glass.LocalBackdropLayer
import com.flow.focus.ui.motion.Motion
import com.flow.focus.ui.motion.rememberEnterState
import com.flow.focus.ui.theme.Flow
import com.flow.focus.ui.theme.LocalGlass
import kotlinx.coroutines.delay

/**
 * Bottom Sheet شیشه‌ای با پرده‌ی تیره‌ی انیمیشنی. با کشیدن دستگیره به پایین، لمس بیرون یا Back بسته می‌شود.
 * ساختار مثل [FlowDialog] است اما از پایین وارد می‌شود و با صفحه‌کلید جابه‌جا می‌شود (imePadding).
 */
@Composable
fun FlowSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    level: GlassLevel = GlassLevel.Strong,
    content: @Composable ColumnScope.() -> Unit,
) {
    val animations = LocalGlass.current.animations
    var visible by remember { mutableStateOf(true) }
    val enterState = rememberEnterState(visible)
    val requestDismiss = { if (visible) visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            if (animations) delay(Motion.Medium.toLong())
            onDismiss()
        }
    }
    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // پرده را خودمان انیمیشن می‌دهیم؛ تیرگی پیش‌فرض پنجره حذف می‌شود.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        CompositionLocalProvider(LocalBackdropLayer provides null) {
            Box(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visibleState = enterState,
                    enter = if (animations) fadeIn(Motion.enter(Motion.Medium)) else EnterTransition.None,
                    exit = if (animations) fadeOut(Motion.exit(Motion.Short)) else ExitTransition.None,
                ) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = requestDismiss),
                    )
                }
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    AnimatedVisibility(
                        visibleState = enterState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = if (animations) slideInVertically(Motion.soft()) { it } + fadeIn(Motion.enter(Motion.Short)) else EnterTransition.None,
                        exit = if (animations) slideOutVertically(Motion.exit(Motion.Medium)) { it } + fadeOut(Motion.exit(Motion.Short)) else ExitTransition.None,
                    ) {
                        GlassSurface(
                            Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 8.dp)
                                .navigationBarsPadding().imePadding().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(30.dp),
                            level = level,
                            tint = Flow.colors.bgTop,
                            elevated = true,
                        ) {
                            Column(
                                Modifier
                                    .border(1.2.dp, Flow.colors.glassEdge.copy(alpha = if (Flow.colors.isDark) 0.30f else 0.75f), RoundedCornerShape(30.dp))
                                    .statusBarsPadding().heightIn(max = 640.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp).padding(bottom = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(13.dp),
                            ) {
                                // دستگیره: کشیدن به پایین یا لمس، شیت را می‌بندد
                                Box(
                                    Modifier.fillMaxWidth().height(26.dp)
                                        .pointerInput(Unit) {
                                            var dragged = 0f
                                            detectVerticalDragGestures(
                                                onVerticalDrag = { _, amount -> dragged += amount },
                                                onDragEnd = { if (dragged > 48f) requestDismiss(); dragged = 0f },
                                                onDragCancel = { dragged = 0f },
                                            )
                                        }
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = requestDismiss),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(Modifier.width(38.dp).height(4.dp).clip(CircleShape).background(Flow.colors.textSecondary.copy(alpha = 0.4f)))
                                }
                                if (title != null) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(title, Modifier.weight(1f), style = Flow.type.title, color = Flow.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        GlassIconButton(FlowIcons.Close, requestDismiss, size = 34.dp, iconSize = 16.dp, contentDescription = "بستن")
                                    }
                                }
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}
