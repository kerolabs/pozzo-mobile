package pe.kerolabs.pozzo.core.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kerolabs.pozzo.core.database.PozzoDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePozzoDatabase(@ApplicationContext context: Context): PozzoDatabase =
        Room.databaseBuilder<PozzoDatabase>(context, "pozzo-database")
            .setDriver(AndroidSQLiteDriver())
            // The database is only a cache of the backend, so it is rebuilt when its schema changes.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}
