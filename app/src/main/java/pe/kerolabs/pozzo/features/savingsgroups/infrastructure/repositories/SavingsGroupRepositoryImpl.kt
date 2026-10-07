package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.repositories

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupPreview
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupRole
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupStatus
import pe.kerolabs.pozzo.features.savingsgroups.domain.Invitation
import pe.kerolabs.pozzo.features.savingsgroups.domain.NewGroup
import pe.kerolabs.pozzo.features.savingsgroups.domain.Periodicity
import pe.kerolabs.pozzo.features.savingsgroups.domain.Readiness
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroupRepository
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local.GroupDao
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local.GroupEntity
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote.CreateGroupRequestDto
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote.DestinationDto
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

    override suspend fun createGroup(group: NewGroup): Result<SavingsGroup> =
        apiCall {
            service.createGroup(
                CreateGroupRequestDto(
                    name = group.name,
                    contributionAmount = group.contributionAmount,
                    periodicity = group.periodicity.name,
                    seats = group.seats,
                    firstContributionDate = group.firstContributionDate.toString(),
                    destination = group.destination?.let { DestinationDto(it.method.name, it.phoneNumber) },
                ),
            )
        }.map { dto -> saveLocally(dto) }

    override suspend fun generateInvitation(groupId: String): Result<Invitation> =
        apiCall { service.generateInvitation(groupId) }
            .map { Invitation(it.code, it.link, Instant.parse(it.expiresAt)) }

    override suspend fun previewInvitation(code: String): Result<GroupPreview> =
        apiCall { service.previewInvitation(code) }.map { dto ->
            GroupPreview(
                groupId = dto.groupId,
                name = dto.name,
                status = GroupStatus.valueOf(dto.status),
                organizerName = dto.organizerName,
                contributionAmount = dto.rules.contributionAmount,
                periodicity = Periodicity.valueOf(dto.rules.periodicity),
                seats = dto.rules.seats,
                potAmount = dto.rules.potAmount,
                firstContributionDate = LocalDate.parse(dto.rules.firstContributionDate),
                cutoffDay = dto.rules.cutoffDay,
                membersCount = dto.membersCount,
                freeSeats = dto.freeSeats,
            )
        }

    override suspend fun joinGroup(code: String): Result<SavingsGroup> =
        apiCall { service.joinGroup(code) }.map { dto -> saveLocally(dto) }

    /** A group created or joined appears in the list right away, without waiting for a refresh. */
    private suspend fun saveLocally(dto: GroupDto): SavingsGroup {
        val entity = dto.toEntity()
        dao.upsertAll(listOf(entity))
        return entity.toDomain()
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
