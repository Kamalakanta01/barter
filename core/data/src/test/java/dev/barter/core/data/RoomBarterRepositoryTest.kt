package dev.barter.core.data

import dev.barter.core.domain.BarterState
import dev.barter.core.domain.Listing
import dev.barter.core.domain.SwapOffer
import dev.barter.core.domain.SwapSide
import dev.barter.core.domain.SwapStatus
import dev.barter.core.domain.TraderId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rules are covered in `core:domain` and Room itself needs a device to run, so these cover the
 * parts of the storage adapter that are plain Kotlin: translating to and from rows, and id handout.
 */
class RoomBarterRepositoryTest {

    private val owner = TraderId("abc123")

    @Test
    fun `a listing survives a round trip through its stored form`() {
        val listing = Listing(id = 7, ownerId = owner, title = "Drill", wants = "A lamp", isTraded = true)

        assertEquals(listing, listing.toEntity().toDomain())
    }

    @Test
    fun `an offer survives a round trip through its stored form`() {
        val offer = SwapOffer(
            id = 9,
            side = SwapSide.INCOMING,
            mineListingId = 7,
            theirsListingId = 3,
            status = SwapStatus.DECLINED,
        )

        assertEquals(offer, offer.toEntity().toDomain())
    }

    @Test
    fun `the first id on an empty store is 1`() {
        assertEquals(1L, BarterState().nextId())
    }

    @Test
    fun `new ids start after the highest listing or offer id`() {
        val state = BarterState(
            listings = listOf(Listing(id = 5, ownerId = owner, title = "Drill", wants = "A lamp")),
            offers = listOf(SwapOffer(id = 9, side = SwapSide.OUTGOING, mineListingId = 5, theirsListingId = 2)),
        )

        assertEquals(10L, state.nextId())
    }
}
