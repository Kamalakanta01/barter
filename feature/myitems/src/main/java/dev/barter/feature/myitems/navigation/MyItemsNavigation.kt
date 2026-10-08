package dev.barter.feature.myitems.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.barter.feature.myitems.ui.MyItemsRoute

const val MY_ITEMS_ROUTE = "my-items"

fun NavGraphBuilder.myItemsScreen() {
    composable(MY_ITEMS_ROUTE) {
        MyItemsRoute()
    }
}
