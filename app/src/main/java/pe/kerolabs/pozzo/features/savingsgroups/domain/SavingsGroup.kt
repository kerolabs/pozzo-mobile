package pe.kerolabs.pozzo.features.savingsgroups.domain

import java.math.BigDecimal
import java.time.LocalDate

enum class GroupStatus { DRAFT, READY, STARTED, CLOSED }

enum class GroupRole { ORGANIZER, PARTICIPANT }

enum class Periodicity { WEEKLY, BIWEEKLY, MONTHLY }

/**
 * Conditions to start a group.
 */
data class Readiness(
    val groupFull: Boolean,
    val turnsAssigned: Boolean,
    val destinationDefined: Boolean,
    val canStart: Boolean,
)

/**
 * A savings group the member belongs to, as the list of groups shows it.
 */
data class SavingsGroup(
    val id: String,
    val name: String,
    val status: GroupStatus,
    val role: GroupRole,
    val organizerName: String,
    val contributionAmount: BigDecimal,
    val currency: String,
    val periodicity: Periodicity,
    val seats: Int,
    val potAmount: BigDecimal,
    val firstContributionDate: LocalDate,
    val myTurnNumber: Int?,
    val myTurnDate: LocalDate?,
    val readiness: Readiness,
) {
    val isOrganizer: Boolean get() = role == GroupRole.ORGANIZER
}

interface SavingsGroupRepository {

    /** The groups kept on the phone, updated every time [refreshMyGroups] succeeds. */
    fun observeMyGroups(): kotlinx.coroutines.flow.Flow<List<SavingsGroup>>

    /** Downloads the groups of the member and replaces the local copy. */
    suspend fun refreshMyGroups(): Result<Unit>

    /** Removes the local copy, e.g. when the member signs out. */
    suspend fun clearLocalGroups()
}
