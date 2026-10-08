package dev.barter.core.identity

import dev.barter.core.domain.TraderId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The id this install presents as.
 *
 * Phase 2 replaces [value] with an HMAC of the SIM number (see [TraderIds.fromPhoneNumber]), falling
 * back to a Keystore-stored random id when the carrier reports no number — emulators and some
 * carriers always return `null` from `getLine1Number()`. Until then it is a fixed constant.
 *
 * Careful when replacing it: every saved listing records its owner as this id, so a different id
 * would make the user's existing items look like someone else's. Ship a Room migration that rewrites
 * the owner of existing rows in the same release.
 */
@Module
@InstallIn(SingletonComponent::class)
object IdentityModule {

    @Provides
    @Singleton
    fun providesLocalTraderId(): TraderId = LocalTraderId
}

val LocalTraderId: TraderId = TraderIds.local()
