package dev.barter.core.domain

/** [INCOMING] offers need an answer from me; [OUTGOING] means I am waiting. */
enum class SwapSide { INCOMING, OUTGOING }

enum class SwapStatus { PENDING, ACCEPTED, DECLINED, CLOSED }

/**
 * A proposed swap between two listings. [mineListingId] and [theirsListingId] are always relative
 * to the current user, so incoming and outgoing offers share one shape.
 */
data class SwapOffer(
    val id: Long,
    val side: SwapSide,
    val mineListingId: Long,
    val theirsListingId: Long,
    val status: SwapStatus = SwapStatus.PENDING,
)
