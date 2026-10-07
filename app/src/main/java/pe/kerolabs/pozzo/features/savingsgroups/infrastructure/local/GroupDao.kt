package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GroupDao {

    @Query("SELECT * FROM my_groups ORDER BY created_at DESC")
    abstract fun observeAll(): Flow<List<GroupEntity>>

    @Upsert
    abstract suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM my_groups")
    abstract suspend fun deleteAll()

    /** Replaces the local copy in one transaction, so a group the member left disappears. */
    @Transaction
    open suspend fun replaceAll(groups: List<GroupEntity>) {
        deleteAll()
        upsertAll(groups)
    }
}
