package com.expensetracker.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expensetracker.app.domain.model.ChartPoint
import com.expensetracker.app.presentation.theme.LocalAppColors
import kotlin.math.max

/**
 * Rounded vertical bar chart with grow-in animation.
 * [highlightIndices] marks bars (e.g. today / highest day) with the full gradient.
 */
@Composable
fun BarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    highlightIndices: Set<Int> = emptySet(),
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    val appColors = LocalAppColors.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(800))
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
            val slot = chartWidth / n
            val barWidth = slot * 0.55f
            val corner = CornerRadius(5.dp.toPx(), 5.dp.toPx())

            points.forEachIndexed { index, point ->
                val targetHeight = ((point.value / safeMax) * (chartHeight - 14.dp.toPx())).toFloat()
                val animatedHeight = targetHeight * progress.value
                val x = index * slot + (slot - barWidth) / 2
                val y = chartHeight - animatedHeight

                val isHighlight = index in highlightIndices
                drawRoundRect(
                    brush = if (isHighlight) {
                        Brush.verticalGradient(
                            listOf(appColors.gradientStart, appColors.gradientEnd)
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                barColor.copy(alpha = 0.45f),
                                barColor.copy(alpha = 0.75f),
                            )
                        )
                    },
                    topLeft = Offset(x, y),
                    size = Size(barWidth, animatedHeight),
                    cornerRadius = corner,
                )
            }
        }

        // X labels aligned to slots.
        val labelIndices = pickLabelIndices(n)
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { index, point ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index in labelIndices && point.label.isNotBlank()) {
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
