package pe.kerolabs.pozzo.features.savingsgroups.presentation.creategroup

import androidx.lifecycle.SavedStateHandle
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
import pe.kerolabs.pozzo.features.savingsgroups.application.GetGroupDetailUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.UpdateGroupUseCase
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
    /** The group being edited, or null while creating a new one. */
    val editingGroupId: String? = null,
    /** True while the group to edit is read. */
    val isPreparing: Boolean = false,
    /** The fewest seats allowed: the members the group already has. */
    val minSeats: Int = NewGroup.MIN_SEATS,
    val originalSeats: Int? = null,
    val turnsAssigned: Boolean = false,
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = editingGroupId != null

    /** Changing the number of members discards the turns, because they no longer cover the whole group. */
    val clearsTurns: Boolean get() = turnsAssigned && originalSeats != null && seats != originalSeats

    /** The amount typed, or null while it is not a valid amount of soles. */
    val contributionAmount: BigDecimal?
        get() = amount.replace(',', '.').toBigDecimalOrNull()
            ?.takeIf { it >= BigDecimal.ONE && it.scale() <= 2 }

    val potAmount: BigDecimal? get() = contributionAmount?.multiply(BigDecimal(seats))

    val rulesValid: Boolean
        get() = name.isNotBlank() && contributionAmount != null && seats in minSeats..NewGroup.MAX_SEATS

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

/**
 * C1 to C3, also used to edit a group before it starts: opened with a "groupId" the steps start from the
 * group as it is and the last one saves the changes.
 */
@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createGroup: CreateGroupUseCase,
    private val updateGroup: UpdateGroupUseCase,
    private val getGroupDetail: GetGroupDetailUseCase,
    private val getProfile: GetProfileUseCase,
) : ViewModel() {

    private val editingGroupId: String? = savedStateHandle["groupId"]

    private val _state = MutableStateFlow(CreateGroupUiState(editingGroupId = editingGroupId, isPreparing = editingGroupId != null))
    val state: StateFlow<CreateGroupUiState> = _state.asStateFlow()

    init {
        if (editingGroupId != null) loadGroup(editingGroupId) else offerWalletNumber()
    }

    // The Yape or Plin number of the profile is offered as the destination; the organizer can change it.
    private fun offerWalletNumber() {
        viewModelScope.launch {
            getProfile().getOrNull()?.walletNumber?.let { number ->
                _state.update { if (it.destinationPhone.isEmpty()) it.copy(destinationPhone = number) else it }
            }
        }
    }

    private fun loadGroup(groupId: String) {
        viewModelScope.launch {
            getGroupDetail(groupId)
                .onSuccess { detail ->
                    val group = detail.group
                    _state.update {
                        it.copy(
                            isPreparing = false,
                            name = group.name,
                            amount = group.contributionAmount.stripTrailingZeros().toPlainString(),
                            periodicity = group.periodicity,
                            seats = group.seats,
                            firstContributionDate = group.firstContributionDate,
                            paymentMethod = group.destination?.method ?: it.paymentMethod,
                            destinationPhone = group.destination?.phoneNumber.orEmpty(),
                            minSeats = maxOf(NewGroup.MIN_SEATS, detail.activeMembers),
                            originalSeats = group.seats,
                            turnsAssigned = group.readiness.turnsAssigned,
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isPreparing = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(NewGroup.NAME_MAX_LENGTH)) }

    fun onAmountChange(value: String) =
        _state.update { it.copy(amount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10)) }

    fun onPeriodicityChange(value: Periodicity) = _state.update { it.copy(periodicity = value) }

    fun onSeatsChange(value: Int) =
        _state.update { it.copy(seats = value.coerceIn(it.minSeats, NewGroup.MAX_SEATS)) }

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

    /** The last step: creates the group, or saves the changes of the one being edited. */
    fun submit() {
        val current = _state.value
        if (!current.rulesValid || !current.datesValid || current.isLoading) return
        val groupId = current.editingGroupId ?: return create()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            updateGroup(groupId, current.toNewGroup())
                .onSuccess { _state.update { it.copy(isLoading = false, saved = true) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    private fun create() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            createGroup(current.toNewGroup())
                .onSuccess { created -> _state.update { it.copy(isLoading = false, created = created) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onCreatedHandled() = _state.update { it.copy(created = null) }

    fun onSavedHandled() = _state.update { it.copy(saved = false) }
}
