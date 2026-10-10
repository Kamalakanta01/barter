package dev.barter.app.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.barter.app.R
import dev.barter.core.designsystem.component.EvenIcons
import dev.barter.feature.browse.navigation.BROWSE_ROUTE
import dev.barter.feature.myitems.navigation.MY_ITEMS_ROUTE
import dev.barter.feature.offers.navigation.OFFERS_ROUTE

/**
 * The three destinations, at the bottom of the screen where a thumb reaches them.
 *
 * This is the standard Material navigation bar, so touch targets, the selected state and the screen
 * reader's "tab, selected" announcement come from the component rather than from custom drawing.
 * Offers carries a badge with the number of offers waiting for an answer.
 */
@Composable
fun EvenNavigationBar(
    currentRoute: String?,
    pendingIncoming: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onBackground,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            NavItem(
                selected = currentRoute == BROWSE_ROUTE,
                onClick = { onSelect(BROWSE_ROUTE) },
                icon = EvenIcons.Browse,
                label = stringResource(R.string.tab_browse),
                colors = colors,
            )
            NavItem(
                selected = currentRoute == MY_ITEMS_ROUTE,
                onClick = { onSelect(MY_ITEMS_ROUTE) },
                icon = EvenIcons.Listing,
                label = stringResource(R.string.tab_my_items),
                colors = colors,
            )
            NavItem(
                selected = currentRoute == OFFERS_ROUTE,
                onClick = { onSelect(OFFERS_ROUTE) },
                icon = EvenIcons.Swap,
                label = stringResource(R.string.tab_offers),
                colors = colors,
                badgeCount = pendingIncoming,
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    label: String,
    colors: NavigationBarItemColors,
    badgeCount: Int = 0,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            if (badgeCount > 0) {
                val waiting = pluralStringResource(R.plurals.tab_offers_waiting, badgeCount, badgeCount)
                BadgedBox(
                    badge = { Badge { Text(if (badgeCount > 9) "9+" else badgeCount.toString()) } },
                    modifier = Modifier.semantics { contentDescription = waiting },
                ) {
                    Icon(painter = painterResource(icon), contentDescription = null)
                }
            } else {
                Icon(painter = painterResource(icon), contentDescription = null)
            }
        },
        label = { Text(label) },
        colors = colors,
    )
}
