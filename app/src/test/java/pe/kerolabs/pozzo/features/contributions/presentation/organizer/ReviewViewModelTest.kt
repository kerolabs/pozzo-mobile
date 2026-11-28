package pe.kerolabs.pozzo.features.contributions.presentation.organizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import pe.kerolabs.pozzo.features.contributions.application.GetPendingReviewsUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetReviewedContributionsUseCase
import pe.kerolabs.pozzo.features.contributions.application.ReopenReviewUseCase
import pe.kerolabs.pozzo.features.contributions.application.ReviewContributionUseCase
import pe.kerolabs.pozzo.features.contributions.domain.ContributionStatus
import pe.kerolabs.pozzo.features.contributions.domain.Inconsistency
import pe.kerolabs.pozzo.features.contributions.domain.ReviewDecision
import pe.kerolabs.pozzo.features.contributions.testing.FakeContributionRepository
import pe.kerolabs.pozzo.features.contributions.testing.GROUP_ID
import pe.kerolabs.pozzo.features.contributions.testing.MainDispatcherRule
import pe.kerolabs.pozzo.features.contributions.testing.contribution
import pe.kerolabs.pozzo.features.contributions.testing.ruleBroken

/** The organizer's review of a receipt that did not match: approve, reject and take the decision back. */
class ReviewViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val inconsistent = contribution(
        id = "c-1",
        status = ContributionStatus.INCONSISTENT,
        inconsistencies = listOf(Inconsistency("PAYEE", "Anna Weber", "Beltran Ramos")),
    )
    private val approved = contribution(id = "c-2", status = ContributionStatus.APPROVED)
    private val repository = FakeContributionRepository().apply {
        pendingReviews = Result.success(listOf(inconsistent))
        reviewed = Result.success(listOf(approved))
    }

    // The route of a screen is read from Android's navigation, which the JVM tests do not have, so the view
    // model is built with the group and the contribution the route would carry.
    private fun reviews(contributionId: String?) = object : ReviewsBaseViewModel(
        GROUP_ID,
        contributionId,
        GetPotStateUseCase(repository),
        GetPendingReviewsUseCase(repository),
        ReviewContributionUseCase(repository),
        GetReviewedContributionsUseCase(repository),
        ReopenReviewUseCase(repository),
    ) {}

    private fun detail() = reviews("c-1")

    private fun list() = reviews(null)

    @Test
    fun `lists what waits for review and what can still be reviewed again`() {
        val vm = list()

        vm.load()

        assertEquals(listOf(inconsistent), vm.state.value.reviews)
        assertEquals(listOf(approved), vm.state.value.reviewed)
    }

    @Test
    fun `approves with the note trimmed`() {
        val vm = detail()

        vm.decide(ReviewDecision.APPROVE, "  pagó el resto en efectivo  ")

        assertEquals(Triple("c-1", ReviewDecision.APPROVE, "pagó el resto en efectivo"), repository.reviews.single())
        assertEquals(ReviewDecision.APPROVE, vm.state.value.decided)
    }

    @Test
    fun `rejects without a note when the note is blank`() {
        val vm = detail()

        vm.decide(ReviewDecision.REJECT, "   ")

        assertEquals(Triple("c-1", ReviewDecision.REJECT, null), repository.reviews.single())
    }

    @Test
    fun `a decision the service refuses is not shown as taken`() {
        repository.reviewResult = Result.failure(ruleBroken("POT_ALREADY_DELIVERED", "El pozo de este período ya se entregó."))
        val vm = detail()

        vm.decide(ReviewDecision.APPROVE, null)

        assertNull(vm.state.value.decided)
        assertEquals("El pozo de este período ya se entregó.", vm.state.value.errorMessage)
    }

    @Test
    fun `reviewing again sends the contribution back to the list to review`() {
        val vm = list()
        vm.load()
        repository.pendingReviews = Result.success(listOf(inconsistent, approved.copy(status = ContributionStatus.INCONSISTENT)))
        repository.reviewed = Result.success(emptyList())

        vm.reopen("c-2")

        assertEquals(listOf("c-2"), repository.reopened)
        assertEquals(2, vm.state.value.reviews.size)
        assertEquals(emptyList<Any>(), vm.state.value.reviewed)
        assertNull(vm.state.value.reopening)
    }
}
