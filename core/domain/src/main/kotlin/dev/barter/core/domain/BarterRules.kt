package dev.barter.core.domain

/**
 * Every legal transition of the barter aggregate, as pure `BarterState` to `BarterState` functions.
 *
 * These live in domain rather than in a repository implementation so that swapping the temporary
 * in-memory store for Room changes no logic, and so the rules can be tested without Android or Hilt.
 * Callers supply new ids, which keeps this object free of any id-generation policy.
 *
 * A rule asked for something impossible returns the state unchanged. Once listings arrive from other
 * phones over the transport, an offer can be accepted or withdrawn on one device while the other is
 * acting on it, so "impossible" stops being a programming error and becomes a race both devices can
 * lose. No-op is therefore a defined outcome rather than an exception.
 */
object BarterRules {

    fun addListing(
        state: BarterState,
        id: Long,
        ownerId: TraderId,
        title: String,
        wants: String,
    ): BarterState = if (title.isBlank() || wants.isBlank()) {
        state
    } else {
        state.copy(
            listings = state.listings + Listing(id = id, ownerId = ownerId, title = title, wants = wants),
        )
    }

    /** Removing a listing also drops any offer that referenced it, so no offer points at nothing. */
    fun removeListing(state: BarterState, listingId: Long): BarterState = state.copy(
        listings = state.listings.filterNot { it.id == listingId },
        offers = state.offers.filterNot { it.mineListingId == listingId || it.theirsListingId == listingId },
    )

    /**
     * Puts a removed listing back with its original id, so an undo does not invalidate anything that
     * already referenced it, and in id order, so it returns to its old place in the grid. Offers
     * dropped by [removeListing] are not resurrected.
     */
    fun restoreListing(state: BarterState, listing: Listing): BarterState =
        if (state.listing(listing.id) != null) {
            state
        } else {
            state.copy(listings = (state.listings + listing).sortedBy { it.id })
        }

    fun sendOffer(
        state: BarterState,
        id: Long,
        mineListingId: Long,
        theirsListingId: Long,
    ): BarterState {
        val mine = state.listing(mineListingId) ?: return state
        val theirs = state.listing(theirsListingId) ?: return state
        if (mine.ownerId == theirs.ownerId) return state
        if (mine.isTraded || theirs.isTraded) return state
        val alreadyProposed = state.offers.any {
            it.status == SwapStatus.PENDING && it.offersBetween(mineListingId, theirsListingId)
        }
        if (alreadyProposed) return state

        return state.copy(
            offers = state.offers + SwapOffer(
                id = id,
                side = SwapSide.OUTGOING,
                mineListingId = mineListingId,
                theirsListingId = theirsListingId,
            ),
        )
    }

    /**
     * Accepting marks both listings traded and closes every other pending offer touching either of
     * them. Done in one transition so there is never an observable moment where a listing is traded
     * but a conflicting offer still looks live.
     */
    fun acceptOffer(state: BarterState, offerId: Long): BarterState {
        val accepted = state.offers.firstOrNull { it.id == offerId } ?: return state
        if (accepted.status != SwapStatus.PENDING) return state
        val involved = setOf(accepted.mineListingId, accepted.theirsListingId)

        return state.copy(
            listings = state.listings.map { if (it.id in involved) it.copy(isTraded = true) else it },
            offers = state.offers.map { offer ->
                when {
                    offer.id == offerId -> offer.copy(status = SwapStatus.ACCEPTED)
                    offer.status == SwapStatus.PENDING && offer.touchesAnyOf(involved) ->
                        offer.copy(status = SwapStatus.CLOSED)

                    else -> offer
                }
            },
        )
    }

    fun declineOffer(state: BarterState, offerId: Long): BarterState {
        if (state.pendingOffer(offerId) == null) return state
        return state.copy(
            offers = state.offers.map { if (it.id == offerId) it.copy(status = SwapStatus.DECLINED) else it },
        )
    }

    fun withdrawOffer(state: BarterState, offerId: Long): BarterState {
        if (state.pendingOffer(offerId) == null) return state
        return state.copy(offers = state.offers.filterNot { it.id == offerId })
    }

    /** The offer only if it still needs an answer, which is what both answerable rules require. */
    private fun BarterState.pendingOffer(offerId: Long): SwapOffer? =
        offers.firstOrNull { it.id == offerId && it.status == SwapStatus.PENDING }

    private fun SwapOffer.touchesAnyOf(listingIds: Set<Long>): Boolean =
        mineListingId in listingIds || theirsListingId in listingIds

    /** Direction-agnostic, because the same pair meeting twice in opposite directions is still one negotiation. */
    private fun SwapOffer.offersBetween(first: Long, second: Long): Boolean =
        (mineListingId == first && theirsListingId == second) ||
            (mineListingId == second && theirsListingId == first)
}
