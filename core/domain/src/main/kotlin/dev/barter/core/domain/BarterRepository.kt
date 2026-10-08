package dev.barter.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth. Implementations apply [BarterRules] and publish the result; features
 * only read [state] and issue commands.
 */
interface BarterRepository {

    val state: StateFlow<BarterState>

    suspend fun addListing(title: String, wants: String)

    suspend fun removeListing(listingId: Long)

    /** Undoes [removeListing] for the same listing, keeping its id. */
    suspend fun restoreListing(listing: Listing)

    suspend fun sendOffer(mineListingId: Long, theirsListingId: Long)

    suspend fun acceptOffer(offerId: Long)

    suspend fun declineOffer(offerId: Long)

    suspend fun withdrawOffer(offerId: Long)
}
