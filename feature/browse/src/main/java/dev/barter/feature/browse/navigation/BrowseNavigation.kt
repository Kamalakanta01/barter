package dev.barter.feature.browse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.barter.feature.browse.ui.BrowseRoute

const val BROWSE_ROUTE = "browse"

/**
 * [onListItem] is invoked when the swap dialog finds you have nothing to offer.
 * [onOfferSent] fires once an offer is sent, so the app can show its result rather than leaving the
 * user on the grid with no confirmation.
 */
fun NavGraphBuilder.browseScreen(
    onListItem: () -> Unit,
    onOfferSent: () -> Unit,
) {
    composable(BROWSE_ROUTE) {
        BrowseRoute(onListItem = onListItem, onOfferSent = onOfferSent)
    }
}
