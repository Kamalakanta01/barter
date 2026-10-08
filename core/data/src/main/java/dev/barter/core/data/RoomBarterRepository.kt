package dev.barter.core.data

import dev.barter.core.domain.BarterRepository
import dev.barter.core.domain.BarterRules
import dev.barter.core.domain.BarterState
import dev.barter.core.domain.Listing
import dev.barter.core.domain.SwapOffer
import dev.barter.core.domain.Trader
import dev.barter.core.domain.TraderId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the current [BarterState] in memory and mirrors every change to disk.
 *
 * This class is the only writer, so memory is always the truth and the database is its saved copy.
 * That is what lets the screen see each rule's result as one step: it never watches the tables
 * directly, so it can never catch a listing already marked traded while its offer still reads
 * pending. Writes run one at a time, each starting from the state the previous one left.
 */
@Singleton
internal class RoomBarterRepository @Inject constructor(
    private val dao: BarterDao,
    private val me: TraderId,
) : BarterRepository {

    private val traders = listOf(Trader(me, "You"))
    private val mutableState = MutableStateFlow(BarterState(traders = traders))
    override val state: StateFlow<BarterState> = mutableState.asStateFlow()

    private val writeLock = Mutex()
    private var loadedFromDisk = false

    init {
        // Load at start-up so the screens fill in at once. Every write also goes through
        // [current], so a tap that beats this load still starts from what is on disk.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { writeLock.withLock { current() } }
    }

    override suspend fun addListing(title: String, wants: String) =
        update { state, id -> BarterRules.addListing(state, id, me, title, wants) }

    override suspend fun removeListing(listingId: Long) =
        update { state, _ -> BarterRules.removeListing(state, listingId) }

    override suspend fun restoreListing(listing: Listing) =
        update { state, _ -> BarterRules.restoreListing(state, listing) }

    override suspend fun sendOffer(mineListingId: Long, theirsListingId: Long) =
        update { state, id -> BarterRules.sendOffer(state, id, mineListingId, theirsListingId) }

    override suspend fun acceptOffer(offerId: Long) =
        update { state, _ -> BarterRules.acceptOffer(state, offerId) }

    override suspend fun declineOffer(offerId: Long) =
        update { state, _ -> BarterRules.declineOffer(state, offerId) }

    override suspend fun withdrawOffer(offerId: Long) =
        update { state, _ -> BarterRules.withdrawOffer(state, offerId) }

    /** Applies [rule], saves the result, then publishes it. A rule that changes nothing writes nothing. */
    private suspend fun update(rule: (state: BarterState, newId: Long) -> BarterState) = writeLock.withLock {
        val before = current()
        val after = rule(before, before.nextId())
        if (after != before) {
            dao.replaceAll(after.listings.map(Listing::toEntity), after.offers.map(SwapOffer::toEntity))
            mutableState.value = after
        }
    }

    /** Must be called with [writeLock] held. */
    private suspend fun current(): BarterState {
        if (!loadedFromDisk) {
            mutableState.value = BarterState(
                traders = traders,
                listings = dao.listings().map(ListingEntity::toDomain),
                offers = dao.offers().map(OfferEntity::toDomain),
            )
            loadedFromDisk = true
        }
        return mutableState.value
    }
}

/**
 * One more than the highest id in use, shared by listings and offers. Reusing the number of a row
 * that was just removed is safe: undo only lasts a few seconds, and nothing keeps pointing at a
 * removed listing, because removing it also removes the offers that named it.
 */
internal fun BarterState.nextId(): Long =
    maxOf(listings.maxOfOrNull { it.id } ?: 0L, offers.maxOfOrNull { it.id } ?: 0L) + 1
