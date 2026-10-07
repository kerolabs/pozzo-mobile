package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote

import java.math.BigDecimal

data class GroupDto(
    val id: String,
    val name: String,
    val status: String,
    val role: String,
    val organizerName: String,
    val rules: RulesDto,
    val turnMethod: String?,
    val myTurnNumber: Int?,
    val myTurnDate: String?,
    val readiness: ReadinessDto,
    val createdAt: String,
    val startedAt: String?,
)

data class RulesDto(
    val contributionAmount: BigDecimal,
    val currency: String,
    val periodicity: String,
    val seats: Int,
    val firstContributionDate: String,
    val cutoffDay: Int,
    val potAmount: BigDecimal,
    val destination: DestinationDto?,
)

data class DestinationDto(val method: String, val phoneNumber: String)

data class CreateGroupRequestDto(
    val name: String,
    val contributionAmount: BigDecimal,
    val periodicity: String,
    val seats: Int,
    val firstContributionDate: String,
    val destination: DestinationDto?,
)

data class InvitationDto(val code: String, val link: String, val expiresAt: String)

data class GroupPreviewDto(
    val groupId: String,
    val name: String,
    val status: String,
    val organizerName: String,
    val rules: RulesDto,
    val membersCount: Int,
    val freeSeats: Int,
    val invitationExpiresAt: String,
)

data class ReadinessDto(
    val groupFull: Boolean,
    val turnsAssigned: Boolean,
    val destinationDefined: Boolean,
    val canStart: Boolean,
)
