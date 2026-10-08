package dev.barter.core.domain

/**
 * A pseudonymous, stable identity. Never the phone number itself: production ids are HMACs of the
 * number (see `TraderIds.fromPhoneNumber`), so the number itself never leaves the device.
 *
 * Deliberately not a `value class` — Dagger mangles the generated factory name for inline classes
 * and fails with "not a valid name", and [TraderId] has to be injectable.
 */
data class TraderId(val hex: String) {
    init {
        require(hex.isNotBlank()) { "TraderId must not be blank" }
    }

    /** What you see when no display name is known for this trader. */
    val shortForm: String get() = hex.take(SHORT_LENGTH).uppercase()

    private companion object {
        const val SHORT_LENGTH = 6
    }
}

data class Trader(
    val id: TraderId,
    val displayName: String,
)
