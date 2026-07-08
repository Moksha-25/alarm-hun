package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VoltAccentViolet
import com.example.ui.theme.VoltBorder
import com.example.ui.theme.VoltDivider
import com.example.ui.theme.VoltSurface
import com.example.ui.theme.VoltTextPrimary
import com.example.ui.theme.VoltTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    // Spring animation values for appearing logo
    val scaleAnim = remember { Animatable(0.3f) }
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessLow
            )
        )
        rotationAnim.animateTo(
            targetValue = 360f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = VoltSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scaleAnim.value)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_volt_logo),
                    contentDescription = "VoltAlarm Logo",
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "VoltAlarm",
                color = VoltTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Wake Smarter, Live Better",
                color = VoltTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = VoltDivider)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Crafted with ❤ by",
                color = VoltTextSecondary,
                fontSize = 13.sp,
                letterSpacing = 0.15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Premium visual container with soft glow behind Developer Name
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Soft glow
                Box(
                    modifier = Modifier
                        .blur(8.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    VoltAccentViolet.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                        .size(180.dp, 40.dp)
                )
                Text(
                    text = "INJAM MOKSHAGNA",
                    color = VoltAccentViolet,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.25.sp
                )
            }

            Text(
                text = "Solo Developer & Designer",
                color = VoltTextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = VoltDivider)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Version 1.0.0  |  © 2026",
                color = VoltTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Premium outlined pill-style action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Actions */ },
                    border = BorderStroke(1.dp, VoltBorder),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltTextPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⭐ Rate App", fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("injammokshagna@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "VoltAlarm Feedback")
                            putExtra(Intent.EXTRA_TEXT, "Hi INJAM MOKSHAGNA,\n\nApp version: 1.0.0\n\n")
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send email"))
                        } catch (e: Exception) {
                            // Handler fallback
                        }
                    },
                    border = BorderStroke(1.dp, VoltBorder),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltTextPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📧 Contact Dev", fontSize = 12.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Policy link */ },
                    border = BorderStroke(1.dp, VoltBorder),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltTextPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🔒 Privacy", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { /* Licenses dialog */ },
                    border = BorderStroke(1.dp, VoltBorder),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltTextPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📄 Licenses", fontSize = 12.sp)
                }
            }
        }
    }
}
