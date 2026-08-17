package com.expensetracker.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DonutSegment(
    val value: Double,
    val color: Color,
)

/**
 * Donut chart with rounded, gapped segments and a center slot for the total.
 */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    strokeWidth: Dp = 24.dp,
    centerContent: @Composable BoxScope.() -> Unit = {},
) {
    val progress = remember { Animatable(0f) }
    val total = segments.sumOf { it.value }

    LaunchedEffect(segments) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(900))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            if (total <= 0.0) {
                // Empty ring placeholder
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx()),
                )
                return@Canvas
            }
            val gapDegrees = 4f
            var startAngle = -90f
            val sweepFraction = progress.value
            segments.forEach { segment ->
                val sweep = (segment.value / total * 360.0 * sweepFraction).toFloat()
                if (sweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle + gapDegrees / 2,
                        sweepAngle = (sweep - gapDegrees).coerceAtLeast(0.5f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                    )
                }
                startAngle += (segment.value / total * 360.0).toFloat()
            }
        }
        centerContent()
    }
}
