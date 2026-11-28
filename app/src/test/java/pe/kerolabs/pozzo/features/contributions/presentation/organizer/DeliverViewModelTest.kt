package pe.kerolabs.pozzo.features.contributions.presentation.organizer

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.kerolabs.pozzo.features.contributions.application.DeliverPotUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetCycleHistoryUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.domain.PeriodState
import pe.kerolabs.pozzo.features.contributions.domain.PotDelivery
import pe.kerolabs.pozzo.features.contributions.testing.FakeContributionRepository
import pe.kerolabs.pozzo.features.contributions.testing.GROUP_ID
import pe.kerolabs.pozzo.features.contributions.testing.MainDispatcherRule
import pe.kerolabs.pozzo.features.contributions.testing.PERIOD_ID
import pe.kerolabs.pozzo.features.contributions.testing.cycle
import pe.kerolabs.pozzo.features.contributions.testing.member
import pe.kerolabs.pozzo.features.contributions.testing.noConnection
import pe.kerolabs.pozzo.features.contributions.testing.period
import pe.kerolabs.pozzo.features.contributions.testing.soles

/**
 * Delivering the pot, and what the organizer sees when the answer is lost: the app checks whether the
 * delivery was recorded before saying it failed.
 */
class DeliverViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val complete = period(
        state = PeriodState.POT_COMPLETE,
        members = listOf(
            member("m-anna", "Anna Weber", ContributionState.VALIDATED, isMe = true, collects = true),
            member("m-sofia", "Sofia Gonzales", ContributionState.VALIDATED),
            member("m-luis", "Luis Rojas", ContributionState.COVERED),
        ),
        payoutAmount = soles("280"),
    )
    private val repository = FakeContributionRepository(currentPeriod = Result.success(complete))

    private fun viewModel() = DeliverViewModel(
        SavedStateHandle(mapOf("groupId" to GROUP_ID)),
        GetPotStateUseCase(repository),
        DeliverPotUseCase(repository),
        GetCycleHistoryUseCase(repository),
    )

    @Test
    fun `delivers the pot and opens the next turn`() {
        val delivery = PotDelivery(PERIOD_ID, 1, soles("280"), cycleClosed = false, nextTurn = 2)
        repository.deliverResults += Result.success(delivery)
        val vm = viewModel()

        vm.deliver()

        assertEquals(delivery, vm.state.value.delivery)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `a lost answer after a recorded delivery shows the delivery, not an error`() {
        repository.deliverResults += Result.failure(noConnection())
        val vm = viewModel()
        repository.cycle = Result.success(cycle(currentTurn = 2))
        repository.periods = Result.success(listOf(complete.copy(state = PeriodState.DELIVERED)))

        vm.deliver()

        val shown = vm.state.value.delivery!!
        assertEquals(PERIOD_ID, shown.deliveredPeriodId)
        assertEquals(0, soles("280").compareTo(shown.deliveredAmount))
        assertEquals(2, shown.nextTurn)
        assertFalse(shown.cycleClosed)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `a recorded delivery of the last turn shows the cycle closed`() {
        repository.deliverResults += Result.failure(noConnection())
        val vm = viewModel()
        repository.cycle = Result.success(cycle(isActive = false, currentTurn = 3))
        repository.periods = Result.success(listOf(complete.copy(state = PeriodState.DELIVERED)))

        vm.deliver()

        assertTrue(vm.state.value.delivery!!.cycleClosed)
        assertNull(vm.state.value.delivery!!.nextTurn)
    }

    @Test
    fun `a delivery that was not recorded says the pot is still pending, and can be tried again`() {
        repository.deliverResults += Result.failure(noConnection())
        repository.deliverResults += Result.success(PotDelivery(PERIOD_ID, 1, soles("280"), false, 2))
        val vm = viewModel()

        vm.deliver()

        assertNull(vm.state.value.delivery)
        val message = vm.state.value.errorMessage!!
        assertTrue(message.startsWith("No se registró la entrega"))
        assertTrue(message.endsWith("El pozo sigue pendiente de entrega; puedes intentarlo de nuevo."))
        assertFalse(vm.state.value.isSaving)

        vm.deliver()

        assertEquals(2, repository.deliverCalls)
        assertEquals(2, vm.state.value.delivery!!.nextTurn)
        assertNull(vm.state.value.errorMessage)
    }
}
