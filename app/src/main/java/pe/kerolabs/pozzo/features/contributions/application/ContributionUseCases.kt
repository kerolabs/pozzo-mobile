package pe.kerolabs.pozzo.features.contributions.application

import android.net.Uri
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import pe.kerolabs.pozzo.features.contributions.domain.ContributionRepository
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptData
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptReader
import pe.kerolabs.pozzo.features.contributions.domain.ReviewDecision

/**
 * The cycle of a group and its period in progress, which together are the state of the pot.
 */
class GetPotStateUseCase @Inject constructor(private val repository: ContributionRepository) {
    suspend operator fun invoke(groupId: String): Result<Pair<Cycle, Period>> =
        repository.getCycleOfGroup(groupId).mapCatching { cycle ->
            cycle to repository.getCurrentPeriod(cycle.id).getOrThrow()
        }
}

class ReadReceiptUseCase @Inject constructor(private val reader: ReceiptReader) {
    suspend operator fun invoke(imageUri: Uri) = reader.read(imageUri)
}

class RegisterContributionUseCase @Inject constructor(private val repository: ContributionRepository) {
    suspend operator fun invoke(periodId: String, receipt: ReceiptData) =
        repository.registerContribution(
            periodId,
            receipt.copy(operationNumber = receipt.operationNumber.trim(), payeeName = receipt.payeeName.trim()),
        )
}

class RegisterCashUseCase @Inject constructor(private val repository: ContributionRepository) {
    suspend operator fun invoke(periodId: String, membershipId: String, amount: BigDecimal, receivedOn: LocalDate) =
        repository.registerCash(periodId, membershipId, amount, receivedOn)
}

class RegisterCoverageUseCase @Inject constructor(private val repository: ContributionRepository) {
    suspend operator fun invoke(periodId: String, membershipId: String, coveredByMembershipId: String) =
        repository.registerCoverage(periodId, membershipId, coveredByMembershipId)
}

class GetPendingReviewsUseCase @Inject constructor(private val repository: ContributionRepository) {
    suspend operator fun invoke(periodId: String) = repository.getPendingReviews(periodId)
}

class ReviewContributionUseCase @Inject constructor(private val repository: ContributionRepository) {
    suspend operator fun invoke(contributionId: String, decision: ReviewDecision, note: String?) =
        repository.review(contributionId, decision, note?.takeIf { it.isNotBlank() })
}

class DeliverPotUseCase @Inject constructor(private val repository: ContributionRepository) {
    suspend operator fun invoke(periodId: String) = repository.deliverPot(periodId)
}
