package pe.kerolabs.pozzo.features.contributions.presentation.register

import android.content.ContextWrapper
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.MockMakers
import org.mockito.Mockito.mock
import org.mockito.Mockito.withSettings
import pe.kerolabs.pozzo.features.contributions.application.AttachReceiptImageUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.application.ReadReceiptUseCase
import pe.kerolabs.pozzo.features.contributions.application.RegisterContributionUseCase
import pe.kerolabs.pozzo.features.contributions.domain.ReadByApp
import pe.kerolabs.pozzo.features.contributions.domain.ReadReceipt
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptReader
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptSource
import pe.kerolabs.pozzo.features.contributions.infrastructure.local.ReceiptImageReader
import pe.kerolabs.pozzo.features.contributions.testing.CUTOFF
import pe.kerolabs.pozzo.features.contributions.testing.FakeContributionRepository
import pe.kerolabs.pozzo.features.contributions.testing.GROUP_ID
import pe.kerolabs.pozzo.features.contributions.testing.MainDispatcherRule
import pe.kerolabs.pozzo.features.contributions.testing.awaitUntil
import pe.kerolabs.pozzo.features.contributions.testing.ruleBroken
import pe.kerolabs.pozzo.features.contributions.testing.soles

/** Registering a contribution with its receipt: what the app reads, what the member corrects and what fails. */
class RegisterContributionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val paidAt = LocalDate.of(2026, 11, 20)
    private val fullRead = ReadReceipt(soles("100"), paidAt, "Anna Weber", "04581273", ReceiptSource.YAPE)
    private val repository = FakeContributionRepository()
    private var read: Result<ReadReceipt> = Result.success(fullRead)
    // Uri is an Android class the default mock maker cannot change; a subclass stands in for the chosen image.
    private val image: Uri = mock(Uri::class.java, withSettings().mockMaker(MockMakers.SUBCLASS))

    private fun viewModel() = RegisterContributionViewModel(
        SavedStateHandle(mapOf("groupId" to GROUP_ID)),
        GetPotStateUseCase(repository),
        ReadReceiptUseCase(object : ReceiptReader {
            override suspend fun read(imageUri: Uri) = read
        }),
        RegisterContributionUseCase(repository),
        // Without a real phone the image cannot be compressed, as when the upload of the image fails.
        AttachReceiptImageUseCase(repository, ReceiptImageReader(ContextWrapper(null))),
    )

    @Test
    fun `sends the receipt as the app read it`() {
        val vm = viewModel()

        vm.onImageChosen(image)
        assertEquals(RegisterStep.REVIEW, vm.state.value.step)
        assertTrue(vm.state.value.showsReadData)
        assertTrue(vm.state.value.matchesExpected)

        vm.confirm()
        awaitUntil { !vm.state.value.isSaving }

        val sent = repository.registered.single()
        assertEquals(0, soles("100").compareTo(sent.amount))
        assertEquals("04581273", sent.operationNumber)
        assertEquals(ReadByApp("04581273", "Anna Weber", soles("100"), paidAt), sent.readByApp)
        assertEquals(RegisterStep.RESULT, vm.state.value.step)
    }

    @Test
    fun `a field corrected by hand travels with what was read, so the organizer reviews it`() {
        val vm = viewModel()
        vm.onImageChosen(image)

        vm.correct()
        vm.onAmountChange("150")
        vm.confirm()
        awaitUntil { !vm.state.value.isSaving }

        val sent = repository.registered.single()
        assertEquals(0, soles("150").compareTo(sent.amount))
        assertEquals(0, soles("100").compareTo(sent.readByApp!!.amount))
        assertFalse(vm.state.value.matchesExpected)
    }

    @Test
    fun `a receipt the app cannot read is completed by hand, without read data`() {
        read = Result.failure(IllegalStateException("blurry"))
        val vm = viewModel()

        vm.onImageChosen(image)

        assertEquals("No pudimos leer el comprobante. Completa los datos a mano.", vm.state.value.errorMessage)
        assertNull(vm.state.value.readByApp)
        assertEquals("Anna Weber", vm.state.value.form.payeeName)

        vm.onAmountChange("100")
        vm.onOperationChange("04581273")
        vm.confirm()
        awaitUntil { !vm.state.value.isSaving }

        assertNull(repository.registered.single().readByApp)
    }

    @Test
    fun `the contribution is kept when its image cannot be saved`() {
        val vm = viewModel()
        vm.onImageChosen(image)

        vm.confirm()
        awaitUntil { !vm.state.value.isSaving }

        assertNotNull(vm.state.value.result)
        assertTrue(vm.state.value.imageNotKept)
        assertEquals(RegisterStep.RESULT, vm.state.value.step)
    }

    @Test
    fun `a rejected receipt shows the reason and keeps the data to try again`() {
        repository.registerResult = Result.failure(ruleBroken("OPERATION_ALREADY_USED", "Ese comprobante ya fue usado en la junta."))
        val vm = viewModel()
        vm.onImageChosen(image)

        vm.confirm()
        awaitUntil { !vm.state.value.isSaving }

        assertEquals("Ese comprobante ya fue usado en la junta.", vm.state.value.errorMessage)
        assertNull(vm.state.value.result)
        assertEquals(RegisterStep.REVIEW, vm.state.value.step)
        assertEquals("04581273", vm.state.value.form.operationNumber)
    }

    @Test
    fun `an incomplete form is not sent`() {
        val vm = viewModel()
        vm.enterManually()

        vm.onAmountChange("100")
        vm.confirm()
        awaitUntil { !vm.state.value.isSaving }

        assertTrue(repository.registered.isEmpty())
    }

    @Test
    fun `a date after the cutoff does not match what is expected`() {
        val vm = viewModel()
        vm.onImageChosen(image)

        vm.onPaidAtChange(CUTOFF.plusDays(1))

        assertFalse(vm.state.value.matchesExpected)
    }

    @Test
    fun `the payee matches without accents, case or the part the wallet hides`() {
        assertTrue(namesMatch("ANNA WÉBER", "Anna Weber"))
        assertTrue(namesMatch("Anna W*", "Anna Weber"))
        assertFalse(namesMatch("Ana Weber", "Anna Weber"))
    }
}
