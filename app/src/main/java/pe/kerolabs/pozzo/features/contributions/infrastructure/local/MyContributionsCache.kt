package pe.kerolabs.pozzo.features.contributions.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert

/**
 * Last copy of the member's contributions in a group, as the backend sent it, so the receipts can be
 * shown without connection.
 */
@Entity(tableName = "my_contributions")
data class MyContributionsEntity(
    @PrimaryKey @ColumnInfo(name = "group_id") val groupId: String,
    val json: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)

@Dao
abstract class MyContributionsDao {

    @Query("SELECT * FROM my_contributions WHERE group_id = :groupId")
    abstract suspend fun find(groupId: String): MyContributionsEntity?

    @Upsert
    abstract suspend fun upsert(entity: MyContributionsEntity)

    @Query("DELETE FROM my_contributions")
    abstract suspend fun deleteAll()
}
