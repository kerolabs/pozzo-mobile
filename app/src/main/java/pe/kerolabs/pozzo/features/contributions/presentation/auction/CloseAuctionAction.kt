package pe.kerolabs.pozzo.features.contributions.presentation.auction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.CloseAuctionUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetCycleOfGroupUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Auction
import pe.kerolabs.pozzo.features.contributions.domain.AuctionFallback
import pe.kerolabs.pozzo.features.contributions.presentation.common.MemberOption
import pe.kerolabs.pozzo.features.savingsgroups.application.GetTurnsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.Turn

data class CloseAuctionUiState(
    /** The members who have not collected yet, among whom the organizer chooses when nobody bid. */
    val candidates: List<Turn> = emptyList(),
    val winner: String? = null,
    val isClosing: Boolean = false,
    val errorMessage: String? = null,
    /** The auction once closed, with its winner, to hand back to the screen. */
    val closed: Auction? = null,
)

/**
 * US21: the organizer closes the auction of the turn. With bids the highest one wins; without bids the turn
 * goes by draw, or to the member the organizer chooses when the group decided so.
 */
@HiltViewModel
class CloseAuctionViewModel @Inject constructor(
    private val getCycleOfGroup: GetCycleOfGroupUseCase,
    private val getTurns: GetTurnsUseCase,
    private val closeAuction: CloseAuctionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CloseAuctionUiState())
    val state: StateFlow<CloseAuctionUiState> = _state.asStateFlow()

    /** The turns from the one in progress onward belong to the members who have not collected. */
    fun loadCandidates(groupId: String) {
        viewModelScope.launch {
            getCycleOfGroup(groupId).mapCatching { cycle ->
                getTurns(groupId).getOrThrow().turns.filter { it.turnNumber >= cycle.currentTurn }
            }
                .onSuccess { candidates -> _state.update { it.copy(candidates = candidates) } }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.userMessage()) } }
        }
    }

    fun onWinnerSelected(membershipId: String) = _state.update { it.copy(winner = membershipId, errorMessage = null) }

    fun close(periodId: String, winnerMembershipId: String?) {
        if (_state.value.isClosing) return
        viewModelScope.launch {
            _state.update { it.copy(isClosing = true, errorMessage = null) }
            closeAuction(periodId, winnerMembershipId)
                .onSuccess { auction -> _state.update { it.copy(isClosing = false, closed = auction) } }
                .onFailure { error -> _state.update { it.copy(isClosing = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onClosedShown() = _state.update { it.copy(closed = null, winner = null) }
}

/**
 * The organizer's action on an open auction: close it now, before its closing day. [onClosed] gets the closed
 * auction, with who won.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseAuctionAction(groupId: String, auction: Auction, onClosed: (Auction) -> Unit) {
    if (!auction.isOpen) return
    val viewModel: CloseAuctionViewModel = hiltViewModel(key = "close-auction-${auction.periodId}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    val organizerChooses = auction.bids.isEmpty() && auction.fallback == AuctionFallback.ORGANIZER

    LaunchedEffect(groupId) { viewModel.loadCandidates(groupId) }
    LaunchedEffect(state.closed) {
        val closed = state.closed ?: return@LaunchedEffect
        confirming = false
        choosing = false
        viewModel.onClosedShown()
        onClosed(closed)
    }

    state.errorMessage?.takeIf { !confirming && !choosing }?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
    PozzoOutlinedButton(
        text = "Cerrar la subasta",
        onClick = { if (organizerChooses) choosing = true else confirming = true },
        icon = Icons.Outlined.LockClock,
    )

    if (confirming) {
        val highest = auction.highestBid
        AlertDialog(
            onDismissRequest = { if (!state.isClosing) confirming = false },
            icon = { Icon(Icons.Outlined.LockClock, contentDescription = null) },
            title = { Text("¿Cerrar la subasta ahora?") },
            text = {
                Text(
                    if (highest != null) "Cobra ${highest.displayName}, con la mayor oferta. Ya no se aceptan más ofertas."
                    else "Nadie ofertó: el turno se sortea entre quienes aún no cobran, sin descuento.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.close(auction.periodId, null) }, enabled = !state.isClosing) { Text("Cerrar") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }, enabled = !state.isClosing) { Text("Cancelar") } },
        )
    }

    if (choosing) {
        ModalBottomSheet(onDismissRequest = { if (!state.isClosing) choosing = false }) {
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                Text("¿Quién cobra este turno?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nadie ofertó. Elige entre quienes aún no cobran; recibe el pozo completo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                state.candidates.forEach { turn ->
                    MemberOption(
                        name = turn.displayName,
                        subtitle = null,
                        isMe = turn.isMe,
                        selected = state.winner == turn.membershipId,
                        onClick = { viewModel.onWinnerSelected(turn.membershipId) },
                        photoUrl = turn.photoUrl,
                    )
                }
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                PozzoPrimaryButton(
                    text = "Dar el turno",
                    onClick = { viewModel.close(auction.periodId, state.winner) },
                    enabled = state.winner != null,
                    loading = state.isClosing,
                )
            }
        }
    }
}
