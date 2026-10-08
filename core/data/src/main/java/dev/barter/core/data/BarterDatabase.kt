package dev.barter.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import dev.barter.core.domain.Listing
import dev.barter.core.domain.SwapOffer
import dev.barter.core.domain.SwapSide
import dev.barter.core.domain.SwapStatus
import dev.barter.core.domain.TraderId

/**
 * The on-disk shape of a [Listing]. Kept separate from the domain type so a change to how rows are
 * stored never leaks into the rules.
 */
@Entity(tableName = "listings")
internal data class ListingEntity(
    @PrimaryKey val id: Long,
    val ownerId: String,
    val title: String,
    val wants: String,
    val isTraded: Boolean,
)

@Entity(tableName = "offers")
internal data class OfferEntity(
    @PrimaryKey val id: Long,
    val side: SwapSide,
    val mineListingId: Long,
    val theirsListingId: Long,
    val status: SwapStatus,
)

@Dao
internal abstract class BarterDao {

    @Query("SELECT * FROM listings ORDER BY id")
    abstract suspend fun listings(): List<ListingEntity>

    @Query("SELECT * FROM offers ORDER BY id")
    abstract suspend fun offers(): List<OfferEntity>

    /**
     * Writes a whole new state in one transaction, so a crash can never leave a listing traded
     * without its offer accepted. Rewriting everything is fine at this size; revisit if a trader ever
     * holds thousands of rows.
     */
    @Transaction
    open suspend fun replaceAll(listings: List<ListingEntity>, offers: List<OfferEntity>) {
        clearListings()
        clearOffers()
        insertListings(listings)
        insertOffers(offers)
    }

    @Query("DELETE FROM listings")
    abstract suspend fun clearListings()

    @Query("DELETE FROM offers")
    abstract suspend fun clearOffers()

    @Insert
    abstract suspend fun insertListings(listings: List<ListingEntity>)

    @Insert
    abstract suspend fun insertOffers(offers: List<OfferEntity>)
}

/**
 * Version 1. There is deliberately no destructive-migration fallback: if a later version forgets to
 * ship a migration the app should fail loudly in testing, not quietly erase people's items.
 */
@Database(entities = [ListingEntity::class, OfferEntity::class], version = 1, exportSchema = true)
internal abstract class BarterDatabase : RoomDatabase() {
    abstract fun dao(): BarterDao
}

internal fun ListingEntity.toDomain() = Listing(
    id = id,
    ownerId = TraderId(ownerId),
    title = title,
    wants = wants,
    isTraded = isTraded,
)

internal fun Listing.toEntity() = ListingEntity(
    id = id,
    ownerId = ownerId.hex,
    title = title,
    wants = wants,
    isTraded = isTraded,
)

internal fun OfferEntity.toDomain() = SwapOffer(
    id = id,
    side = side,
    mineListingId = mineListingId,
    theirsListingId = theirsListingId,
    status = status,
)

internal fun SwapOffer.toEntity() = OfferEntity(
    id = id,
    side = side,
    mineListingId = mineListingId,
    theirsListingId = theirsListingId,
    status = status,
)
