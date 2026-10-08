package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Local copy of a group of the member, so the list opens without connection.
 */
@Entity(tableName = "my_groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val role: String,
    @ColumnInfo(name = "organizer_name") val organizerName: String,
    @ColumnInfo(name = "contribution_amount") val contributionAmount: String,
    val currency: String,
    val periodicity: String,
    val seats: Int,
    @ColumnInfo(name = "pot_amount") val potAmount: String,
    @ColumnInfo(name = "first_contribution_date") val firstContributionDate: String,
    @ColumnInfo(name = "destination_method") val destinationMethod: String?,
    @ColumnInfo(name = "destination_phone") val destinationPhone: String?,
    @ColumnInfo(name = "turn_method") val turnMethod: String?,
    @ColumnInfo(name = "my_turn_number") val myTurnNumber: Int?,
    @ColumnInfo(name = "my_turn_date") val myTurnDate: String?,
    @ColumnInfo(name = "group_full") val groupFull: Boolean,
    @ColumnInfo(name = "turns_assigned") val turnsAssigned: Boolean,
    @ColumnInfo(name = "destination_defined") val destinationDefined: Boolean,
    @ColumnInfo(name = "can_start") val canStart: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
