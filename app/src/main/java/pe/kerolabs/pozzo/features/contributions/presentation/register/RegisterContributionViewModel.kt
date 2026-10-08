package pe.kerolabs.pozzo.features.contributions.presentation.register

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.AttachReceiptImageUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.application.ReadReceiptUseCase
import pe.kerolabs.pozzo.features.contributions.application.RegisterContributionUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Contribution
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptData
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptSource
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.RegisterContributionRoute

enum class RegisterStep { INSTRUCTIONS, CAPTURE, REVIEW, RESULT }

/** The fields of the receipt as the member confirms them in F4. */
data class ReceiptForm(
    val amount: String = "",
    val paidAt: LocalDate = LocalDate.now(),
    val payeeName: String = "",
    val operationNumber: String = "",
    val source: ReceiptSource = ReceiptSource.YAPE,
    val readFromImage: Boolean = false,
) {
    val amountValue: BigDecimal? get() = amount.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val isComplete: Boolean
        get() = amountValue != null && payeeName.isNotBlank() && operationNumber.length >= 4
}

data class RegisterUiState(
    val cycle: Cycle? = null,
    val period: Period? = null,
    val step: RegisterStep = RegisterStep.INSTRUCTIONS,
    val form: ReceiptForm = ReceiptForm(),
    val isReading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val result: Contribution? = null,
    /** The receipt the member chose; its image is kept once the contribution is registered. */
    val imageUri: Uri? = null,
    /** The contribution was registered but its image could not be kept. */
    val imageNotKept: Boolean = false,
) {
    /** Whether the confirmed data match what is expected, shown before registering as a hint. */
    val matchesExpected: Boolean
        get() {
            val cycle = cycle ?: return false
            val period = period ?: return false
            val amount = form.amountValue ?: return false
            return amount.compareTo(period.contributionAmount) == 0 &&
                !form.paidAt.isAfter(period.cutoffDate) &&
                namesMatch(form.payeeName, cycle.payeeName)
        }
}

/**
 * Same rule as the backend: the receipt names the payee if it contains every word of the expected
 * name, ignoring accents and case; a word masked by the wallet ("Ann*") matches by its beginning.
 */
internal fun namesMatch(found: String, expected: String): Boolean {
    fun words(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase().replace(Regex("[^a-z0-9* ]"), " ")
        .trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val foundWords = words(found)
    return words(expected).all { expectedWord ->
        foundWords.any { it == expectedWord || (it.endsWith("*") && expectedWord.startsWith(it.removeSuffix("*"))) }
    }
}

@HiltViewModel
class RegisterContributionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPotState: GetPotStateUseCase,
    private val readReceipt: ReadReceiptUseCase,
    private val registerContribution: RegisterContributionUseCase,
    private val attachReceiptImage: AttachReceiptImageUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<RegisterContributionRoute>().groupId

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            getPotState(groupId)
                .onSuccess { (cycle, period) -> _state.update { it.copy(cycle = cycle, period = period, errorMessage = null) } }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.userMessage()) } }
        }
    }

    fun goTo(step: RegisterStep) = _state.update { it.copy(step = step, errorMessage = null) }

    /** Goes back one step; returns false on the first one, where back leaves the flow. */
    fun back(): Boolean {
        val step = _state.value.step
        if (step == RegisterStep.INSTRUCTIONS || step == RegisterStep.RESULT) return false
        goTo(RegisterStep.entries[step.ordinal - 1])
        return true
    }

    fun onImageChosen(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isReading = true, errorMessage = null, imageUri = uri) }
            val read = readReceipt(uri).getOrNull()
            val expected = _state.value.cycle
            _state.update {
                it.copy(
                    isReading = false,
                    step = RegisterStep.REVIEW,
                    form = ReceiptForm(
                        amount = read?.amount?.stripTrailingZeros()?.toPlainString().orEmpty(),
                        paidAt = read?.paidAt ?: LocalDate.now(),
                        payeeName = read?.payeeName ?: expected?.payeeName.orEmpty(),
                        operationNumber = read?.operationNumber.orEmpty(),
                        source = read?.source ?: ReceiptSource.YAPE,
                        readFromImage = read != null,
                    ),
                    errorMessage = if (read == null) "No pudimos leer el comprobante. Completa los datos a mano." else null,
                )
            }
        }
    }

    fun enterManually() = _state.update {
        it.copy(step = RegisterStep.REVIEW, form = ReceiptForm(payeeName = it.cycle?.payeeName.orEmpty()), imageUri = null)
    }

    fun onAmountChange(value: String) =
        _state.update { it.copy(form = it.form.copy(amount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10))) }

    fun onPaidAtChange(value: LocalDate) = _state.update { it.copy(form = it.form.copy(paidAt = value)) }

    fun onPayeeChange(value: String) = _state.update { it.copy(form = it.form.copy(payeeName = value.take(120))) }

    fun onOperationChange(value: String) =
        _state.update { it.copy(form = it.form.copy(operationNumber = value.filter(Char::isLetterOrDigit).take(40))) }

    fun onSourceChange(value: ReceiptSource) = _state.update { it.copy(form = it.form.copy(source = value)) }

    fun confirm() {
        val current = _state.value
        val period = current.period ?: return
        val amount = current.form.amountValue ?: return
        if (!current.form.isComplete || current.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            registerContribution(
                period.id,
                ReceiptData(
                    operationNumber = current.form.operationNumber,
                    payerName = null,
                    payeeName = current.form.payeeName,
                    amount = amount,
                    paidAt = current.form.paidAt,
                    source = current.form.source,
                ),
            )
                .onSuccess { contribution ->
                    // The image goes after the data: a contribution is never lost because its image failed.
                    val imageUri = current.imageUri
                    val kept = imageUri?.let { attachReceiptImage(contribution.id, it) }
                    _state.update {
                        it.copy(
                            isSaving = false,
                            result = kept?.getOrNull() ?: contribution,
                            imageNotKept = kept?.isFailure == true,
                            step = RegisterStep.RESULT,
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }
}
