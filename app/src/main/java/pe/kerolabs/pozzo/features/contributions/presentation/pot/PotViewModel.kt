package pe.kerolabs.pozzo.features.contributions.presentation.pot

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
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.PotRoute

/**
 * UI state for the contribution pot screen (F1).
 *
 * @property cycle Active savings group cycle definition and metadata.
 * @property period Current round/period data, including member contribution records and pot progress.
 * @property isLoading Indicates whether initial network synchronization is taking place.
 * @property errorMessage Human-readable description of any encountered failure.
 */
data class PotUiState(
    val cycle: Cycle? = null,
    val period: Period? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * ViewModel managing the state of an active savings group pot and period contributions (F1).
 *
 * Shared across cycle screens, it retrieves cycle details, turn distribution, member payment receipts,
 * and settlement deadlines for the specified [groupId] extracted from the navigation route.
 *
 * @param savedStateHandle Saved state handle containing navigation route arguments.
 * @param getPotState Use case retrieving cycle and active period status.
 */
@HiltViewModel
class PotViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPotState: GetPotStateUseCase,
) : ViewModel() {

    val groupId: String = savedStateHandle.toRoute<PotRoute>().groupId

    private val _state = MutableStateFlow(PotUiState())
    val state: StateFlow<PotUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.period == null, errorMessage = null) }
            getPotState(groupId)
                .onSuccess { (cycle, period) -> _state.update { it.copy(isLoading = false, cycle = cycle, period = period) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }
}
