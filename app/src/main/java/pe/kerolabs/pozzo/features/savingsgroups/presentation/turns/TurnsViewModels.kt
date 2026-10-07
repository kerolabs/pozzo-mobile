package pe.kerolabs.pozzo.features.savingsgroups.presentation.turns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.savingsgroups.application.AssignAgreedTurnsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.DrawTurnsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.GetGroupDetailUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.StartGroupUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupDetail
import pe.kerolabs.pozzo.features.savingsgroups.domain.Member
import pe.kerolabs.pozzo.features.savingsgroups.domain.TurnCalendar
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.AgreedOrderRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.DrawResultRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.StartGroupRoute

data class DrawUiState(
    val calendar: TurnCalendar? = null,
    val isDrawing: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * E4: draws the order as soon as it opens; drawing again replaces the result until the group starts.
 */
@HiltViewModel
class DrawResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val drawTurns: DrawTurnsUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<DrawResultRoute>().groupId

    private val _state = MutableStateFlow(DrawUiState())
    val state: StateFlow<DrawUiState> = _state.asStateFlow()

    init {
        draw()
    }

    fun draw() {
        viewModelScope.launch {
            _state.update { it.copy(isDrawing = true, errorMessage = null) }
            drawTurns(groupId)
                .onSuccess { calendar -> _state.update { it.copy(isDrawing = false, calendar = calendar) } }
                .onFailure { error -> _state.update { it.copy(isDrawing = false, errorMessage = error.userMessage()) } }
        }
    }
}

data class AgreedOrderUiState(
    val order: List<Member> = emptyList(),
    val detail: GroupDetail? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

/**
 * E5: the organizer puts the members in the order the group agreed.
 */
@HiltViewModel
class AgreedOrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupDetail: GetGroupDetailUseCase,
    private val assignAgreedTurns: AssignAgreedTurnsUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<AgreedOrderRoute>().groupId

    private val _state = MutableStateFlow(AgreedOrderUiState())
    val state: StateFlow<AgreedOrderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getGroupDetail(groupId)
                .onSuccess { detail ->
                    // Starts from the current order when there is one, otherwise in the order members joined.
                    val order = detail.members.sortedBy { it.turnNumber ?: Int.MAX_VALUE }
                    _state.update { it.copy(isLoading = false, detail = detail, order = order) }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun move(from: Int, to: Int) {
        val order = _state.value.order
        if (from !in order.indices || to !in order.indices) return
        _state.update { it.copy(order = order.toMutableList().apply { add(to, removeAt(from)) }) }
    }

    fun confirm() {
        val order = _state.value.order
        if (order.isEmpty() || _state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            assignAgreedTurns(groupId, order.map { it.membershipId })
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }
}

data class StartGroupUiState(
    val detail: GroupDetail? = null,
    val isLoading: Boolean = true,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
    val started: Boolean = false,
)

/**
 * E6: the last check before starting; starting freezes the rules and opens the first period.
 */
@HiltViewModel
class StartGroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupDetail: GetGroupDetailUseCase,
    private val startGroup: StartGroupUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<StartGroupRoute>().groupId

    private val _state = MutableStateFlow(StartGroupUiState())
    val state: StateFlow<StartGroupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getGroupDetail(groupId)
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun start() {
        if (_state.value.isStarting) return
        viewModelScope.launch {
            _state.update { it.copy(isStarting = true, errorMessage = null) }
            startGroup(groupId)
                .onSuccess { _state.update { it.copy(isStarting = false, started = true) } }
                .onFailure { error -> _state.update { it.copy(isStarting = false, errorMessage = error.userMessage()) } }
        }
    }
}
