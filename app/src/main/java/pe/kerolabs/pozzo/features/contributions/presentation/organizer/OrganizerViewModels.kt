package pe.kerolabs.pozzo.features.contributions.presentation.organizer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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
import pe.kerolabs.pozzo.features.contributions.application.DeliverPotUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPendingReviewsUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.application.RegisterCashUseCase
import pe.kerolabs.pozzo.features.contributions.application.RegisterCoverageUseCase
import pe.kerolabs.pozzo.features.contributions.application.ReviewContributionUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Contribution
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.PotDelivery
import pe.kerolabs.pozzo.features.contributions.domain.ReviewDecision
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.CashRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.CoverRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.DeliverRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.ReviewDetailRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.ReviewsRoute

/** H1 and H2: the contributions whose receipt did not match, and the decision on one of them. */
data class ReviewsUiState(
    val cycle: Cycle? = null,
    val period: Period? = null,
    val reviews: List<Contribution> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val decided: ReviewDecision? = null,
)

abstract class ReviewsBaseViewModel(
    private val groupId: String,
    val contributionId: String?,
    private val getPotState: GetPotStateUseCase,
    private val getPendingReviews: GetPendingReviewsUseCase,
    private val reviewContribution: ReviewContributionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewsUiState())
    val state: StateFlow<ReviewsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.period == null, errorMessage = null) }
            val pot = getPotState(groupId).getOrElse { error ->
                _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) }
                return@launch
            }
            getPendingReviews(pot.second.id)
                .onSuccess { reviews ->
                    _state.update { it.copy(cycle = pot.first, period = pot.second, reviews = reviews, isLoading = false) }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun decide(decision: ReviewDecision, note: String?) {
        val id = contributionId ?: return
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            reviewContribution(id, decision, note?.trim()?.ifEmpty { null })
                .onSuccess { _state.update { it.copy(isSaving = false, decided = decision) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }
}

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPotState: GetPotStateUseCase,
    getPendingReviews: GetPendingReviewsUseCase,
    reviewContribution: ReviewContributionUseCase,
) : ReviewsBaseViewModel(
    savedStateHandle.toRoute<ReviewsRoute>().groupId, null, getPotState, getPendingReviews, reviewContribution,
)

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPotState: GetPotStateUseCase,
    getPendingReviews: GetPendingReviewsUseCase,
    reviewContribution: ReviewContributionUseCase,
) : ReviewsBaseViewModel(
    savedStateHandle.toRoute<ReviewDetailRoute>().groupId,
    savedStateHandle.toRoute<ReviewDetailRoute>().contributionId,
    getPotState,
    getPendingReviews,
    reviewContribution,
)

/** H3 and H5: a cash contribution received by the organizer, or a contribution covered by another member. */
data class SettleUiState(
    val cycle: Cycle? = null,
    val period: Period? = null,
    val isLoading: Boolean = true,
    val selectedMember: String? = null,
    val coveredBy: String? = null,
    val amount: String = "",
    val receivedOn: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val done: Contribution? = null,
) {
    val amountValue: BigDecimal? get() = amount.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
}

abstract class SettleViewModel(
    private val groupId: String,
    private val getPotState: GetPotStateUseCase,
) : ViewModel() {

    protected val _state = MutableStateFlow(SettleUiState())
    val state: StateFlow<SettleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getPotState(groupId)
                .onSuccess { (cycle, period) ->
                    _state.update {
                        it.copy(
                            cycle = cycle,
                            period = period,
                            isLoading = false,
                            amount = period.contributionAmount.stripTrailingZeros().toPlainString(),
                            selectedMember = period.unsettled.singleOrNull()?.membershipId,
                            coveredBy = cycle.myMembershipId,
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onMemberSelected(membershipId: String) = _state.update {
        it.copy(selectedMember = membershipId, coveredBy = if (it.coveredBy == membershipId) null else it.coveredBy)
    }

    fun onCoveredBySelected(membershipId: String) = _state.update { it.copy(coveredBy = membershipId) }

    fun onAmountChange(value: String) =
        _state.update { it.copy(amount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10)) }

    fun onReceivedOnChange(value: LocalDate) = _state.update { it.copy(receivedOn = value) }

    protected fun save(block: suspend (SettleUiState, Period) -> Result<Contribution>) {
        val current = _state.value
        val period = current.period ?: return
        if (current.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            block(current, period)
                .onSuccess { contribution -> _state.update { it.copy(isSaving = false, done = contribution) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }
}

@HiltViewModel
class CashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPotState: GetPotStateUseCase,
    private val registerCash: RegisterCashUseCase,
) : SettleViewModel(savedStateHandle.toRoute<CashRoute>().groupId, getPotState) {

    fun register() = save { state, period ->
        val member = state.selectedMember ?: return@save Result.failure(IllegalStateException("Elige a quién recibiste el aporte."))
        val amount = state.amountValue ?: return@save Result.failure(IllegalStateException("Ingresa el monto recibido."))
        registerCash(period.id, member, amount, state.receivedOn)
    }
}

@HiltViewModel
class CoverViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPotState: GetPotStateUseCase,
    private val registerCoverage: RegisterCoverageUseCase,
) : SettleViewModel(savedStateHandle.toRoute<CoverRoute>().groupId, getPotState) {

    fun register() = save { state, period ->
        val member = state.selectedMember ?: return@save Result.failure(IllegalStateException("Elige el aporte que se cubre."))
        val coveredBy = state.coveredBy ?: return@save Result.failure(IllegalStateException("Elige quién lo cubre."))
        registerCoverage(period.id, member, coveredBy)
    }
}

/** H4: the organizer marks the pot as delivered to whoever collects the turn. */
data class DeliverUiState(
    val cycle: Cycle? = null,
    val period: Period? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val delivery: PotDelivery? = null,
)

@HiltViewModel
class DeliverViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPotState: GetPotStateUseCase,
    private val deliverPot: DeliverPotUseCase,
) : ViewModel() {

    val groupId: String = savedStateHandle.toRoute<DeliverRoute>().groupId

    private val _state = MutableStateFlow(DeliverUiState())
    val state: StateFlow<DeliverUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getPotState(groupId)
                .onSuccess { (cycle, period) -> _state.update { it.copy(cycle = cycle, period = period, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun deliver() {
        val period = _state.value.period ?: return
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            deliverPot(period.id)
                .onSuccess { delivery -> _state.update { it.copy(isSaving = false, delivery = delivery) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }
}
