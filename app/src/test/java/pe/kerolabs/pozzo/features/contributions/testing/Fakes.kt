package pe.kerolabs.pozzo.features.contributions.testing

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pe.kerolabs.pozzo.core.network.ApiException
import pe.kerolabs.pozzo.features.contributions.domain.Auction
import pe.kerolabs.pozzo.features.contributions.domain.AuctionBid
import pe.kerolabs.pozzo.features.contributions.domain.AuctionFallback
import pe.kerolabs.pozzo.features.contributions.domain.AuctionRepository
import pe.kerolabs.pozzo.features.contributions.domain.AuctionResolution
import pe.kerolabs.pozzo.features.contributions.domain.AuctionStatus
import pe.kerolabs.pozzo.features.contributions.domain.Contribution
import pe.kerolabs.pozzo.features.contributions.domain.ContributionMethod
import pe.kerolabs.pozzo.features.contributions.domain.ContributionRepository
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.domain.ContributionStatus
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Inconsistency
import pe.kerolabs.pozzo.features.contributions.domain.MemberContribution
import pe.kerolabs.pozzo.features.contributions.domain.MyContributions
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.PeriodState
import pe.kerolabs.pozzo.features.contributions.domain.PotDelivery
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptData
import pe.kerolabs.pozzo.features.contributions.domain.ReviewDecision

/** Runs the view models' coroutines at once, on the test thread, instead of on Android's main thread. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Waits for work the view model moved off the test thread, such as compressing an image on the IO
 * dispatcher, before the test checks the result.
 */
fun awaitUntil(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMillis
    while (!condition()) {
        check(System.currentTimeMillis() < end) { "The condition did not hold within $timeoutMillis ms" }
        Thread.sleep(10)
    }
}

const val GROUP_ID = "group-1"
const val CYCLE_ID = "cycle-1"
const val PERIOD_ID = "period-1"
val NOW: Instant = Instant.parse("2026-11-20T15:00:00Z")
val CUTOFF: LocalDate = LocalDate.of(2026, 11, 30)

fun soles(value: String) = BigDecimal(value)

/** The failure the backend sends for a business rule, as the app reads it. */
fun ruleBroken(code: String, message: String) = ApiException(422, code, message)

/** The failure the app reports when it cannot reach the backend. */
fun noConnection() = ApiException(0, "NETWORK_ERROR", "No pudimos conectarnos. Revisa tu conexión e inténtalo de nuevo.")

fun cycle(isActive: Boolean = true, currentTurn: Int = 1, isOrganizer: Boolean = true) = Cycle(
    id = CYCLE_ID,
    groupId = GROUP_ID,
    groupName = "Junta Los Olivos",
    isActive = isActive,
    currentTurn = currentTurn,
    totalTurns = 3,
    contributionAmount = soles("100"),
    potAmount = soles("300"),
    destinationMethod = "YAPE",
    destinationPhone = "999000123",
    payeeName = "Anna Weber",
    isOrganizer = isOrganizer,
    myMembershipId = "m-anna",
    myTurnNumber = 1,
)

fun member(
    id: String,
    name: String,
    state: ContributionState = ContributionState.PENDING,
    amount: BigDecimal? = null,
    isMe: Boolean = false,
    collects: Boolean = false,
    contributionId: String? = null,
) = MemberContribution(id, name, isMe, collects, state, contributionId, amount)

fun period(
    id: String = PERIOD_ID,
    state: PeriodState = PeriodState.OPEN,
    members: List<MemberContribution> = listOf(
        member("m-anna", "Anna Weber", isMe = true, collects = true),
        member("m-sofia", "Sofia Gonzales"),
        member("m-luis", "Luis Rojas"),
    ),
    payoutAmount: BigDecimal = soles("300"),
) = Period(
    id = id,
    cycleId = CYCLE_ID,
    groupName = "Junta Los Olivos",
    turnNumber = 1,
    totalTurns = 3,
    state = state,
    cutoffDate = CUTOFF,
    daysToCutoff = 10,
    payoutMembershipId = "m-anna",
    payoutMemberName = "Anna Weber",
    contributionAmount = soles("100"),
    potAmount = soles("300"),
    collectedAmount = BigDecimal.ZERO,
    missingAmount = soles("300"),
    settledCount = 0,
    membersCount = members.size,
    myState = members.firstOrNull { it.isMe }?.state,
    members = members,
    payoutAmount = payoutAmount,
)

fun contribution(
    id: String = "c-1",
    membershipId: String = "m-sofia",
    amount: BigDecimal = soles("100"),
    method: ContributionMethod = ContributionMethod.TRANSFER,
    status: ContributionStatus = ContributionStatus.VALIDATED,
    receipt: ReceiptData? = null,
    inconsistencies: List<Inconsistency> = emptyList(),
) = Contribution(id, PERIOD_ID, membershipId, "Sofia Gonzales", amount, method, status, receipt, inconsistencies, NOW)

fun auction(
    myBid: BigDecimal? = null,
    myBidIsHighest: Boolean = false,
    bids: List<AuctionBid> = emptyList(),
    status: AuctionStatus = AuctionStatus.OPEN,
    winner: String? = null,
) = Auction(
    periodId = PERIOD_ID,
    turnNumber = 1,
    status = status,
    closesOn = CUTOFF.minusDays(3),
    minimumBid = soles("10"),
    potAmount = soles("300"),
    fallback = AuctionFallback.DRAW,
    canBid = true,
    myBid = myBid,
    myBidIsHighest = myBidIsHighest,
    bids = bids,
    winnerMembershipId = winner,
    winnerName = winner?.let { "Sofia Gonzales" },
    discount = if (winner != null) bids.firstOrNull()?.amount else null,
    payoutAmount = if (winner != null) soles("300") - (bids.firstOrNull()?.amount ?: BigDecimal.ZERO) else null,
    resolution = if (winner != null) AuctionResolution.BID else null,
    closedAt = if (winner != null) NOW else null,
)

/**
 * A repository in memory: each answer can be set by the test, and every call that changes something is
 * kept so the test can check what the view model sent.
 */
class FakeContributionRepository(
    var cycle: Result<Cycle> = Result.success(cycle()),
    var currentPeriod: Result<Period> = Result.success(period()),
    var periods: Result<List<Period>> = Result.success(listOf(period())),
) : ContributionRepository {

    var registerResult: Result<Contribution> = Result.success(contribution())
    val registered = mutableListOf<ReceiptData>()

    var cashResult: Result<Contribution> = Result.success(contribution(method = ContributionMethod.CASH))
    data class CashCall(val membershipId: String, val amount: BigDecimal, val receivedOn: LocalDate, val differenceConfirmed: Boolean)
    val cashCalls = mutableListOf<CashCall>()

    var pendingReviews: Result<List<Contribution>> = Result.success(emptyList())
    var reviewed: Result<List<Contribution>> = Result.success(emptyList())
    var reviewResult: Result<Contribution> = Result.success(contribution(status = ContributionStatus.APPROVED))
    val reviews = mutableListOf<Triple<String, ReviewDecision, String?>>()
    var reopenResult: Result<Contribution> = Result.success(contribution(status = ContributionStatus.INCONSISTENT))
    val reopened = mutableListOf<String>()

    var deliverResults = ArrayDeque<Result<PotDelivery>>()
    var deliverCalls = 0

    override suspend fun getCycleOfGroup(groupId: String) = cycle
    override suspend fun getCurrentPeriod(cycleId: String) = currentPeriod
    override suspend fun getPeriods(cycleId: String) = periods
    override suspend fun getMyContributions(groupId: String): Result<MyContributions> = error("not used")
    override suspend fun clearLocalContributions() = Unit

    override suspend fun registerContribution(periodId: String, receipt: ReceiptData): Result<Contribution> {
        registered += receipt
        return registerResult
    }

    override suspend fun registerCash(
        periodId: String,
        membershipId: String,
        amount: BigDecimal,
        receivedOn: LocalDate,
        differenceConfirmed: Boolean,
    ): Result<Contribution> {
        cashCalls += CashCall(membershipId, amount, receivedOn, differenceConfirmed)
        return cashResult
    }

    override suspend fun registerCoverage(periodId: String, membershipId: String, coveredByMembershipId: String) =
        Result.success(contribution(method = ContributionMethod.COVERAGE))

    override suspend fun attachReceiptImage(contributionId: String, jpeg: ByteArray) = Result.success(contribution(id = contributionId))
    override suspend fun getReceiptImageUrl(contributionId: String) = Result.success("https://example.test/receipt.jpg")
    override suspend fun getPendingReviews(periodId: String) = pendingReviews

    override suspend fun review(contributionId: String, decision: ReviewDecision, note: String?): Result<Contribution> {
        reviews += Triple(contributionId, decision, note)
        return reviewResult
    }

    override suspend fun getReviewed(periodId: String) = reviewed

    override suspend fun reopenReview(contributionId: String): Result<Contribution> {
        reopened += contributionId
        return reopenResult
    }

    override suspend fun deliverPot(periodId: String): Result<PotDelivery> {
        deliverCalls++
        return deliverResults.removeFirst()
    }

    override suspend fun swapTurns(cycleId: String, firstMembershipId: String, secondMembershipId: String) = cycle
    override suspend fun attachPayoutProof(periodId: String, jpeg: ByteArray) = Result.success(Unit)
    override suspend fun getPayoutProofUrl(periodId: String) = Result.success("https://example.test/payout.jpg")
    override suspend fun shareContribution(contributionId: String) = Result.success("https://kerolabs.github.io/aporte/?t=abc")
}

class FakeAuctionRepository(var current: Result<Auction> = Result.success(auction())) : AuctionRepository {

    var bidResult: Result<Auction> = Result.success(auction())
    val bids = mutableListOf<BigDecimal>()
    var closeResult: Result<Auction> = Result.success(auction(status = AuctionStatus.CLOSED, winner = "m-sofia"))
    val closes = mutableListOf<String?>()

    override suspend fun getAuction(periodId: String) = current

    override suspend fun placeBid(periodId: String, amount: BigDecimal): Result<Auction> {
        bids += amount
        return bidResult
    }

    override suspend fun closeAuction(periodId: String, winnerMembershipId: String?): Result<Auction> {
        closes += winnerMembershipId
        return closeResult
    }
}
