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
import pe.kerolabs.pozzo.features.contributions.application.GetCycleHistoryUseCase
import pe.kerolabs.pozzo.features.contributions.application.ReopenReviewUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetReviewedContributionsUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.application.RegisterCashUseCase
import pe.kerolabs.pozzo.features.contributions.application.RegisterCoverageUseCase
import pe.kerolabs.pozzo.features.contributions.application.ReviewContributionUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Contribution
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.PeriodState
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
    /** Approved or rejected in this period, and still possible to review again. */
    val reviewed: List<Contribution> = emptyList(),
    val reopening: String? = null,
)

abstract class ReviewsBaseViewModel(
    private val groupId: String,
    val contributionId: String?,
    private val getPotState: GetPotStateUseCase,
    private val getPendingReviews: GetPendingReviewsUseCase,
    private val reviewContribution: ReviewContributionUseCase,
    private val getReviewed: GetReviewedContributionsUseCase,
    private val reopenReview: ReopenReviewUseCase,
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
            val reviewed = getReviewed(pot.second.id).getOrDefault(emptyList())
            getPendingReviews(pot.second.id)
                .onSuccess { reviews ->
                    _state.update {
                        it.copy(cycle = pot.first, period = pot.second, reviews = reviews, reviewed = reviewed, isLoading = false)
                    }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    /** Takes back the approval or the rejection of a contribution, which goes back to the list to review. */
    fun reopen(contributionId: String) {
        if (_state.value.reopening != null) return
        viewModelScope.launch {
            _state.update { it.copy(reopening = contributionId, errorMessage = null) }
            reopenReview(contributionId)
                .onSuccess {
                    _state.update { it.copy(reopening = null) }
                    load()
                }
                .onFailure { error -> _state.update { it.copy(reopening = null, errorMessage = error.userMessage()) } }
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
    getReviewed: GetReviewedContributionsUseCase,
    reopenReview: ReopenReviewUseCase,
) : ReviewsBaseViewModel(
    savedStateHandle.toRoute<ReviewsRoute>().groupId, null, getPotState, getPendingReviews, reviewContribution,
    getReviewed, reopenReview,
)

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPotState: GetPotStateUseCase,
    getPendingReviews: GetPendingReviewsUseCase,
    reviewContribution: ReviewContributionUseCase,
    getReviewed: GetReviewedContributionsUseCase,
    reopenReview: ReopenReviewUseCase,
) : ReviewsBaseViewModel(
    savedStateHandle.toRoute<ReviewDetailRoute>().groupId,
    savedStateHandle.toRoute<ReviewDetailRoute>().contributionId,
    getPotState,
    getPendingReviews,
    reviewContribution,
    getReviewed,
    reopenReview,
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
                            amount = period.unsettled.singleOrNull()?.let { period.amountOwedBy(it.membershipId) }
                                ?.stripTrailingZeros()?.toPlainString()
                                ?: period.contributionAmount.stripTrailingZeros().toPlainString(),
                            selectedMember = period.unsettled.singleOrNull()?.membershipId,
                            coveredBy = cycle.myMembershipId,
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    /** Picking a member also proposes what they owe, which an auction may have lowered. */
    fun onMemberSelected(membershipId: String) = _state.update {
        it.copy(
            selectedMember = membershipId,
            coveredBy = if (it.coveredBy == membershipId) null else it.coveredBy,
            amount = it.period?.amountOwedBy(membershipId)?.stripTrailingZeros()?.toPlainString() ?: it.amount,
        )
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

    /** [differenceConfirmed] is true once the organizer confirmed an amount other than what the member owes. */
    fun register(differenceConfirmed: Boolean = false) = save { state, period ->
        val member = state.selectedMember ?: return@save Result.failure(IllegalStateException("Elige a quién recibiste el aporte."))
        val amount = state.amountValue ?: return@save Result.failure(IllegalStateException("Ingresa el monto recibido."))
        registerCash(period.id, member, amount, state.receivedOn, differenceConfirmed)
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

/** The delivery of [period] as the history shows it, or null when it was not recorded. */
private suspend fun DeliverViewModel.recordedDelivery(period: Period, groupId: String, history: GetCycleHistoryUseCase): PotDelivery? {
    val (cycle, periods) = history(groupId).getOrNull() ?: return null
    val delivered = periods.firstOrNull { it.id == period.id && it.state == PeriodState.DELIVERED } ?: return null
    return PotDelivery(
        deliveredPeriodId = delivered.id,
        deliveredTurn = delivered.turnNumber,
        deliveredAmount = delivered.payoutAmount,
        cycleClosed = !cycle.isActive,
        nextTurn = if (cycle.isActive) cycle.currentTurn else null,
    )
}

@HiltViewModel
class DeliverViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPotState: GetPotStateUseCase,
    private val deliverPot: DeliverPotUseCase,
    private val getCycleHistory: GetCycleHistoryUseCase,
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
                .onFailure { error ->
                    // The answer may have been lost after the delivery was recorded: look before reporting a failure.
                    val recorded = deliveryRecorded(period)
                    _state.update {
                        if (recorded != null) {
                            it.copy(isSaving = false, delivery = recorded)
                        } else {
                            it.copy(
                                isSaving = false,
                                errorMessage = "No se registró la entrega: ${error.userMessage()} " +
                                    "El pozo sigue pendiente de entrega; puedes intentarlo de nuevo.",
                            )
                        }
                    }
                }
        }
    }

    private suspend fun deliveryRecorded(period: Period): PotDelivery? = recordedDelivery(period, groupId, getCycleHistory)
}
