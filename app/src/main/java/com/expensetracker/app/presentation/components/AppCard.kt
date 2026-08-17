package com.expensetracker.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expensetracker.app.presentation.theme.LocalAppColors

val CardRadius = 24.dp

/** Standard elevated card used throughout the app. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val shape = RoundedCornerShape(CardRadius)
    val colors = CardDefaults.cardColors(
        containerColor = Color.Transparent,
    )
    val cardModifier = modifier.background(appColors.cardBrush, shape)
    if (onClick != null) {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = BorderStroke(1.dp, appColors.cardStroke),
            onClick = onClick,
            content = content,
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = BorderStroke(1.dp, appColors.cardStroke),
            content = content,
        )
    }
}

/** Gradient hero card for totals. Defaults to the theme's hero brush
 * (blue gradient in dark mode, black-to-grey gradient in light mode). */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val finalBrush = brush ?: appColors.heroBrush
    Column(
        modifier = modifier
            .background(finalBrush, RoundedCornerShape(CardRadius))
            .clip(RoundedCornerShape(CardRadius)),
        content = content,
    )
}
