package dev.barter.core.domain

/**
 * Everything the UI can render, and the only thing a [BarterRepository] publishes.
 *
 * Selections take the current user explicitly rather than hiding the filter, because the transport
 * layers will merge listings and offers from many traders into this one aggregate.
 */
data class BarterState(
    val traders: List<Trader> = emptyList(),
    val listings: List<Listing> = emptyList(),
    val offers: List<SwapOffer> = emptyList(),
) {
    fun listing(id: Long): Listing? = listings.firstOrNull { it.id == id }

    fun offer(id: Long): SwapOffer? = offers.firstOrNull { it.id == id }

    fun traderName(id: TraderId): String = traders.firstOrNull { it.id == id }?.displayName ?: id.shortForm

    fun myListings(myId: TraderId): List<Listing> = listings.filter { it.ownerId == myId }

    /** What I can still put up for trade: my own listings that have not been swapped away. */
    fun offerableMine(myId: TraderId): List<Listing> = listings.filter { it.ownerId == myId && !it.isTraded }

    fun tradable(myId: TraderId): List<Listing> = listings.filter { it.ownerId != myId && !it.isTraded }

    fun pendingIncoming(): List<SwapOffer> =
        offers.filter { it.side == SwapSide.INCOMING && it.status == SwapStatus.PENDING }
}
