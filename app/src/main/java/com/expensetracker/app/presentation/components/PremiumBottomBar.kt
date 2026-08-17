package com.expensetracker.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expensetracker.app.presentation.theme.LocalAppColors

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Five-slot bottom bar: Home, Expenses, center gradient [+], Analytics, More.
 * The center slot is always the add action.
 */
@Composable
fun PremiumBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val leftTabs = tabs.take(2)
    val rightTabs = tabs.drop(2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        // Slim top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(appColors.cardStroke),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftTabs.forEach { tab -> TabSlot(tab, currentRoute, onTabSelected) }
            Spacer(Modifier.weight(0.35f))
            Box(contentAlignment = Alignment.Center) {
                GradientFab(
                    onClick = onAddClick,
                    size = 52,
                )
            }
            Spacer(Modifier.weight(0.35f))
            rightTabs.forEach { tab -> TabSlot(tab, currentRoute, onTabSelected) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabSlot(
    tab: BottomTab,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
) {
    val selected = currentRoute == tab.route
    val appColors = LocalAppColors.current
    val tint by animateColorAsState(
        targetValue = if (selected) appColors.gradientStart
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "tabTint",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = tween(220),
        label = "tabIndicator",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onTabSelected(tab.route) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        // Small sliding indicator pill
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(indicatorWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tint),
        )
    }
}
