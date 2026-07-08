package com.example

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VoltBorder
import com.example.ui.theme.VoltSurface

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            VoltSurface,
            VoltBorder,
            VoltSurface
        ),
        start = Offset(translateAnim.value - 200f, translateAnim.value - 200f),
        end = Offset(translateAnim.value, translateAnim.value)
    )
}

@Composable
fun ShimmerAlarmCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(shimmerBrush(), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    )
}

@Composable
fun ShimmerCityCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(shimmerBrush(), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    )
}

@Composable
fun ShimmerWeatherCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(shimmerBrush(), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    )
}

@Composable
fun ShimmerLoadingList(type: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        repeat(if (type == "city") 5 else 3) {
            if (type == "city") {
                ShimmerCityCard()
            } else {
                ShimmerAlarmCard()
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
