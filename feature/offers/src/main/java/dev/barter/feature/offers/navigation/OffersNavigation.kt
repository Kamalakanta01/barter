package dev.barter.feature.offers.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.barter.feature.offers.ui.OffersRoute

const val OFFERS_ROUTE = "offers"

fun NavGraphBuilder.offersScreen() {
    composable(OFFERS_ROUTE) {
        OffersRoute()
    }
}
