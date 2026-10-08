package dev.barter.core.domain

/**
 * One thing a trader owns and is willing to trade. [wants] is what they would take in return.
 *
 * Ownership is a [TraderId] rather than a boolean so a listing received from a nearby phone is
 * indistinguishable from one that arrived over the global transport.
 */
data class Listing(
    val id: Long,
    val ownerId: TraderId,
    val title: String,
    val wants: String,
    val isTraded: Boolean = false,
)
