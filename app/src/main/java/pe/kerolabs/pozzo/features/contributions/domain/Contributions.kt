package pe.kerolabs.pozzo.features.contributions.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * The cycle of a started group: a frozen copy of its rules and the turn in progress.
 */
data class Cycle(
    val id: String,
    val groupId: String,
    val groupName: String,
    val isActive: Boolean,
    val currentTurn: Int,
    val totalTurns: Int,
    val contributionAmount: BigDecimal,
    val potAmount: BigDecimal,
    val destinationMethod: String,
    val destinationPhone: String,
    val payeeName: String,
    val isOrganizer: Boolean,
    val myMembershipId: String,
    val myTurnNumber: Int,
)

/**
 * State of a member's contribution in a period. Late is derived from the cutoff date.
 */
enum class ContributionState { VALIDATED, IN_REVIEW, PENDING, LATE, COVERED }

val ContributionState.isSettled: Boolean
    get() = this == ContributionState.VALIDATED || this == ContributionState.COVERED

enum class PeriodState { OPEN, POT_COMPLETE, DELIVERED }

data class MemberContribution(
    val membershipId: String,
    val displayName: String,
    val isMe: Boolean,
    val collects: Boolean,
    val state: ContributionState,
    val contributionId: String?,
)

/**
 * A turn of the cycle: who collects, how much was gathered and the state of every member.
 */
data class Period(
    val id: String,
    val cycleId: String,
    val groupName: String,
    val turnNumber: Int,
    val totalTurns: Int,
    val state: PeriodState,
    val cutoffDate: LocalDate,
    val daysToCutoff: Long,
    val payoutMembershipId: String,
    val payoutMemberName: String,
    val contributionAmount: BigDecimal,
    val potAmount: BigDecimal,
    val collectedAmount: BigDecimal,
    val missingAmount: BigDecimal,
    val settledCount: Int,
    val membersCount: Int,
    val myState: ContributionState?,
    val members: List<MemberContribution>,
) {
    val progress: Float
        get() = if (potAmount.signum() == 0) 0f else collectedAmount.toFloat() / potAmount.toFloat()
    val inReviewCount: Int get() = members.count { it.state == ContributionState.IN_REVIEW }
    val unsettled: List<MemberContribution> get() = members.filter { !it.state.isSettled && it.state != ContributionState.IN_REVIEW }
}

enum class ReceiptSource { YAPE, PLIN, BANK }

/**
 * The data of a payment receipt, as read on the phone and confirmed by the member.
 */
data class ReceiptData(
    val operationNumber: String,
    val payerName: String?,
    val payeeName: String,
    val amount: BigDecimal,
    val paidAt: LocalDate,
    val source: ReceiptSource,
)

enum class ContributionMethod { TRANSFER, CASH, COVERAGE }

enum class ContributionStatus { VALIDATED, INCONSISTENT, APPROVED, REJECTED }

/** A field of the receipt that did not match: AMOUNT, PAYEE or DATE. */
data class Inconsistency(val field: String, val expected: String, val found: String)

data class Contribution(
    val id: String,
    val periodId: String,
    val membershipId: String,
    val memberName: String,
    val amount: BigDecimal,
    val method: ContributionMethod,
    val status: ContributionStatus,
    val receipt: ReceiptData?,
    val inconsistencies: List<Inconsistency>,
    val registeredAt: Instant,
    /** True when the image of the receipt was kept; its link is asked for when it is opened. */
    val hasReceiptImage: Boolean = false,
)

/**
 * What the delivery of a pot left: the next period, or the end of the cycle.
 */
data class PotDelivery(val deliveredTurn: Int, val deliveredAmount: BigDecimal, val cycleClosed: Boolean, val nextTurn: Int?)

enum class ReviewDecision { APPROVE, REJECT }

interface ContributionRepository {

    /** The cycle of a started group, or a failure with status 404 while it has not started. */
    suspend fun getCycleOfGroup(groupId: String): Result<Cycle>

    suspend fun getCurrentPeriod(cycleId: String): Result<Period>

    /** Every period opened so far, in turn order. */
    suspend fun getPeriods(cycleId: String): Result<List<Period>>

    /** The member's contributions in the cycle of a group, or the last copy kept on the phone. */
    suspend fun getMyContributions(groupId: String): Result<MyContributions>

    /** Removes the copies kept on the phone, e.g. when the member signs out. */
    suspend fun clearLocalContributions()

    suspend fun registerContribution(periodId: String, receipt: ReceiptData): Result<Contribution>

    suspend fun registerCash(periodId: String, membershipId: String, amount: BigDecimal, receivedOn: LocalDate): Result<Contribution>

    suspend fun registerCoverage(periodId: String, membershipId: String, coveredByMembershipId: String): Result<Contribution>

    /** Keeps the image of the receipt of a contribution the member registered, as a JPEG. */
    suspend fun attachReceiptImage(contributionId: String, jpeg: ByteArray): Result<Contribution>

    /** A link to the image of a receipt that works for a few minutes. */
    suspend fun getReceiptImageUrl(contributionId: String): Result<String>

    suspend fun getPendingReviews(periodId: String): Result<List<Contribution>>

    suspend fun review(contributionId: String, decision: ReviewDecision, note: String?): Result<Contribution>

    suspend fun deliverPot(periodId: String): Result<PotDelivery>
}

/**
 * Reads the text of a receipt image on the phone; the image itself is kept apart, only once the
 * contribution is registered.
 */
interface ReceiptReader {
    suspend fun read(imageUri: android.net.Uri): Result<ReadReceipt>
}

/**
 * What could be read from a receipt; any field may be missing and the member completes it.
 */
data class ReadReceipt(
    val amount: BigDecimal?,
    val paidAt: LocalDate?,
    val payeeName: String?,
    val operationNumber: String?,
    val source: ReceiptSource?,
)
