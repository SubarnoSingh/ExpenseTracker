package com.expensetracker.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expensetracker.app.domain.model.ChartPoint
import com.expensetracker.app.presentation.theme.LocalAppColors
import kotlin.math.max

/**
 * Smooth area chart with gradient fill, dots and a subtle draw-in animation.
 * Data comes from the ViewModel - never fake/static values.
 */
@Composable
fun SpendingChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val appColors = LocalAppColors.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(900))
    }

    val maxValue = points.maxOfOrNull { it.value } ?: 0.0
    val safeMax = max(maxValue, 1.0)
    val n = points.size

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            if (n == 0) return@Canvas

            val chartWidth = size.width
            val chartHeight = size.height
            // Inset by the dot radius, or the first and last points get sliced in
            // half by the edges of the card.
            val inset = 5.dp.toPx()
            val stepX = (chartWidth - inset * 2) / (n - 1).coerceAtLeast(1)
            fun xFor(index: Int): Float = inset + index * stepX
            val baseline = chartHeight - 4.dp.toPx()

            fun yFor(value: Double): Float =
                (baseline - (value / safeMax) * (chartHeight - 18.dp.toPx())).toFloat()

            val linePath = Path()
            points.forEachIndexed { index, point ->
                val x = xFor(index)
                val y = yFor(point.value)
                if (index == 0) linePath.moveTo(x, y)
                else {
                    val prev = points[index - 1]
                    val prevX = xFor(index - 1)
                    val prevY = yFor(prev.value)
                    val midX = (prevX + x) / 2
                    val midY = (prevY + y) / 2
                    linePath.quadraticBezierTo(prevX, prevY, midX, midY)
                }
            }
            val lastX = xFor(n - 1)
            val lastY = yFor(points.last().value)

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(lastX, baseline)
                lineTo(0f, baseline)
                close()
            }

            val fillFraction = progress.value
            clipRect(right = chartWidth * fillFraction) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.30f),
                            accent.copy(alpha = 0.02f),
                        ),
                        startY = 0f,
                        endY = baseline,
                    ),
                )
                drawPath(
                    path = linePath,
                    color = accent,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Dots on each point when there aren't too many.
                if (n <= 31) {
                    val visible = (n * fillFraction).toInt().coerceIn(1, n)
                    for (i in 0 until visible) {
                        val point = points[i]
                        if (point.value <= 0.0) continue
                        val x = xFor(i)
                        val y = yFor(point.value)
                        drawCircle(
                            color = appColors.gradientEnd,
                            radius = 4.5.dp.toPx(),
                            center = Offset(x, y),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = 2.dp.toPx(),
                            center = Offset(x, y),
                        )
                    }
                }
            }
        }

        // X-axis labels, aligned to the same slot widths as the data points.
        val labelIndices = pickLabelIndices(n)
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { index, point ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    if (index in labelIndices && point.label.isNotBlank()) {
                        // A 30-day month gives each slot ~11dp, so the label has to be
                        // allowed to overflow its slot or it wraps one digit per line.
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                        )
                    }
                }
            }
        }
    }
}

/** ~4 evenly spaced label indices for charts. */
fun pickLabelIndices(n: Int): Set<Int> {
    if (n <= 0) return emptySet()
    return when {
        n <= 7 -> (0 until n).toSet()
        else -> {
            val step = (n - 1) / 4.0
            (0..4).map { (it * step).toInt() }.toSet() + (n - 1)
        }
    }
}
