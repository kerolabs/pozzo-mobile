package pe.kerolabs.pozzo.features.contributions.presentation.mycontributions

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
import pe.kerolabs.pozzo.features.contributions.application.GetCycleOfGroupUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetMyContributionsUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.MyContributions
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.MyContributionsRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.TurnCalendarRoute
import pe.kerolabs.pozzo.features.savingsgroups.application.GetTurnsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.TurnCalendar

data class MyContributionsUiState(
    val contributions: MyContributions? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** G1: the member's contributions in a group and their receipts, also without connection. */
@HiltViewModel
class MyContributionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMyContributions: GetMyContributionsUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<MyContributionsRoute>().groupId

    private val _state = MutableStateFlow(MyContributionsUiState())
    val state: StateFlow<MyContributionsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getMyContributions(groupId)
                .onSuccess { contributions -> _state.update { it.copy(contributions = contributions, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }
}

data class TurnCalendarUiState(
    val cycle: Cycle? = null,
    val calendar: TurnCalendar? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** G2: who collects in each period and when, with the turn in progress marked. */
@HiltViewModel
class TurnCalendarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCycleOfGroup: GetCycleOfGroupUseCase,
    private val getTurns: GetTurnsUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<TurnCalendarRoute>().groupId

    private val _state = MutableStateFlow(TurnCalendarUiState())
    val state: StateFlow<TurnCalendarUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = getCycleOfGroup(groupId).mapCatching { cycle -> cycle to getTurns(groupId).getOrThrow() }
            result
                .onSuccess { (cycle, calendar) -> _state.update { it.copy(cycle = cycle, calendar = calendar, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }
}
