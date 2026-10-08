package dev.barter.feature.offers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barter.core.domain.BarterRepository
import dev.barter.core.domain.BarterState
import dev.barter.core.domain.SwapOffer
import dev.barter.core.domain.SwapSide
import dev.barter.core.domain.SwapStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * How an offer reads to the user. Kept as an enum rather than a string so the copy stays in
 * resources; [OfferRow.theirsOwnerName] is the format argument for the two notes that name someone.
 */
enum class OfferNote { SENT_TO_YOU, WAITING_ON_THEM, SWAPPED, DECLINED, NO_LONGER_AVAILABLE }

data class OfferRow(
    val id: Long,
    val mineTitle: String,
    val theirsTitle: String,
    val theirsOwnerName: String,
    val note: OfferNote,
    /** Incoming and still unanswered. */
    val canRespond: Boolean,
    /** Outgoing and still awaiting a reply. */
    val canWithdraw: Boolean,
)

data class OffersUiState(
    val incoming: List<OfferRow> = emptyList(),
    val outgoing: List<OfferRow> = emptyList(),
) {
    val isEmpty: Boolean get() = incoming.isEmpty() && outgoing.isEmpty()
}

/** What the user just did to an offer, so the screen can confirm it. */
enum class OfferOutcome { ACCEPTED, DECLINED, WITHDRAWN }

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val repository: BarterRepository,
) : ViewModel() {

    private val outcomes = Channel<OfferOutcome>(Channel.BUFFERED)
    val offerOutcomes: Flow<OfferOutcome> = outcomes.receiveAsFlow()

    val uiState: StateFlow<OffersUiState> = repository.state
        .map { state ->
            OffersUiState(
                incoming = state.offers.withSide(SwapSide.INCOMING).mapNotNull(state::toRow),
                outgoing = state.offers.withSide(SwapSide.OUTGOING).mapNotNull(state::toRow),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OffersUiState(),
        )

    fun onAccept(offerId: Long) = respond(offerId, OfferOutcome.ACCEPTED) { repository.acceptOffer(it) }

    fun onDecline(offerId: Long) = respond(offerId, OfferOutcome.DECLINED) { repository.declineOffer(it) }

    fun onWithdraw(offerId: Long) = respond(offerId, OfferOutcome.WITHDRAWN) { repository.withdrawOffer(it) }

    /**
     * The rules ignore an offer that no longer needs an answer, so without this guard a second tap
     * would report success for something that did not happen.
     */
    private fun respond(offerId: Long, outcome: OfferOutcome, action: suspend (Long) -> Unit) {
        viewModelScope.launch {
            if (repository.state.value.offer(offerId)?.status != SwapStatus.PENDING) return@launch
            action(offerId)
            outcomes.send(outcome)
        }
    }
}

/**
 * An offer whose listing is missing is dropped rather than rendered as a blank row. `removeListing`
 * already takes the referencing offers with it, but once listings arrive from other phones an offer
 * can be seen before the listing it points at.
 */
private fun BarterState.toRow(offer: SwapOffer): OfferRow? {
    val mine = listing(offer.mineListingId) ?: return null
    val theirs = listing(offer.theirsListingId) ?: return null
    return OfferRow(
        id = offer.id,
        mineTitle = mine.title,
        theirsTitle = theirs.title,
        theirsOwnerName = traderName(theirs.ownerId),
        note = offer.note(),
        canRespond = offer.side == SwapSide.INCOMING && offer.status == SwapStatus.PENDING,
        canWithdraw = offer.side == SwapSide.OUTGOING && offer.status == SwapStatus.PENDING,
    )
}

private fun SwapOffer.note(): OfferNote = when {
    status == SwapStatus.ACCEPTED -> OfferNote.SWAPPED
    status == SwapStatus.DECLINED -> OfferNote.DECLINED
    status == SwapStatus.CLOSED -> OfferNote.NO_LONGER_AVAILABLE
    side == SwapSide.INCOMING -> OfferNote.SENT_TO_YOU
    else -> OfferNote.WAITING_ON_THEM
}

private fun List<SwapOffer>.withSide(side: SwapSide) = filter { it.side == side }
