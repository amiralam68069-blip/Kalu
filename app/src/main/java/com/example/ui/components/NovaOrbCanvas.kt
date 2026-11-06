package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.AssistantState
import com.example.ui.theme.*
import kotlin.math.*

private class OrbParticle(
    val theta: Float, // Azimuth angle
    val phi: Float,   // Polar angle
    val radiusRatio: Float,
    val baseSize: Float,
    val speedFactor: Float
)

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
    val secondaryColor = when (glowThemeIndex) {
        1 -> DeepViolet
        2 -> AmberGlow
        3 -> CyanGlow
        else -> ElectricPurple
    }

    // Continuous rotation for HUD rings and 3D particle sphere
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 2000
                    AssistantState.SPEAKING -> 4500
                    AssistantState.LISTENING -> 6000
                    AssistantState.IDLE -> 12000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbBaseRotation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    val waveRipple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveRipple"
    )

    // Smooth animated amplitude response
    val animatedAmplitude by animateFloatAsState(
        targetValue = audioAmplitude,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SmoothAmplitude"
    )

    // Pre-calculate 75 deterministic spherical particles
    val particles = remember {
        val list = mutableListOf<OrbParticle>()
        val count = 75
        for (i in 0 until count) {
            val theta = (i * 137.5f * (PI / 180.0)).toFloat() // Golden angle
            val phi = acos(1f - 2f * (i + 0.5f) / count)
            val rRatio = 0.65f + (i % 7) * 0.05f
            val size = 2.0f + (i % 4) * 1.5f
            val speed = 0.8f + (i % 5) * 0.1f
            list.add(OrbParticle(theta, phi, rRatio, size, speed))
        }
        list
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val minDim = min(size.width, size.height)
        val coreRadius = (minDim * 0.32f) * when (state) {
            AssistantState.LISTENING -> (1.05f + animatedAmplitude * 0.35f)
            AssistantState.SPEAKING -> (1.02f + animatedAmplitude * 0.25f)
            AssistantState.THINKING -> 1.08f
            AssistantState.IDLE -> breathingScale
        }

        // 1. Ambient Background Radial Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (state == AssistantState.LISTENING) 0.35f else 0.20f),
                    secondaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = coreRadius * 2.2f
            ),
            radius = coreRadius * 2.2f,
            center = Offset(centerX, centerY)
        )

        // 2. Outer HUD Concentric Rings
        drawHudRings(
            centerX = centerX,
            centerY = centerY,
            radius = coreRadius * 1.35f,
            rotation = baseRotation,
            state = state,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor
        )

        // 3. Speaking / Listening Ripple Waves
        if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
            val rippleRadius1 = coreRadius + (waveRipple * coreRadius * 0.8f)
            val rippleAlpha1 = (1f - waveRipple).coerceIn(0f, 0.7f)
            drawCircle(
                color = primaryColor.copy(alpha = rippleAlpha1),
                radius = rippleRadius1,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )

            val ripplePhase2 = (waveRipple + 0.5f) % 1f
            val rippleRadius2 = coreRadius + (ripplePhase2 * coreRadius * 0.8f)
            val rippleAlpha2 = (1f - ripplePhase2).coerceIn(0f, 0.6f)
            drawCircle(
                color = secondaryColor.copy(alpha = rippleAlpha2),
                radius = rippleRadius2,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 4. 3D Spherical Particle Cloud
        drawParticleSphere(
            centerX = centerX,
            centerY = centerY,
            baseRadius = coreRadius,
            rotationDeg = baseRotation,
            state = state,
            amplitude = animatedAmplitude,
            particles = particles,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor
        )

        // 5. Inner Energy Core Orb
        val coreGradient = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                primaryColor.copy(alpha = 0.85f),
                secondaryColor.copy(alpha = 0.55f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = coreRadius * 0.65f
        )
        drawCircle(
            brush = coreGradient,
            radius = coreRadius * 0.65f,
            center = Offset(centerX, centerY)
        )

        // 6. Central Cybernetic Reticle / Crosshair Marks
        val markLength = coreRadius * 0.22f
        val reticleAlpha = when (state) {
            AssistantState.LISTENING -> 0.9f
            AssistantState.THINKING -> 0.7f
            AssistantState.SPEAKING -> 0.85f
            AssistantState.IDLE -> 0.5f
        }
        val reticleColor = primaryColor.copy(alpha = reticleAlpha)

        // Horizontal reticle ticks
        drawLine(
            color = reticleColor,
            start = Offset(centerX - coreRadius * 0.5f - markLength, centerY),
            end = Offset(centerX - coreRadius * 0.5f, centerY),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = reticleColor,
            start = Offset(centerX + coreRadius * 0.5f, centerY),
            end = Offset(centerX + coreRadius * 0.5f + markLength, centerY),
            strokeWidth = 2.dp.toPx()
        )
        // Vertical reticle ticks
        drawLine(
            color = reticleColor,
            start = Offset(centerX, centerY - coreRadius * 0.5f - markLength),
            end = Offset(centerX, centerY - coreRadius * 0.5f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = reticleColor,
            start = Offset(centerX, centerY + coreRadius * 0.5f),
            end = Offset(centerX, centerY + coreRadius * 0.5f + markLength),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun DrawScope.drawHudRings(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotation: Float,
    state: AssistantState,
    primaryColor: Color,
    secondaryColor: Color
) {
    // Outer dashed ring
    drawArc(
        color = primaryColor.copy(alpha = 0.45f),
        startAngle = rotation,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = Offset(centerX - radius, centerY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
    )

    drawArc(
        color = secondaryColor.copy(alpha = 0.5f),
        startAngle = rotation + 180f,
        sweepAngle = 80f,
        useCenter = false,
        topLeft = Offset(centerX - radius, centerY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Inner counter-rotating ring
    val innerR = radius * 0.85f
    drawArc(
        color = primaryColor.copy(alpha = 0.35f),
        startAngle = -rotation * 1.5f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(centerX - innerR, centerY - innerR),
        size = Size(innerR * 2, innerR * 2),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
    )

    // Fine orbital ring
    drawCircle(
        color = primaryColor.copy(alpha = 0.15f),
        radius = radius * 1.15f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawParticleSphere(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    rotationDeg: Float,
    state: AssistantState,
    amplitude: Float,
    particles: List<OrbParticle>,
    primaryColor: Color,
    secondaryColor: Color
) {
    val rotRadY = (rotationDeg * (PI / 180.0)).toFloat()
    val rotRadX = ((rotationDeg * 0.5f) * (PI / 180.0)).toFloat()

    val expansion = when (state) {
        AssistantState.LISTENING -> 1.0f + amplitude * 0.4f
        AssistantState.SPEAKING -> 1.0f + amplitude * 0.3f
        AssistantState.THINKING -> 1.15f
        AssistantState.IDLE -> 1.0f
    }

    particles.forEach { p ->
        val r = baseRadius * p.radiusRatio * expansion

        // Spherical to Cartesian coordinates (x, y, z)
        val x0 = r * sin(p.phi) * cos(p.theta)
        val y0 = r * cos(p.phi)
        val z0 = r * sin(p.phi) * sin(p.theta)

        // Rotate around Y axis
        val x1 = x0 * cos(rotRadY * p.speedFactor) + z0 * sin(rotRadY * p.speedFactor)
        val z1 = -x0 * sin(rotRadY * p.speedFactor) + z0 * cos(rotRadY * p.speedFactor)

        // Rotate around X axis
        val y2 = y0 * cos(rotRadX) - z1 * sin(rotRadX)
        val z2 = y0 * sin(rotRadX) + z1 * cos(rotRadX)

        // Perspective 3D projection
        val fov = 400f
        val depthFactor = (z2 + fov) / fov
        if (depthFactor > 0.1f) {
            val projX = centerX + x1 * depthFactor
            val projY = centerY + y2 * depthFactor

            // Alpha and particle size scaled with depth
            val alpha = ((z2 / (baseRadius * 1.2f) + 1f) / 2f).coerceIn(0.15f, 0.95f)
            val pSize = (p.baseSize * depthFactor).coerceAtLeast(1.5f)

            val pColor = if (z2 > 0) primaryColor else secondaryColor
            drawCircle(
                color = pColor.copy(alpha = alpha),
                radius = pSize,
                center = Offset(projX, projY)
            )
        }
    }
}
