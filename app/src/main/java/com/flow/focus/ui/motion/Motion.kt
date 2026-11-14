package com.flow.focus.ui.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.flow.focus.ui.theme.LocalGlass

/**
 * زبان حرکتی واحد MatFocus: مدت‌ها و منحنی‌ها فقط از همین‌جا می‌آیند.
 * وقتی انیمیشن‌ها در تنظیمات برنامه یا مقیاس انیمیشن سیستم خاموش باشند، [LocalGlass].animations
 * false می‌شود و تمام مسیرهای این فایل به snap تبدیل می‌شوند.
 */
object Motion {
    const val Fast = 120
    const val Short = 180
    const val Medium = 240
    const val Long = 360
    const val ThemeSwitch = 420

    /** ورود: شروع سریع و توقف نرم */
    val Decelerate = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** خروج: شروع نرم و پایان سریع */
    val Accelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    fun <T> enter(ms: Int = Medium, delayMs: Int = 0): FiniteAnimationSpec<T> = tween(ms, delayMs, Decelerate)
    fun <T> exit(ms: Int = Short): FiniteAnimationSpec<T> = tween(ms, 0, Accelerate)

    /** فنر ملایم برای جابه‌جایی عناصر (انتخاب‌گر، پنل‌ها) */
    fun <T> soft(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

    /** فنر کمی زنده‌تر برای لمس و انتخاب */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.72f, stiffness = 520f)

    fun <T> maybe(animations: Boolean, spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (animations) spec else snap()

    /**
     * جابه‌جایی کوتاه یک مقدار (رقم، عدد آمار، برچسب) با لغزش عمودی کم؛ با خاموش‌بودن انیمیشن‌ها بدون حرکت.
     * همه‌ی AnimatedContent های مقداری باید از همین استفاده کنند تا تنظیمات انیمیشن رعایت شود.
     */
    fun valueSwap(animations: Boolean, inMs: Int = 150, outMs: Int = 110): ContentTransform =
        if (animations) {
            (fadeIn(tween(inMs)) + slideInVertically(tween(inMs + 20)) { it / 4 }) togetherWith
                (fadeOut(tween(outMs)) + slideOutVertically(tween(outMs + 20)) { -it / 4 })
        } else ContentTransform(EnterTransition.None, ExitTransition.None)
}

/**
 * AnimatedVisibility با visible=true از همان ابتدا هیچ انیمیشن ورودی‌ای اجرا نمی‌کند.
 * این State از false شروع می‌شود تا ورود واقعاً دیده شود.
 */
@Composable
fun rememberEnterState(visible: Boolean = true): MutableTransitionState<Boolean> {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    return state
}

/** ظاهر شدن نرم یک آیتم لیست (با تأخیر پلکانی). */
@Composable
fun Appear(modifier: Modifier = Modifier, delayMs: Int = 0, content: @Composable () -> Unit) {
    val animations = LocalGlass.current.animations
    val state = rememberEnterState(true)
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = if (animations) {
            fadeIn(Motion.enter(Motion.Medium, delayMs)) + slideInVertically(Motion.enter(Motion.Medium, delayMs)) { it / 14 }
        } else EnterTransition.None,
    ) { content() }
}
