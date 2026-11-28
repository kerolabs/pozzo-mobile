package pe.kerolabs.pozzo.features.contributions.presentation.organizer

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.application.RegisterCashUseCase
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.testing.FakeContributionRepository
import pe.kerolabs.pozzo.features.contributions.testing.GROUP_ID
import pe.kerolabs.pozzo.features.contributions.testing.MainDispatcherRule
import pe.kerolabs.pozzo.features.contributions.testing.member
import pe.kerolabs.pozzo.features.contributions.testing.period
import pe.kerolabs.pozzo.features.contributions.testing.ruleBroken
import pe.kerolabs.pozzo.features.contributions.testing.soles

/** US27: the organizer registers cash, and an amount other than what is owed only once confirmed. */
class CashViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    // Sofia owes S/ 80 after the credit of an auction, and she is the only one still to pay.
    private val repository = FakeContributionRepository(
        currentPeriod = Result.success(
            period(
                members = listOf(
                    member("m-anna", "Anna Weber", ContributionState.VALIDATED, isMe = true, collects = true),
                    member("m-sofia", "Sofia Gonzales", amount = soles("80")),
                    member("m-luis", "Luis Rojas", ContributionState.VALIDATED),
                ),
            ),
        ),
    )

    private fun viewModel() = CashViewModel(
        SavedStateHandle(mapOf("groupId" to GROUP_ID)),
        GetPotStateUseCase(repository),
        RegisterCashUseCase(repository),
    )

    @Test
    fun `proposes the only member still to pay and what they owe after the auction`() {
        val vm = viewModel()

        assertEquals("m-sofia", vm.state.value.selectedMember)
        assertEquals("80", vm.state.value.amount)
    }

    @Test
    fun `the amount owed is registered without asking for a confirmation`() {
        val vm = viewModel()

        vm.register()

        val call = repository.cashCalls.single()
        assertEquals(0, soles("80").compareTo(call.amount))
        assertFalse(call.differenceConfirmed)
        assertTrue(vm.state.value.done != null)
    }

    @Test
    fun `a different amount travels with the organizer's confirmation`() {
        val vm = viewModel()

        vm.onAmountChange("60")
        vm.register(differenceConfirmed = true)

        val call = repository.cashCalls.single()
        assertEquals(0, soles("60").compareTo(call.amount))
        assertTrue(call.differenceConfirmed)
    }

    @Test
    fun `a different amount the service refuses shows the reason`() {
        repository.cashResult = Result.failure(
            ruleBroken("AMOUNT_MISMATCH", "El monto en efectivo es distinto del aporte; confirma la diferencia para registrarlo."),
        )
        val vm = viewModel()

        vm.onAmountChange("60")
        vm.register()

        assertNull(vm.state.value.done)
        assertEquals("El monto en efectivo es distinto del aporte; confirma la diferencia para registrarlo.", vm.state.value.errorMessage)
    }
}
