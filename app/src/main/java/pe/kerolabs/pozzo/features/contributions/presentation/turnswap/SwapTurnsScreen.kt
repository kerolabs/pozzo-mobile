package pe.kerolabs.pozzo.features.contributions.presentation.turnswap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.GetCycleOfGroupUseCase
import pe.kerolabs.pozzo.features.contributions.application.SwapTurnsUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError
import pe.kerolabs.pozzo.features.contributions.presentation.common.MemberOption
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.SwapTurnsRoute
import pe.kerolabs.pozzo.features.savingsgroups.application.GetTurnsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.Turn

data class SwapTurnsUiState(
    val cycle: Cycle? = null,
    /** The turns whose period has not started, the only ones that can be swapped. */
    val turns: List<Turn> = emptyList(),
    val selected: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val swapped: Boolean = false,
)

/**
 * US18: two members agreed to collect in each other's turn; the organizer swaps two turns whose periods have
 * not started, and both members get a notice with their new turn.
 */
@HiltViewModel
class SwapTurnsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCycleOfGroup: GetCycleOfGroupUseCase,
    private val getTurns: GetTurnsUseCase,
    private val swapTurns: SwapTurnsUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<SwapTurnsRoute>().groupId

    private val _state = MutableStateFlow(SwapTurnsUiState())
    val state: StateFlow<SwapTurnsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getCycleOfGroup(groupId).mapCatching { cycle ->
                cycle to getTurns(groupId).getOrThrow().turns.filter { it.turnNumber > cycle.currentTurn }
            }
                .onSuccess { (cycle, turns) -> _state.update { it.copy(isLoading = false, cycle = cycle, turns = turns) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    /** Picks up to two members; picking a third replaces the oldest choice. */
    fun toggle(membershipId: String) = _state.update { state ->
        val selected = when {
            membershipId in state.selected -> state.selected - membershipId
            state.selected.size < 2 -> state.selected + membershipId
            else -> state.selected.drop(1) + membershipId
        }
        state.copy(selected = selected, errorMessage = null)
    }

    fun swap() {
        val current = _state.value
        val cycle = current.cycle ?: return
        if (current.selected.size != 2 || current.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            swapTurns(cycle.id, current.selected[0], current.selected[1])
                .onSuccess { _state.update { it.copy(isSaving = false, swapped = true) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }
}

/**
 * US18: the organizer picks the two members whose turns are swapped. Only the turns whose period has not
 * started appear.
 */
@Composable
fun SwapTurnsScreen(onBack: () -> Unit, onSwapped: () -> Unit, viewModel: SwapTurnsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.swapped) { if (state.swapped) onSwapped() }

    Scaffold(topBar = { PozzoTopBar(title = "Intercambiar turnos", onBack = onBack) }) { padding ->
        if (state.cycle == null) {
            LoadingOrError(state.isLoading, state.errorMessage, viewModel::load, Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Elige a los dos integrantes que acordaron cambiar de turno.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (state.turns.size < 2) {
                    InfoBanner("Quedan menos de dos turnos sin empezar, así que no hay nada que intercambiar.", Icons.Outlined.Info)
                }
                state.turns.forEach { turn ->
                    MemberOption(
                        name = turn.displayName,
                        subtitle = "Turno ${turn.turnNumber} · corte ${formatMediumDate(turn.cutoffDate)}",
                        isMe = turn.isMe,
                        selected = turn.membershipId in state.selected,
                        onClick = { viewModel.toggle(turn.membershipId) },
                        photoUrl = turn.photoUrl,
                    )
                }
                Spacer(Modifier.height(8.dp))
                InfoBanner("A los dos les llega un aviso con su nuevo turno.", Icons.Outlined.Info)
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))
            PozzoPrimaryButton(
                text = "Intercambiar turnos",
                onClick = viewModel::swap,
                enabled = state.selected.size == 2,
                loading = state.isSaving,
                icon = Icons.Outlined.SwapVert,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
