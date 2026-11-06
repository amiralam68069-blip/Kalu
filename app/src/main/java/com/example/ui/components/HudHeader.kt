package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AssistantState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudHeader(
    assistantName: String,
    state: AssistantState,
    isOnline: Boolean = true,
    onOpenSettings: () -> Unit,
    onClearChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    val statusLabel = when {
        !isOnline -> "Offline — systems standby"
        state == AssistantState.LISTENING -> "Listening — capturing audio"
        state == AssistantState.THINKING -> "Processing — analyzing intent"
        state == AssistantState.SPEAKING -> "Speaking — voice transmission"
        else -> "Calm — monitoring systems"
    }

    val statusDotColor = when {
        !isOnline -> TextMuted
        state == AssistantState.LISTENING -> CyanBright
        state == AssistantState.THINKING -> ElectricPurple
        state == AssistantState.SPEAKING -> EmeraldGlow
        else -> NeonCyan
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xE604090C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Glowing Hexagon-Circle Core Badge + Assistant Name + Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                HexCoreAvatar(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onClearChat() }
                        .testTag("clear_chat_button")
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = assistantName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(statusDotColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Right: Vertical Divider + Clock & Signal Bars + Hex Settings Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(CyberCardBorder)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currentTime,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    SignalBarsIndicator()
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Hexagonal Settings Button matching Screenshot 4
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF112028))
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onOpenSettings() }
                        .testTag("settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Bottom thin divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF0E252E))
        )
    }
}

@Composable
fun HexCoreAvatar(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f

        // Outer glowing cyan ring
        drawCircle(
            color = NeonCyan.copy(alpha = 0.25f),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = NeonCyan,
            radius = r * 0.90f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.2.dp.toPx())
        )

        // Inner dark hexagon
        val hexPath = Path()
        val hexR = r * 0.68f
        for (i in 0 until 6) {
            val angleRad = Math.toRadians((60.0 * i) - 30.0)
            val px = cx + (hexR * cos(angleRad)).toFloat()
            val py = cy + (hexR * sin(angleRad)).toFloat()
            if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
        }
        hexPath.close()
        drawPath(path = hexPath, color = Color(0xFF162832))
        drawPath(path = hexPath, color = NeonCyan.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))

        // 7-dot inner matrix (1 center + 6 surrounding)
        val dotR = 1.6.dp.toPx()
        drawCircle(color = NeonCyan, radius = dotR, center = Offset(cx, cy))
        val ringR = r * 0.30f
        for (i in 0 until 6) {
            val angleRad = Math.toRadians(60.0 * i)
            val dx = cx + (ringR * cos(angleRad)).toFloat()
            val dy = cy + (ringR * sin(angleRad)).toFloat()
            drawCircle(color = NeonCyan, radius = dotR, center = Offset(dx, dy))
        }
    }
}

@Composable
private fun SignalBarsIndicator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(10.dp)
    ) {
        val heights = listOf(3.dp, 5.dp, 7.dp, 10.dp)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(h)
                    .background(NeonCyan, RoundedCornerShape(1.dp))
            )
        }
    }
}
