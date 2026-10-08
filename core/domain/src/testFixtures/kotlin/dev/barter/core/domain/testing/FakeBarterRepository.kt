package dev.barter.core.domain.testing

import dev.barter.core.domain.BarterRepository
import dev.barter.core.domain.BarterRules
import dev.barter.core.domain.BarterState
import dev.barter.core.domain.Listing
import dev.barter.core.domain.SwapOffer
import dev.barter.core.domain.SwapSide
import dev.barter.core.domain.Trader
import dev.barter.core.domain.TraderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A repository feature tests can drive directly. It applies the same [BarterRules] the real store
 * does, so ViewModel tests exercise production rules without needing Hilt or Android.
 */
class FakeBarterRepository : BarterRepository {

    val ana = TraderId("ana")
    val mei = TraderId("mei")
    val me = TraderId("me")

    private val mutableState = MutableStateFlow(seed())
    override val state: StateFlow<BarterState> = mutableState.asStateFlow()

    private var nextId = 300L

    private fun seed() = BarterState(
        traders = listOf(Trader(me, "You"), Trader(ana, "Ana"), Trader(mei, "Mei")),
        listings = listOf(
            Listing(1, ana, "Film camera", "Bike lights"),
            Listing(2, mei, "Ceramic mug set", "Houseplants"),
            Listing(101, me, "Mechanical keyboard", "Plants"),
            Listing(102, me, "Road bike", "Camera gear"),
        ),
        offers = listOf(SwapOffer(201, SwapSide.INCOMING, mineListingId = 101, theirsListingId = 2)),
    )

    override suspend fun addListing(title: String, wants: String) {
        val id = nextId++
        mutableState.update { BarterRules.addListing(it, id, me, title, wants) }
    }

    override suspend fun removeListing(listingId: Long) {
        mutableState.update { BarterRules.removeListing(it, listingId) }
    }

    override suspend fun restoreListing(listing: Listing) {
        mutableState.update { BarterRules.restoreListing(it, listing) }
    }

    override suspend fun sendOffer(mineListingId: Long, theirsListingId: Long) {
        val id = nextId++
        mutableState.update { BarterRules.sendOffer(it, id, mineListingId, theirsListingId) }
    }

    override suspend fun acceptOffer(offerId: Long) {
        mutableState.update { BarterRules.acceptOffer(it, offerId) }
    }

    override suspend fun declineOffer(offerId: Long) {
        mutableState.update { BarterRules.declineOffer(it, offerId) }
    }

    override suspend fun withdrawOffer(offerId: Long) {
        mutableState.update { BarterRules.withdrawOffer(it, offerId) }
    }
}
