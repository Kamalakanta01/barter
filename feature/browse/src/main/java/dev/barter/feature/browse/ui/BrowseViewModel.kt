package dev.barter.feature.browse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barter.core.domain.BarterRepository
import dev.barter.core.domain.BarterState
import dev.barter.core.domain.Listing
import dev.barter.core.domain.TraderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A listing as this screen needs it. Trades are resolved to a display name in the ViewModel so the
 * composable never has to look names up.
 */
data class BrowseListing(
    val id: Long,
    val title: String,
    val ownerName: String,
    val wants: String,
)

data class BrowseUiState(
    val query: String = "",
    val listings: List<BrowseListing> = emptyList(),
    val offerable: List<BrowseListing> = emptyList(),
    /** Non-null while the swap dialog is open. */
    val swapTarget: BrowseListing? = null,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: BarterRepository,
    private val me: TraderId,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val swapTargetId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<BrowseUiState> =
        combine(repository.state, query, swapTargetId) { state, search, targetId ->
            BrowseUiState(
                query = search,
                listings = state.tradable(me).matching(search).map(state::toBrowseListing),
                offerable = state.offerableMine(me).map(state::toBrowseListing),
                swapTarget = state.listing(targetId ?: -1)?.let(state::toBrowseListing),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrowseUiState(),
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onListingClick(listing: BrowseListing) {
        swapTargetId.value = listing.id
    }

    fun onSwapDismissed() {
        swapTargetId.value = null
    }

    fun onSendOffer(mineListingId: Long) {
        val theirsId = swapTargetId.value ?: return
        viewModelScope.launch {
            repository.sendOffer(mineListingId = mineListingId, theirsListingId = theirsId)
            swapTargetId.value = null
        }
    }
}

private fun BarterState.toBrowseListing(listing: Listing) = BrowseListing(
    id = listing.id,
    title = listing.title,
    ownerName = traderName(listing.ownerId),
    wants = listing.wants,
)

/** Matches the prototype's filter, which searched the title and the wants text together. */
private fun List<Listing>.matching(query: String): List<Listing> {
    if (query.isBlank()) return this
    return filter { "${it.title} ${it.wants}".contains(query, ignoreCase = true) }
}
