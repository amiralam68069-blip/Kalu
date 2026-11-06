package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.AssistantState
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun NovaOrbCanvas(
    state: AssistantState,
    audioAmplitude: Float,
    glowThemeIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val primaryColor = when (glowThemeIndex) {
        1 -> ElectricPurple
        2 -> NeonPink
        3 -> EmeraldGlow
        else -> NeonCyan
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ReactorOrb")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 4000
                    AssistantState.SPEAKING -> 7000
                    AssistantState.LISTENING -> 9000
                    AssistantState.IDLE -> 18000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRotation"
    )

    val innerNodeRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 3500
                    AssistantState.SPEAKING -> 6000
                    else -> 14000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerNodeRotation"
    )

    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseFactor"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = audioAmplitude,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "AnimatedAmp"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = (min(size.width, size.height) / 2f) * 0.92f
        val reactiveScale = when (state) {
            AssistantState.LISTENING -> 1f + animatedAmp * 0.14f
            AssistantState.SPEAKING -> 1f + animatedAmp * 0.12f
            AssistantState.THINKING -> 1.03f
            AssistantState.IDLE -> pulseFactor
        }

        // 1. Soft Ambient Radial Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    primaryColor.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = maxR * 1.15f
            ),
            radius = maxR * 1.15f,
            center = Offset(cx, cy)
        )

        // 2. Outermost Dashed Telemetry Ring + Cardinal Ticks
        val rOuter1 = maxR * 0.95f
        drawCircle(
            color = primaryColor.copy(alpha = 0.45f),
            radius = rOuter1,
            center = Offset(cx, cy),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f), phase = outerRotation)
            )
        )

        // 36 radial tick marks around the outer ring (matching Screenshot 4)
        val tickCount = 36
        for (i in 0 until tickCount) {
            val angleDeg = (i * (360f / tickCount)) + outerRotation * 0.4f
            val rad = Math.toRadians(angleDeg.toDouble())
            val isMajor = i % 3 == 0
            val innerTickR = if (isMajor) rOuter1 * 0.85f else rOuter1 * 0.90f
            val outerTickR = if (isMajor) rOuter1 * 0.97f else rOuter1 * 0.94f
            val start = Offset(
                cx + (innerTickR * cos(rad)).toFloat(),
                cy + (innerTickR * sin(rad)).toFloat()
            )
            val end = Offset(
                cx + (outerTickR * cos(rad)).toFloat(),
                cy + (outerTickR * sin(rad)).toFloat()
            )
            drawLine(
                color = primaryColor.copy(alpha = if (isMajor) 0.80f else 0.35f),
                start = start,
                end = end,
                strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Secondary Segmented Arc Ring
        val rOuter2 = maxR * 0.78f
        drawCircle(
            color = Color(0xFF0A1D24),
            radius = rOuter2,
            center = Offset(cx, cy),
            style = Stroke(width = 18.dp.toPx())
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.28f),
            radius = rOuter2,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )

        for (i in 0 until 4) {
            drawArc(
                color = primaryColor.copy(alpha = 0.55f),
                startAngle = -outerRotation + (i * 90f) + 15f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = Offset(cx - rOuter2, cy - rOuter2),
                size = Size(rOuter2 * 2, rOuter2 * 2),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 4. Inner Track Ring with 4 Glowing Cyan Orbital Nodes
        val rInnerTrack = maxR * 0.56f * reactiveScale
        drawCircle(
            color = Color(0xFF07151B),
            radius = rInnerTrack * 1.12f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.40f),
            radius = rInnerTrack,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )

        // Deep dark core chamber
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF051319),
                    Color(0xFF02070A)
                ),
                center = Offset(cx, cy),
                radius = rInnerTrack * 0.94f
            ),
            radius = rInnerTrack * 0.94f,
            center = Offset(cx, cy)
        )

        // 4 Glowing Orbital Nodes on the Inner Track (45°, 135°, 225°, 315°)
        val nodeAngles = listOf(45f, 135f, 225f, 315f)
        nodeAngles.forEach { baseAngle ->
            val rad = Math.toRadians((baseAngle + innerNodeRotation * 0.25f).toDouble())
            val nx = cx + (rInnerTrack * 0.84f * cos(rad)).toFloat()
            val ny = cy + (rInnerTrack * 0.84f * sin(rad)).toFloat()

            // Outer glow of orbital node
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = 9.dp.toPx(),
                center = Offset(nx, ny)
            )
            // Solid bright cyan node
            drawCircle(
                color = CyanBright,
                radius = 4.5.dp.toPx(),
                center = Offset(nx, ny)
            )
        }

        // 5. Center Glowing Cyan Core Disc with 7-Dot Matrix (matching Screenshot 4)
        val coreR = maxR * 0.22f * reactiveScale

        // Core outer bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.65f),
                    primaryColor.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = coreR * 1.9f
            ),
            radius = coreR * 1.9f,
            center = Offset(cx, cy)
        )

        // Solid glowing cyan center disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CyanBright,
                    primaryColor
                ),
                center = Offset(cx, cy),
                radius = coreR
            ),
            radius = coreR,
            center = Offset(cx, cy)
        )

        // 7 dark dots inside the center cyan core (1 center + 6 hexagonal ring)
        val centerDotR = coreR * 0.11f
        val dotColor = Color(0xFF04141A)
        drawCircle(
            color = dotColor,
            radius = centerDotR,
            center = Offset(cx, cy)
        )

        val dotRingR = coreR * 0.44f
        for (i in 0 until 6) {
            val angleRad = Math.toRadians((i * 60.0) + outerRotation * 0.5)
            val dx = cx + (dotRingR * cos(angleRad)).toFloat()
            val dy = cy + (dotRingR * sin(angleRad)).toFloat()
            drawCircle(
                color = dotColor,
                radius = centerDotR * 0.9f,
                center = Offset(dx, dy)
            )
        }
    }
}
