package com.expensetracker.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetracker.app.domain.model.SubscriptionEntry
import com.expensetracker.app.domain.model.dayLabel
import java.time.LocalDate

@Composable
fun SubscriptionCard(
    entry: SubscriptionEntry,
    rateLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showActiveState: Boolean = false,
) {
    val sub = entry.subscription
    val category = entry.category
    val categoryColor = Color(category?.color ?: 0xFF8B5CF6)
    val dueSoon = sub.isActive && sub.nextBillingDate.isBefore(LocalDate.now().plusDays(7))

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CategoryBadge(
                icon = CategoryIcons.iconFor(category?.icon ?: "more_horiz"),
                color = categoryColor,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // weight(fill = false) lets a long name ellipsize instead of
                    // squeezing the pill down to one character per line.
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (dueSoon) {
                        StatusPill(
                            text = "Due soon",
                            color = MaterialTheme.colorScheme.primary,
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        )
                    }
                    if (showActiveState && !sub.isActive) {
                        StatusPill(
                            text = "Paused",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            background = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                }
                Text(
                    text = "${category?.name ?: "Subscription"}  •  ${rateLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Next payment: ${sub.nextBillingDate.dayLabel()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (dueSoon) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = rateLabel.substringBefore(" / "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Small status chip. Never wraps - it keeps its own width whatever sits beside it. */
@Composable
private fun StatusPill(text: String, color: Color, background: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(background, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
