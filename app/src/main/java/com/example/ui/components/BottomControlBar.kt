package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AssistantState
import com.example.ui.theme.*

@Composable
fun BottomControlBar(
    state: AssistantState,
    isOnline: Boolean = true,
    onToggleOnline: () -> Unit = {},
    onVisionClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onMicClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onSendText: (String) -> Unit,
    glowThemeIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val primaryGlow = when (glowThemeIndex) {
        1 -> ElectricPurple
        2 -> NeonPink
        3 -> EmeraldGlow
        else -> NeonCyan
    }

    val infiniteTransition = rememberInfiniteTransition(label = "MicHalo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == AssistantState.LISTENING) 1.16f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bottom 4-Control Row matching Screenshot 4: [ONLINE] [VISION] (MIC) [HISTORY]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. ONLINE Pill Button
            DockPillButton(
                icon = Icons.Outlined.PowerSettingsNew,
                label = if (isOnline) "ONLINE" else "OFFLINE",
                iconTint = if (isOnline) primaryGlow else TextMuted,
                borderColor = if (isOnline) CyberCardBorder else Color(0xFF1A262C),
                onClick = onToggleOnline,
                modifier = Modifier
                    .weight(1f)
                    .testTag("online_toggle_button")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 2. VISION Pill Button
            DockPillButton(
                icon = Icons.Outlined.Tv,
                label = "VISION",
                iconTint = primaryGlow,
                borderColor = CyberCardBorder,
                onClick = onVisionClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("vision_button")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Center Glowing Cyan Circular Mic Button (matching Screenshot 4)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (state == AssistantState.LISTENING) pulseScale else 1f),
                contentAlignment = Alignment.Center
            ) {
                // Outer radial cyan halo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryGlow.copy(alpha = 0.45f),
                                    primaryGlow.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Dark ring border
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06161D))
                        .border(2.dp, primaryGlow.copy(alpha = 0.65f), CircleShape)
                        .clickable {
                            if (state == AssistantState.SPEAKING) {
                                onInterruptClick()
                            } else {
                                onMicClick()
                            }
                        }
                        .testTag("main_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner solid glowing cyan circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        CyanBright,
                                        primaryGlow
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (state) {
                                AssistantState.SPEAKING -> Icons.Default.Stop
                                AssistantState.LISTENING -> Icons.Default.GraphicEq
                                AssistantState.THINKING -> Icons.Default.HourglassTop
                                AssistantState.IDLE -> Icons.Default.MicNone
                            },
                            contentDescription = "Microphone",
                            tint = Color(0xFF04141B),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 4. HISTORY Pill Button
            DockPillButton(
                icon = Icons.Outlined.History,
                label = "HISTORY",
                iconTint = primaryGlow,
                borderColor = CyberCardBorder,
                onClick = onHistoryClick,
                modifier = Modifier
                    .weight(1.15f)
                    .testTag("history_button")
            )
        }
    }
}

@Composable
private fun DockPillButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0B181F))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
    }
}
