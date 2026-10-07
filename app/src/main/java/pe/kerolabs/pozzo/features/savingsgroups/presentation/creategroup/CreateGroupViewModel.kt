package pe.kerolabs.pozzo.features.savingsgroups.presentation.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.application.GetProfileUseCase
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.savingsgroups.application.CreateGroupUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.Destination
import pe.kerolabs.pozzo.features.savingsgroups.domain.Invitation
import pe.kerolabs.pozzo.features.savingsgroups.domain.NewGroup
import pe.kerolabs.pozzo.features.savingsgroups.domain.PaymentMethod
import pe.kerolabs.pozzo.features.savingsgroups.domain.Periodicity
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup

enum class CreateGroupStep(val number: Int, val label: String) {
    RULES(1, "Reglas"),
    DATES(2, "Fechas y destino"),
    SUMMARY(3, "Resumen"),
}

data class CreateGroupUiState(
    val step: CreateGroupStep = CreateGroupStep.RULES,
    val name: String = "",
    val amount: String = "",
    val periodicity: Periodicity = Periodicity.MONTHLY,
    val seats: Int = 8,
    val firstContributionDate: LocalDate = LocalDate.now().plusWeeks(1),
    val paymentMethod: PaymentMethod = PaymentMethod.YAPE,
    val destinationPhone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val created: Pair<SavingsGroup, Invitation>? = null,
) {
    /** The amount typed, or null while it is not a valid amount of soles. */
    val contributionAmount: BigDecimal?
        get() = amount.replace(',', '.').toBigDecimalOrNull()
            ?.takeIf { it >= BigDecimal.ONE && it.scale() <= 2 }

    val potAmount: BigDecimal? get() = contributionAmount?.multiply(BigDecimal(seats))

    val rulesValid: Boolean
        get() = name.isNotBlank() && contributionAmount != null && seats in NewGroup.MIN_SEATS..NewGroup.MAX_SEATS

    val datesValid: Boolean
        get() = !firstContributionDate.isBefore(LocalDate.now()) && PhoneNumbers.isValid(destinationPhone)

    fun toNewGroup() = NewGroup(
        name = name,
        contributionAmount = contributionAmount ?: BigDecimal.ZERO,
        periodicity = periodicity,
        seats = seats,
        firstContributionDate = firstContributionDate,
        destination = Destination(paymentMethod, destinationPhone),
    )
}

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroup: CreateGroupUseCase,
    private val getProfile: GetProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGroupUiState())
    val state: StateFlow<CreateGroupUiState> = _state.asStateFlow()

    init {
        // The Yape or Plin number of the profile is offered as the destination; the organizer can change it.
        viewModelScope.launch {
            getProfile().getOrNull()?.walletNumber?.let { number ->
                _state.update { if (it.destinationPhone.isEmpty()) it.copy(destinationPhone = number) else it }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(NewGroup.NAME_MAX_LENGTH)) }

    fun onAmountChange(value: String) =
        _state.update { it.copy(amount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10)) }

    fun onPeriodicityChange(value: Periodicity) = _state.update { it.copy(periodicity = value) }

    fun onSeatsChange(value: Int) =
        _state.update { it.copy(seats = value.coerceIn(NewGroup.MIN_SEATS, NewGroup.MAX_SEATS)) }

    fun onFirstContributionDateChange(value: LocalDate) = _state.update { it.copy(firstContributionDate = value) }

    fun onPaymentMethodChange(value: PaymentMethod) = _state.update { it.copy(paymentMethod = value) }

    fun onDestinationPhoneChange(value: String) =
        _state.update { it.copy(destinationPhone = value.filter(Char::isDigit).take(9)) }

    fun next() {
        _state.update {
            when (it.step) {
                CreateGroupStep.RULES -> if (it.rulesValid) it.copy(step = CreateGroupStep.DATES) else it
                CreateGroupStep.DATES -> if (it.datesValid) it.copy(step = CreateGroupStep.SUMMARY) else it
                CreateGroupStep.SUMMARY -> it
            }
        }
    }

    /** Goes to the previous step; returns false on the first one, where back closes the flow. */
    fun back(): Boolean {
        val step = _state.value.step
        if (step == CreateGroupStep.RULES) return false
        _state.update { it.copy(step = CreateGroupStep.entries[step.ordinal - 1], errorMessage = null) }
        return true
    }

    fun create() {
        val current = _state.value
        if (!current.rulesValid || !current.datesValid || current.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            createGroup(current.toNewGroup())
                .onSuccess { created -> _state.update { it.copy(isLoading = false, created = created) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onCreatedHandled() = _state.update { it.copy(created = null) }
}
