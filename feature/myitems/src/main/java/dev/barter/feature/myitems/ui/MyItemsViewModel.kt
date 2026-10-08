package dev.barter.feature.myitems.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barter.core.domain.BarterRepository
import dev.barter.core.domain.BarterState
import dev.barter.core.domain.Listing
import dev.barter.core.domain.SwapStatus
import dev.barter.core.domain.TraderId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyListing(
    val id: Long,
    val title: String,
    val wants: String,
    val isTraded: Boolean,
    /** Offers still awaiting an answer that involve this listing; removing it cancels them. */
    val pendingOffers: Int = 0,
)

data class MyItemsUiState(
    val listings: List<MyListing> = emptyList(),
    val isAddDialogVisible: Boolean = false,
    /** Non-null while asking the user to confirm removing a listing that has pending offers. */
    val removalToConfirm: MyListing? = null,
)

@HiltViewModel
class MyItemsViewModel @Inject constructor(
    private val repository: BarterRepository,
    private val me: TraderId,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    /**
     * Survives process death, so the dialog is still open after the app is killed and reopened rather
     * than silently losing whatever the user had typed into it.
     */
    private val isAddDialogVisible = MutableStateFlow(savedState[KEY_ADD_DIALOG] ?: false)

    // Deliberately not in SavedStateHandle: the dialog survives rotation because this ViewModel does,
    // and after the process is killed it is better to ask nothing than to resume a half-made decision.
    private val removalToConfirmId = MutableStateFlow<Long?>(null)

    private val removals = Channel<Listing>(Channel.BUFFERED)

    /** Emits each listing as it is taken down, so the screen can say which one and offer an undo. */
    val removedListings: Flow<Listing> = removals.receiveAsFlow()

    val uiState: StateFlow<MyItemsUiState> = combine(
        repository.state.map { state -> state.myListings(me).map(state::toMyListing) },
        isAddDialogVisible,
        removalToConfirmId,
    ) { listings, addVisible, confirmId ->
        MyItemsUiState(
            listings = listings,
            isAddDialogVisible = addVisible,
            removalToConfirm = listings.firstOrNull { it.id == confirmId },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyItemsUiState(),
    )

    fun onAddClicked() = setAddDialogVisible(true)

    fun onAddDismissed() = setAddDialogVisible(false)

    fun onAddConfirmed(title: String, wants: String) {
        setAddDialogVisible(false)
        viewModelScope.launch { repository.addListing(title = title, wants = wants) }
    }

    /** Removing a listing cancels its pending offers for good, so the user is asked first. */
    fun onRemove(listing: MyListing) {
        if (listing.pendingOffers > 0) removalToConfirmId.value = listing.id else remove(listing.id)
    }

    fun onRemoveConfirmed() {
        val id = removalToConfirmId.value ?: return
        removalToConfirmId.value = null
        remove(id)
    }

    fun onRemoveDismissed() {
        removalToConfirmId.value = null
    }

    private fun remove(listingId: Long) {
        val existing = repository.state.value.listing(listingId) ?: return
        viewModelScope.launch {
            repository.removeListing(listingId)
            removals.send(existing)
        }
    }

    /** Puts back [listing], the one this undo was offered for, which is not always the latest removal. */
    fun onUndoRemove(listing: Listing) {
        viewModelScope.launch { repository.restoreListing(listing) }
    }

    private fun setAddDialogVisible(visible: Boolean) {
        savedState[KEY_ADD_DIALOG] = visible
        isAddDialogVisible.value = visible
    }

    private companion object {
        const val KEY_ADD_DIALOG = "addDialogVisible"
    }
}

private fun BarterState.toMyListing(listing: Listing) = MyListing(
    id = listing.id,
    title = listing.title,
    wants = listing.wants,
    isTraded = listing.isTraded,
    pendingOffers = offers.count {
        it.status == SwapStatus.PENDING && (it.mineListingId == listing.id || it.theirsListingId == listing.id)
    },
)
