package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.repositories

import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupRole
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupStatus
import pe.kerolabs.pozzo.features.savingsgroups.domain.Periodicity
import pe.kerolabs.pozzo.features.savingsgroups.domain.Readiness
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroupRepository
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local.GroupDao
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local.GroupEntity
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote.GroupDto
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote.GroupsService

/**
 * The list of groups is read from Room and refreshed from the backend, so it opens without connection.
 */
class SavingsGroupRepositoryImpl @Inject constructor(
    private val service: GroupsService,
    private val dao: GroupDao,
) : SavingsGroupRepository {

    override fun observeMyGroups(): Flow<List<SavingsGroup>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshMyGroups(): Result<Unit> =
        apiCall { service.getMyGroups() }.map { groups -> dao.replaceAll(groups.map { it.toEntity() }) }

    override suspend fun clearLocalGroups() {
        dao.deleteAll()
    }

    private fun GroupDto.toEntity() = GroupEntity(
        id = id,
        name = name,
        status = status,
        role = role,
        organizerName = organizerName,
        contributionAmount = rules.contributionAmount.toPlainString(),
        currency = rules.currency,
        periodicity = rules.periodicity,
        seats = rules.seats,
        potAmount = rules.potAmount.toPlainString(),
        firstContributionDate = rules.firstContributionDate,
        myTurnNumber = myTurnNumber,
        myTurnDate = myTurnDate,
        groupFull = readiness.groupFull,
        turnsAssigned = readiness.turnsAssigned,
        destinationDefined = readiness.destinationDefined,
        canStart = readiness.canStart,
        createdAt = createdAt,
    )

    private fun GroupEntity.toDomain() = SavingsGroup(
        id = id,
        name = name,
        status = GroupStatus.valueOf(status),
        role = GroupRole.valueOf(role),
        organizerName = organizerName,
        contributionAmount = BigDecimal(contributionAmount),
        currency = currency,
        periodicity = Periodicity.valueOf(periodicity),
        seats = seats,
        potAmount = BigDecimal(potAmount),
        firstContributionDate = LocalDate.parse(firstContributionDate),
        myTurnNumber = myTurnNumber,
        myTurnDate = myTurnDate?.let(LocalDate::parse),
        readiness = Readiness(groupFull, turnsAssigned, destinationDefined, canStart),
    )
}
