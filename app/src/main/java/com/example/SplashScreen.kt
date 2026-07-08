package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VoltAccentViolet
import com.example.ui.theme.VoltTextPrimary
import com.example.ui.theme.VoltTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    // Swirling background animation
    val infiniteTransition = rememberInfiniteTransition(label = "radial")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    // Animation values
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoGlow = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(25f) }
    val taglineAlpha = remember { Animatable(0f) }
    val devAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 0-400ms: Scale spring to 1.1f, alpha 1f
        logoAlpha.animateTo(1f, tween(400))
        logoScale.animateTo(
            targetValue = 1.1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 200f
            )
        )
        // 400-600ms: Scale settle to 1.0f
        logoScale.animateTo(1.0f, tween(200))
        // 600-900ms: Glow pulse (0 -> 0.6 -> 0)
        logoGlow.animateTo(0.6f, tween(150))
        logoGlow.animateTo(0f, tween(150))
        // 900-1200ms: App Name slides up + fades in
        textOffsetY.animateTo(0f, tween(300))
        textAlpha.animateTo(1f, tween(300))
        // 1200-1500ms: Tagline fades in
        taglineAlpha.animateTo(1f, tween(300))
        // 1500-2000ms: Developer name fades in
        devAlpha.animateTo(1f, tween(500))
        // 2000-2200ms: Screen fade-out
        delay(200)
        screenAlpha.animateTo(0f, tween(200))
        onAnimationFinished()
    }

    // Swirling center coordinates for a live, fluid background
    val angleRad = Math.toRadians(angle.toDouble())
    val centerXOffset = (cos(angleRad) * 40f).toFloat()
    val centerYOffset = (sin(angleRad) * 40f).toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF090D16)),
                    center = Offset(x = 500f + centerXOffset, y = 1000f + centerYOffset),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo + Ring Pulse Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            ) {
                // Soft glowing background circle
                if (logoGlow.value > 0f) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .blur(16.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        VoltAccentViolet.copy(alpha = logoGlow.value),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Main Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_volt_logo),
                    contentDescription = "VoltAlarm Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App name slide up + fade in
            Text(
                text = "VoltAlarm",
                color = VoltTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffsetY.value.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = "Wake Smarter, Live Better",
                color = VoltTextSecondary,
                fontSize = 15.sp,
                letterSpacing = 0.05.sp,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }

        // Developer credits section fixed at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(devAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Crafted with ❤ by",
                color = VoltTextSecondary,
                fontSize = 13.sp,
                letterSpacing = 0.15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "INJAM MOKSHAGNA",
                color = VoltAccentViolet,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}
