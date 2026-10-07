package pe.kerolabs.pozzo.features.contributions.infrastructure.remote

import java.math.BigDecimal

data class CycleDto(
    val id: String,
    val groupId: String,
    val groupName: String,
    val status: String,
    val currentTurn: Int,
    val totalTurns: Int,
    val contributionAmount: BigDecimal,
    val currency: String,
    val periodicity: String,
    val potAmount: BigDecimal,
    val destinationMethod: String,
    val destinationPhoneNumber: String,
    val payeeName: String,
    val role: String,
    val myMembershipId: String,
    val myTurnNumber: Int,
)

data class PeriodStatusDto(
    val periodId: String,
    val cycleId: String,
    val groupName: String,
    val turnNumber: Int,
    val totalTurns: Int,
    val status: String,
    val cutoffDate: String,
    val daysToCutoff: Long,
    val payoutMembershipId: String,
    val payoutMemberName: String,
    val contributionAmount: BigDecimal,
    val potAmount: BigDecimal,
    val collectedAmount: BigDecimal,
    val missingAmount: BigDecimal,
    val settledCount: Int,
    val membersCount: Int,
    val myStatus: String?,
    val members: List<MemberStatusDto>,
)

data class MemberStatusDto(
    val membershipId: String,
    val displayName: String,
    val me: Boolean,
    val collects: Boolean,
    val status: String,
    val contributionId: String?,
)

data class ReceiptDto(
    val operationNumber: String,
    val payerName: String?,
    val payeeName: String,
    val amount: BigDecimal,
    val paidAt: String,
    val source: String,
)

data class InconsistencyDto(val field: String, val expected: String, val found: String)

data class ContributionDto(
    val id: String,
    val periodId: String,
    val membershipId: String,
    val memberName: String,
    val amount: BigDecimal,
    val method: String,
    val status: String,
    val receipt: ReceiptDto?,
    val inconsistencies: List<InconsistencyDto>?,
    val registeredAt: String,
)

data class RegisterCashRequestDto(val membershipId: String, val amount: BigDecimal, val receivedOn: String)

data class RegisterCoverageRequestDto(val membershipId: String, val coveredByMembershipId: String)

data class ReviewRequestDto(val decision: String, val note: String?)

data class PotDeliveryDto(
    val deliveredPeriodId: String,
    val deliveredTurn: Int,
    val deliveredAmount: BigDecimal,
    val cycleStatus: String,
    val nextPeriodId: String?,
    val nextTurn: Int?,
)
