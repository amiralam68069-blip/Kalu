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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AssistantState
import com.example.ui.theme.*

@Composable
fun BottomControlBar(
    state: AssistantState,
    onMicClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onSendText: (String) -> Unit,
    glowThemeIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    var isKeyboardOpen by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val primaryGlow = when (glowThemeIndex) {
        1 -> ElectricPurple
        2 -> NeonPink
        3 -> EmeraldGlow
        else -> NeonCyan
    }

    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == AssistantState.LISTENING) 1.18f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expandable typed text input bar
        AnimatedVisibility(
            visible = isKeyboardOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .background(GlassSurface)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "Type your command for NOVA...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_command_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendText(textInput.trim())
                            textInput = ""
                            isKeyboardOpen = false
                        }
                    },
                    modifier = Modifier.testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Command",
                        tint = primaryGlow
                    )
                }
            }
        }

        // Action controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Keyboard toggle button
            IconButton(
                onClick = { isKeyboardOpen = !isKeyboardOpen },
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .background(GlassSurface, RoundedCornerShape(12.dp))
                    .testTag("keyboard_toggle_button")
            ) {
                Icon(
                    imageVector = if (isKeyboardOpen) Icons.Default.Close else Icons.Default.Keyboard,
                    contentDescription = "Toggle Keyboard Input",
                    tint = TextIce
                )
            }

            // Centerpiece: Glowing Mic / Interrupt Button
            Box(
                modifier = Modifier
                    .scale(if (state == AssistantState.LISTENING) pulseScale else 1f)
                    .clip(CircleShape)
                    .border(
                        width = if (state == AssistantState.LISTENING) 2.5.dp else 1.5.dp,
                        brush = Brush.radialGradient(
                            listOf(primaryGlow, primaryGlow.copy(alpha = 0.3f))
                        ),
                        shape = CircleShape
                    )
                    .background(
                        Brush.radialGradient(
                            listOf(
                                when (state) {
                                    AssistantState.LISTENING -> primaryGlow.copy(alpha = 0.45f)
                                    AssistantState.SPEAKING -> NeonPink.copy(alpha = 0.45f)
                                    AssistantState.THINKING -> ElectricPurple.copy(alpha = 0.45f)
                                    AssistantState.IDLE -> GlassSurface
                                },
                                ObsidianDark
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        if (state == AssistantState.SPEAKING) {
                            onInterruptClick()
                        } else {
                            onMicClick()
                        }
                    }
                    .padding(18.dp)
                    .testTag("main_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (state) {
                        AssistantState.SPEAKING -> Icons.Default.Stop
                        AssistantState.LISTENING -> Icons.Default.GraphicEq
                        AssistantState.THINKING -> Icons.Default.HourglassTop
                        AssistantState.IDLE -> Icons.Default.Mic
                    },
                    contentDescription = when (state) {
                        AssistantState.SPEAKING -> "Interrupt speech"
                        AssistantState.LISTENING -> "Listening"
                        else -> "Tap to speak"
                    },
                    tint = when (state) {
                        AssistantState.SPEAKING -> NeonPink
                        AssistantState.LISTENING -> primaryGlow
                        AssistantState.THINKING -> ElectricPurple
                        AssistantState.IDLE -> primaryGlow
                    },
                    modifier = Modifier.size(32.dp)
                )
            }

            // Status indicator pill on the right
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (state) {
                        AssistantState.LISTENING -> "LIVE MIC"
                        AssistantState.SPEAKING -> "SPEAKING"
                        AssistantState.THINKING -> "THINKING"
                        AssistantState.IDLE -> "READY"
                    },
                    color = when (state) {
                        AssistantState.LISTENING -> primaryGlow
                        AssistantState.SPEAKING -> NeonPink
                        AssistantState.THINKING -> ElectricPurple
                        AssistantState.IDLE -> EmeraldGlow
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}
