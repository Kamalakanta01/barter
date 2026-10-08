package dev.barter.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarterRulesTest {

    private val me = TraderId("me")
    private val ana = TraderId("ana")
    private val mei = TraderId("mei")

    private val state = BarterState(
        traders = listOf(Trader(me, "You"), Trader(ana, "Ana"), Trader(mei, "Mei")),
        listings = listOf(
            Listing(1, ana, "Film camera", "Bike lights"),
            Listing(3, mei, "Ceramic mug set", "Houseplants"),
            Listing(101, me, "Mechanical keyboard", "Plants"),
            Listing(102, me, "Road bike", "Camera gear"),
        ),
        offers = listOf(SwapOffer(201, SwapSide.INCOMING, mineListingId = 101, theirsListingId = 3)),
    )

    @Test
    fun `addListing appends a listing owned by the given trader`() {
        val next = BarterRules.addListing(state, id = 7, ownerId = me, title = "Drill", wants = "A lamp")

        assertEquals(Listing(7, me, "Drill", "A lamp"), next.listing(7))
    }

    @Test
    fun `removeListing also drops offers that referenced it`() {
        val next = BarterRules.removeListing(state, listingId = 101)

        assertEquals(null, next.listing(101))
        assertTrue(next.offers.isEmpty())
    }

    @Test
    fun `restoreListing puts the listing back in id order`() {
        val removed = state.listing(101)!!

        val next = BarterRules.restoreListing(BarterRules.removeListing(state, listingId = 101), removed)

        assertEquals(listOf(1L, 3L, 101L, 102L), next.listings.map { it.id })
    }

    @Test
    fun `acceptOffer marks both listings traded and closes colliding pending offers`() {
        val withOutgoing = BarterRules.sendOffer(state, id = 202, mineListingId = 102, theirsListingId = 3)

        val next = BarterRules.acceptOffer(withOutgoing, offerId = 201)

        assertTrue(next.listing(101)!!.isTraded)
        assertTrue(next.listing(3)!!.isTraded)
        assertFalse(next.listing(102)!!.isTraded)
        assertEquals(SwapStatus.ACCEPTED, next.offer(201)!!.status)
        assertEquals(SwapStatus.CLOSED, next.offer(202)!!.status)
    }

    @Test
    fun `acceptOffer leaves unrelated pending offers alone`() {
        val withUnrelated = BarterRules.sendOffer(state, id = 203, mineListingId = 102, theirsListingId = 1)

        val next = BarterRules.acceptOffer(withUnrelated, offerId = 201)

        assertEquals(SwapStatus.PENDING, next.offer(203)!!.status)
    }

    @Test
    fun `declineOffer leaves both listings available`() {
        val next = BarterRules.declineOffer(state, offerId = 201)

        assertEquals(SwapStatus.DECLINED, next.offer(201)!!.status)
        assertFalse(next.listing(101)!!.isTraded)
        assertFalse(next.listing(3)!!.isTraded)
    }

    @Test
    fun `withdrawOffer removes the offer entirely`() {
        val next = BarterRules.withdrawOffer(state, offerId = 201)

        assertTrue(next.offers.isEmpty())
    }

    @Test
    fun `sendOffer records an outgoing offer`() {
        val next = BarterRules.sendOffer(state, id = 202, mineListingId = 101, theirsListingId = 1)

        assertEquals(
            SwapOffer(202, SwapSide.OUTGOING, mineListingId = 101, theirsListingId = 1),
            next.offer(202),
        )
    }

    @Test
    fun `addListing ignores a listing with a blank field`() {
        assertEquals(state, BarterRules.addListing(state, id = 7, ownerId = me, title = "  ", wants = "A lamp"))
        assertEquals(state, BarterRules.addListing(state, id = 7, ownerId = me, title = "Drill", wants = ""))
    }

    @Test
    fun `sendOffer ignores a listing that does not exist`() {
        assertEquals(state, BarterRules.sendOffer(state, id = 202, mineListingId = 102, theirsListingId = 999))
    }

    @Test
    fun `sendOffer ignores an offer against an already traded listing`() {
        val withTraded = state.copy(
            listings = state.listings + Listing(4, mei, "Cast iron skillet", "Board games", isTraded = true),
        )

        assertEquals(
            withTraded,
            BarterRules.sendOffer(withTraded, id = 202, mineListingId = 102, theirsListingId = 4),
        )
    }

    @Test
    fun `sendOffer ignores two of my own listings`() {
        assertEquals(state, BarterRules.sendOffer(state, id = 202, mineListingId = 101, theirsListingId = 102))
    }

    @Test
    fun `sendOffer ignores a second proposal for the same pair in either direction`() {
        val offered = BarterRules.sendOffer(state, id = 202, mineListingId = 101, theirsListingId = 1)

        assertEquals(offered, BarterRules.sendOffer(offered, id = 203, mineListingId = 1, theirsListingId = 101))
    }

    @Test
    fun `acceptOffer ignores an offer that does not exist`() {
        assertEquals(state, BarterRules.acceptOffer(state, offerId = 999))
    }

    @Test
    fun `acceptOffer ignores an offer that was already answered`() {
        val declined = BarterRules.declineOffer(state, offerId = 201)

        val next = BarterRules.acceptOffer(declined, offerId = 201)

        assertEquals(declined, next)
        assertFalse(next.listing(101)!!.isTraded)
    }

    @Test
    fun `declineOffer ignores an offer that was already accepted`() {
        val accepted = BarterRules.acceptOffer(state, offerId = 201)

        assertEquals(accepted, BarterRules.declineOffer(accepted, offerId = 201))
    }

    @Test
    fun `withdrawOffer ignores an offer that was already declined`() {
        val declined = BarterRules.declineOffer(state, offerId = 201)

        assertEquals(declined, BarterRules.withdrawOffer(declined, offerId = 201))
    }
}

class BarterStateTest {

    private val me = TraderId("me")
    private val ana = TraderId("ana")

    private val state = BarterState(
        traders = listOf(Trader(me, "You"), Trader(ana, "Ana")),
        listings = listOf(
            Listing(1, ana, "Film camera", "Bike lights"),
            Listing(2, ana, "Cast iron skillet", "Board games", isTraded = true),
            Listing(101, me, "Mechanical keyboard", "Plants"),
            Listing(102, me, "Road bike", "Camera gear", isTraded = true),
        ),
        offers = listOf(
            SwapOffer(201, SwapSide.INCOMING, 101, 1),
            SwapOffer(202, SwapSide.OUTGOING, 102, 1),
            SwapOffer(203, SwapSide.INCOMING, 102, 1, status = SwapStatus.ACCEPTED),
        ),
    )

    @Test
    fun `myListings excludes listings owned by others`() {
        assertEquals(listOf(101L, 102L), state.myListings(me).map { it.id })
    }

    @Test
    fun `offerableMine excludes my traded listings`() {
        assertEquals(listOf(101L), state.offerableMine(me).map { it.id })
    }

    @Test
    fun `tradable excludes my listings and anything already traded`() {
        assertEquals(listOf(1L), state.tradable(me).map { it.id })
    }

    @Test
    fun `pendingIncoming counts only unanswered incoming offers`() {
        assertEquals(listOf(201L), state.pendingIncoming().map { it.id })
    }

    @Test
    fun `traderName falls back to the short id when the name is unknown`() {
        assertEquals("Ana", state.traderName(ana))
        assertEquals("ABCDEF", state.traderName(TraderId("abcdef")))
    }
}
