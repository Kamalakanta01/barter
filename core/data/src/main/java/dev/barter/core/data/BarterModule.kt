package dev.barter.core.data

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.barter.core.domain.BarterRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BarterModule {

    @Binds
    @Singleton
    abstract fun bindsBarterRepository(impl: RoomBarterRepository): BarterRepository

    companion object {

        @Provides
        @Singleton
        fun providesDatabase(@ApplicationContext context: Context): BarterDatabase =
            Room.databaseBuilder(context, BarterDatabase::class.java, "barter.db").build()

        @Provides
        fun providesDao(database: BarterDatabase): BarterDao = database.dao()
    }
}
