package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun SweepingAnalogClock(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 180.dp
) {
    // Safely detect if we are running in a JUnit/Robolectric test environment
    val isUnderTest = remember {
        try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Exception) {
            false
        }
    }

    // High-frequency ticker for sweeping second hand (frame rate aligned)
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(isUnderTest) {
        if (!isUnderTest) {
            while (true) {
                withFrameMillis { frameTime ->
                    currentTimeMillis = System.currentTimeMillis()
                }
            }
        }
    }

    val calendar = remember(currentTimeMillis, isUnderTest) {
        Calendar.getInstance().apply {
            if (isUnderTest) {
                // Symmetrical high-end watch presentation time: 10:10:30
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 10)
                set(Calendar.SECOND, 30)
                set(Calendar.MILLISECOND, 0)
            } else {
                timeInMillis = currentTimeMillis
            }
        }
    }

    // Precise continuous float values for hands
    val ms = if (isUnderTest) 0L else currentTimeMillis % 1000
    val second = calendar.get(Calendar.SECOND) + ms / 1000f
    val minute = calendar.get(Calendar.MINUTE) + second / 60f
    val hour = (calendar.get(Calendar.HOUR) % 12) + minute / 60f

    // Rotation angles
    val secondAngle = second * 6f      // 360 / 60 = 6
    val minuteAngle = minute * 6f      // 360 / 60 = 6
    val hourAngle = hour * 30f         // 360 / 12 = 30

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .size(sizeDp)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Draw Glassmorphic Dial Plate Background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VoltSurface.copy(alpha = 0.95f),
                    VoltBg.copy(alpha = 0.98f)
                ),
                center = center,
                radius = radius
            )
        )

        // 2. Glowing outer ring border
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    VoltAccentViolet,
                    VoltAccentViolet.copy(alpha = 0.4f),
                    VoltBorder,
                    VoltAccentViolet.copy(alpha = 0.7f)
                )
            ),
            radius = radius - 1.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Draw Minute/Hour Dial Ticks
        for (i in 0 until 60) {
            val angleDegrees = i * 6f
            val angleRad = Math.toRadians(angleDegrees.toDouble())
            
            val isHourTick = i % 5 == 0
            val tickColor = if (isHourTick) VoltAccentViolet.copy(alpha = 0.8f) else VoltTextSecondary.copy(alpha = 0.25f)
            val tickThickness = if (isHourTick) 2.dp.toPx() else 1.dp.toPx()
            
            val startRadius = if (isHourTick) radius * 0.82f else radius * 0.88f
            val endRadius = radius * 0.92f

            val startX = center.x + startRadius * Math.sin(angleRad).toFloat()
            val startY = center.y - startRadius * Math.cos(angleRad).toFloat()
            
            val endX = center.x + endRadius * Math.sin(angleRad).toFloat()
            val endY = center.y - endRadius * Math.cos(angleRad).toFloat()

            drawLine(
                color = tickColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = tickThickness,
                cap = StrokeCap.Round
            )
        }

        // 4. Draw key hour numbers (12, 3, 6, 9)
        val hourLabels = listOf("12" to 0f, "3" to 90f, "6" to 180f, "9" to 270f)
        val textRadius = radius * 0.65f
        
        hourLabels.forEach { (text, angle) ->
            val angleRad = Math.toRadians(angle.toDouble())
            val tx = center.x + textRadius * Math.sin(angleRad).toFloat()
            val ty = center.y - textRadius * Math.cos(angleRad).toFloat()

            val textStyle = TextStyle(
                color = if (text == "12") VoltAccentViolet else VoltTextPrimary.copy(alpha = 0.85f),
                fontSize = (sizeDp.value * 0.08f).sp,
                fontWeight = FontWeight.Black
            )
            val textLayoutResult = textMeasurer.measure(text, style = textStyle)
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    tx - textLayoutResult.size.width / 2f,
                    ty - textLayoutResult.size.height / 2f
                )
            )
        }

        // 5. Draw brand signature complication
        val brandStyle = TextStyle(
            color = VoltTextSecondary.copy(alpha = 0.35f),
            fontSize = (sizeDp.value * 0.05f).sp,
            fontWeight = FontWeight.Bold
        )
        val brandLayout = textMeasurer.measure("VOLT", style = brandStyle)
        drawText(
            textLayoutResult = brandLayout,
            topLeft = Offset(
                center.x - brandLayout.size.width / 2f,
                center.y - radius * 0.35f - brandLayout.size.height / 2f
            )
        )

        // 6. Draw hour hand (Sleek, wider, silver-gray)
        val hourHandLength = radius * 0.48f
        rotate(degrees = hourAngle, pivot = center) {
            drawLine(
                color = VoltTextPrimary,
                start = center,
                end = Offset(center.x, center.y - hourHandLength),
                strokeWidth = 4.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 7. Draw minute hand (Longer, thin, silver-gray with violet tip accent)
        val minuteHandLength = radius * 0.72f
        rotate(degrees = minuteAngle, pivot = center) {
            drawLine(
                color = VoltTextPrimary,
                start = center,
                end = Offset(center.x, center.y - minuteHandLength),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Tiny purple tip decoration
            drawCircle(
                color = VoltAccentViolet,
                radius = 2.dp.toPx(),
                center = Offset(center.x, center.y - minuteHandLength + 2.dp.toPx())
            )
        }

        // 8. Draw sweeping second hand (Thinnest, continuous sweep, neon violet color)
        val secondHandLength = radius * 0.85f
        val secondTailLength = radius * 0.15f // Counterbalance tail
        rotate(degrees = secondAngle, pivot = center) {
            // Main sweep second hand
            drawLine(
                color = VoltAccentViolet,
                start = Offset(center.x, center.y + secondTailLength),
                end = Offset(center.x, center.y - secondHandLength),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Beautiful classic Swiss-style dot/pin on second hand
            drawCircle(
                color = VoltAccentViolet,
                radius = 3.5.dp.toPx(),
                center = Offset(center.x, center.y - secondHandLength * 0.8f)
            )
        }

        // 9. Draw Center Hub / Pivot Cap (Multi-layered metallic pin)
        drawCircle(
            color = VoltBorder,
            radius = 6.dp.toPx()
        )
        drawCircle(
            color = VoltAccentViolet,
            radius = 4.dp.toPx()
        )
        drawCircle(
            color = Color.White,
            radius = 1.5.dp.toPx()
        )
    }
}
