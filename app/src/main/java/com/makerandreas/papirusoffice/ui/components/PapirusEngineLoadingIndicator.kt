package com.makerandreas.papirusoffice.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Animated Material 3 Loading Indicator for Papirus Engine document rendering.
 * Supports module-specific color accents (Inky, Cellina, Slidia, Pagella).
 */
@Composable
fun PapirusEngineLoadingIndicator(
    modifier: Modifier = Modifier,
    moduleName: String = "Papirus Engine",
    moduleColor: Color = MaterialTheme.colorScheme.primary,
    statusMessage: String = "Processing document rendering...",
    progress: Float? = null // null for indeterminate, 0.0f..1.0f for determinate
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PapirusLoadingTransition")

    // Rotation animation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Pulse animation for icon scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Alpha fade animation for glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val moduleIcon: ImageVector = when (moduleName.uppercase()) {
        "INKY" -> Icons.Rounded.Description
        "CELLINA" -> Icons.Rounded.GridOn
        "SLIDIA" -> Icons.Rounded.Slideshow
        "PAGELLA" -> Icons.Rounded.Article
        else -> Icons.Rounded.AutoAwesome
    }

    Surface(
        modifier = modifier
            .wrapContentSize()
            .shadow(12.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Canvas Arc + Central Pulse Icon
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Soft Glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(moduleColor.copy(alpha = glowAlpha * 0.3f))
                )

                // Custom Animated Arc Ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Track ring
                    drawArc(
                        color = moduleColor.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Animated Rotating Gradient Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                moduleColor.copy(alpha = 0.1f),
                                moduleColor,
                                moduleColor.copy(alpha = 0.9f)
                            )
                        ),
                        startAngle = rotationAngle,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Central Module Icon
                Icon(
                    imageVector = moduleIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(pulseScale),
                    tint = moduleColor
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Module Label & Engine Indicator
            Text(
                text = moduleName.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.2.sp,
                color = moduleColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusMessage,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 240.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar / Line
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(180.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = moduleColor,
                    trackColor = moduleColor.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = moduleColor
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(180.dp)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = moduleColor,
                    trackColor = moduleColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}

/**
 * Fullscreen / Overlay Composable wrapper for Papirus Engine Loading state.
 */
@Composable
fun PapirusEngineLoadingOverlay(
    isVisible: Boolean,
    moduleName: String = "Papirus Engine",
    moduleColor: Color = MaterialTheme.colorScheme.primary,
    statusMessage: String = "Processing document rendering...",
    progress: Float? = null
) {
    if (!isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        PapirusEngineLoadingIndicator(
            moduleName = moduleName,
            moduleColor = moduleColor,
            statusMessage = statusMessage,
            progress = progress
        )
    }
}
