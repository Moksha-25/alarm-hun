package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object AnimationUtils {
    val EaseOutCubic = Easing { t ->
        val x = t - 1f
        x * x * x + 1f
    }
}

@Composable
fun rememberStaggeredEntranceState(index: Int, delayStepMs: Long = 50L): Float {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * delayStepMs)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = AnimationUtils.EaseOutCubic)
        )
    }
    return animatedProgress.value
}

fun Modifier.staggeredEntrance(index: Int, delayStepMs: Long = 50L): Modifier = composed {
    val progress = rememberStaggeredEntranceState(index, delayStepMs)
    this
        .alpha(progress)
        .offset(y = ((1f - progress) * 20f).dp)
}

fun Modifier.pressScale(onClick: () -> Unit): Modifier = composed {
    val scale = remember { Animatable(1f) }
    this
        .scale(scale.value)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    try {
                        scale.animateTo(0.97f, spring(stiffness = Spring.StiffnessHigh))
                        tryAwaitRelease()
                        scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                    } catch (e: Exception) {
                        scale.snapTo(1f)
                    }
                },
                onTap = { onClick() }
            )
        }
}
