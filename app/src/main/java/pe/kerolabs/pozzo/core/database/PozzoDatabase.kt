package pe.kerolabs.pozzo.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local.GroupDao
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local.GroupEntity

/**
 * Local storage of the app (Room): a copy of the member's groups to open them without connection.
 * Each bounded context adds its own tables and DAO here.
 */
@Database(entities = [GroupEntity::class], version = 1, exportSchema = false)
abstract class PozzoDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
}
