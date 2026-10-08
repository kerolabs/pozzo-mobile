package pe.kerolabs.pozzo.features.contributions.infrastructure.repositories

import com.google.gson.Gson
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.contributions.domain.Contribution
import pe.kerolabs.pozzo.features.contributions.domain.ContributionMethod
import pe.kerolabs.pozzo.features.contributions.domain.ContributionRepository
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.domain.ContributionStatus
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Inconsistency
import pe.kerolabs.pozzo.features.contributions.domain.MemberContribution
import pe.kerolabs.pozzo.features.contributions.domain.MyContributions
import pe.kerolabs.pozzo.features.contributions.domain.MyPeriodContribution
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.PeriodState
import pe.kerolabs.pozzo.features.contributions.domain.PotDelivery
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptData
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptSource
import pe.kerolabs.pozzo.features.contributions.domain.ReviewDecision
import pe.kerolabs.pozzo.features.contributions.infrastructure.local.MyContributionsDao
import pe.kerolabs.pozzo.features.contributions.infrastructure.local.MyContributionsEntity
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.ContributionDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.ContributionsService
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.MyContributionsDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.PeriodStatusDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.ReceiptDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.RegisterCashRequestDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.RegisterCoverageRequestDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.ReviewRequestDto

class ContributionRepositoryImpl @Inject constructor(
    private val service: ContributionsService,
    private val myContributionsDao: MyContributionsDao,
) : ContributionRepository {

    private val gson = Gson()

    override suspend fun getCycleOfGroup(groupId: String): Result<Cycle> =
        apiCall { service.getCycleOfGroup(groupId) }.map { dto ->
            Cycle(
                id = dto.id,
                groupId = dto.groupId,
                groupName = dto.groupName,
                isActive = dto.status == "ACTIVE",
                currentTurn = dto.currentTurn,
                totalTurns = dto.totalTurns,
                contributionAmount = dto.contributionAmount,
                potAmount = dto.potAmount,
                destinationMethod = dto.destinationMethod,
                destinationPhone = dto.destinationPhoneNumber,
                payeeName = dto.payeeName,
                isOrganizer = dto.role == "ORGANIZER",
                myMembershipId = dto.myMembershipId,
                myTurnNumber = dto.myTurnNumber,
            )
        }

    override suspend fun getCurrentPeriod(cycleId: String): Result<Period> =
        apiCall { service.getCurrentPeriod(cycleId) }.map { it.toDomain() }

    override suspend fun getPeriods(cycleId: String): Result<List<Period>> =
        apiCall { service.getPeriods(cycleId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun getMyContributions(groupId: String): Result<MyContributions> {
        val remote = getCycleOfGroup(groupId).mapCatching { cycle ->
            apiCall { service.getMyContributions(cycle.id) }.getOrThrow()
        }
        remote.onSuccess { dto ->
            val now = Instant.now()
            myContributionsDao.upsert(MyContributionsEntity(groupId, gson.toJson(dto), now.toEpochMilli()))
            return Result.success(dto.toDomain(now, isOffline = false))
        }
        // Without connection the receipts are still there: the last copy kept on the phone.
        val cached = myContributionsDao.find(groupId) ?: return Result.failure(remote.exceptionOrNull()!!)
        return runCatching {
            gson.fromJson(cached.json, MyContributionsDto::class.java)
                .toDomain(Instant.ofEpochMilli(cached.savedAt), isOffline = true)
        }
    }

    override suspend fun clearLocalContributions() = myContributionsDao.deleteAll()

    override suspend fun registerContribution(periodId: String, receipt: ReceiptData): Result<Contribution> =
        apiCall {
            service.registerContribution(
                periodId,
                ReceiptDto(
                    operationNumber = receipt.operationNumber,
                    payerName = receipt.payerName,
                    payeeName = receipt.payeeName,
                    amount = receipt.amount,
                    paidAt = receipt.paidAt.toString(),
                    source = receipt.source.name,
                ),
            )
        }.map { it.toDomain() }

    override suspend fun attachReceiptImage(contributionId: String, jpeg: ByteArray): Result<Contribution> {
        val part = MultipartBody.Part.createFormData("image", "receipt.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        return apiCall { service.attachReceiptImage(contributionId, part) }.map { it.toDomain() }
    }

    override suspend fun getReceiptImageUrl(contributionId: String): Result<String> =
        apiCall { service.getReceiptImage(contributionId) }.map { it.url }

    override suspend fun registerCash(
        periodId: String,
        membershipId: String,
        amount: BigDecimal,
        receivedOn: LocalDate,
    ): Result<Contribution> =
        apiCall { service.registerCash(periodId, RegisterCashRequestDto(membershipId, amount, receivedOn.toString())) }
            .map { it.toDomain() }

    override suspend fun registerCoverage(periodId: String, membershipId: String, coveredByMembershipId: String): Result<Contribution> =
        apiCall { service.registerCoverage(periodId, RegisterCoverageRequestDto(membershipId, coveredByMembershipId)) }
            .map { it.toDomain() }

    override suspend fun getPendingReviews(periodId: String): Result<List<Contribution>> =
        apiCall { service.getPendingReviews(periodId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun review(contributionId: String, decision: ReviewDecision, note: String?): Result<Contribution> =
        apiCall { service.review(contributionId, ReviewRequestDto(decision.name, note)) }.map { it.toDomain() }

    override suspend fun deliverPot(periodId: String): Result<PotDelivery> =
        apiCall { service.deliverPot(periodId) }.map {
            PotDelivery(it.deliveredTurn, it.deliveredAmount, it.cycleStatus == "CLOSED", it.nextTurn)
        }

    private fun PeriodStatusDto.toDomain() = Period(
        id = periodId,
        cycleId = cycleId,
        groupName = groupName,
        turnNumber = turnNumber,
        totalTurns = totalTurns,
        state = PeriodState.valueOf(status),
        cutoffDate = LocalDate.parse(cutoffDate),
        daysToCutoff = daysToCutoff,
        payoutMembershipId = payoutMembershipId,
        payoutMemberName = payoutMemberName,
        contributionAmount = contributionAmount,
        potAmount = potAmount,
        collectedAmount = collectedAmount,
        missingAmount = missingAmount,
        settledCount = settledCount,
        membersCount = membersCount,
        myState = myStatus?.let(ContributionState::valueOf),
        members = members.map {
            MemberContribution(
                it.membershipId,
                it.displayName,
                it.me,
                it.collects,
                ContributionState.valueOf(it.status),
                it.contributionId,
            )
        },
    )

    private fun MyContributionsDto.toDomain(savedAt: Instant, isOffline: Boolean) = MyContributions(
        cycleId = cycleId,
        groupName = groupName,
        contributedAmount = contributedAmount,
        pendingAmount = pendingAmount,
        periods = periods.map {
            MyPeriodContribution(
                periodId = it.periodId,
                turnNumber = it.turnNumber,
                cutoffDate = LocalDate.parse(it.cutoffDate),
                amount = it.amount,
                state = ContributionState.valueOf(it.status),
                contribution = it.contribution?.toDomain(),
            )
        },
        savedAt = savedAt,
        isOffline = isOffline,
    )

    private fun ContributionDto.toDomain() = Contribution(
        id = id,
        periodId = periodId,
        membershipId = membershipId,
        memberName = memberName,
        amount = amount,
        method = ContributionMethod.valueOf(method),
        status = ContributionStatus.valueOf(status),
        receipt = receipt?.let {
            ReceiptData(
                operationNumber = it.operationNumber,
                payerName = it.payerName,
                payeeName = it.payeeName,
                amount = it.amount,
                paidAt = LocalDate.parse(it.paidAt),
                source = ReceiptSource.valueOf(it.source),
            )
        },
        inconsistencies = inconsistencies.orEmpty().map { Inconsistency(it.field, it.expected, it.found) },
        registeredAt = Instant.parse(registeredAt),
        hasReceiptImage = hasReceiptImage,
    )
}
