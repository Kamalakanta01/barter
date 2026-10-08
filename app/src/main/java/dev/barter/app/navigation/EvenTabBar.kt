package dev.barter.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.barter.app.R
import dev.barter.feature.browse.navigation.BROWSE_ROUTE
import dev.barter.feature.myitems.navigation.MY_ITEMS_ROUTE
import dev.barter.feature.offers.navigation.OFFERS_ROUTE

/**
 * The prototype's sticky header: the wordmark on the left, three text tabs on the right, with the
 * active tab marked in the accent colour. A bottom bar was the Android-native alternative, but this
 * matches the design and lets the tab label carry the pending-offer count exactly as the
 * prototype's `Offers (n)` did.
 */
@Composable
fun EvenTabBar(
    currentRoute: String?,
    pendingIncoming: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tab(
                        label = stringResource(R.string.tab_browse),
                        selected = currentRoute == BROWSE_ROUTE,
                        onClick = { onSelect(BROWSE_ROUTE) },
                    )
                    Tab(
                        label = stringResource(R.string.tab_my_items),
                        selected = currentRoute == MY_ITEMS_ROUTE,
                        onClick = { onSelect(MY_ITEMS_ROUTE) },
                    )
                    Tab(
                        label = if (pendingIncoming > 0) {
                            stringResource(R.string.tab_offers_count, pendingIncoming)
                        } else {
                            stringResource(R.string.tab_offers)
                        },
                        selected = currentRoute == OFFERS_ROUTE,
                        onClick = { onSelect(OFFERS_ROUTE) },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * The prototype marks the active tab with `box-shadow: inset 0 -2px var(--accent)`, so the
 * indicator is a 2dp accent bar across the bottom of the label rather than a text underline. The
 * tappable box is at least 48dp tall (the minimum for a comfortable touch target) while the bar stays
 * directly under the label.
 */
@Composable
private fun Tab(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .heightIn(min = TAB_MIN_HEIGHT)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .drawBehind {
                    if (!selected) return@drawBehind
                    val bar = INDICATOR_HEIGHT.toPx()
                    drawRect(
                        color = accent,
                        topLeft = Offset(0f, size.height - bar),
                        size = Size(size.width, bar),
                    )
                },
        )
    }
}

private val INDICATOR_HEIGHT = 2.dp
private val TAB_MIN_HEIGHT = 48.dp
