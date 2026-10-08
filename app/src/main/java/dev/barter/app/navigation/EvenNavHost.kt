package dev.barter.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.barter.app.EvenViewModel
import dev.barter.feature.browse.navigation.BROWSE_ROUTE
import dev.barter.feature.browse.navigation.browseScreen
import dev.barter.feature.myitems.navigation.MY_ITEMS_ROUTE
import dev.barter.feature.myitems.navigation.myItemsScreen
import dev.barter.feature.offers.navigation.OFFERS_ROUTE
import dev.barter.feature.offers.navigation.offersScreen

@Composable
fun EvenNavHost(viewModel: EvenViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val pendingIncoming by viewModel.pendingIncoming.collectAsStateWithLifecycle()
    val selectTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(BROWSE_ROUTE) { saveState = true }
        }
    }

    Scaffold(
        topBar = {
            EvenTabBar(
                currentRoute = backStackEntry?.destination?.route,
                pendingIncoming = pendingIncoming,
                onSelect = selectTab,
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BROWSE_ROUTE,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            browseScreen(
                onListItem = { selectTab(MY_ITEMS_ROUTE) },
                onOfferSent = { selectTab(OFFERS_ROUTE) },
            )
            myItemsScreen()
            offersScreen()
        }
    }
}