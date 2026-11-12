package pe.kerolabs.pozzo.features.contributions.presentation.periods

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.GetCycleHistoryUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.PeriodState
import pe.kerolabs.pozzo.features.contributions.presentation.common.ContributionStateChip
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.PastPeriodsRoute
import pe.kerolabs.pozzo.features.contributions.presentation.payout.PayoutProofViewer
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.rememberMemberPhotos

data class PastPeriodsUiState(
    val cycle: Cycle? = null,
    /** The periods whose pot was already delivered, the most recent first. */
    val periods: List<Period> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** US32: the periods already closed of the cycle of a group. */
@HiltViewModel
class PastPeriodsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCycleHistory: GetCycleHistoryUseCase,
) : ViewModel() {

    val groupId: String = savedStateHandle.toRoute<PastPeriodsRoute>().groupId

    private val _state = MutableStateFlow(PastPeriodsUiState())
    val state: StateFlow<PastPeriodsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getCycleHistory(groupId)
                .onSuccess { (cycle, periods) ->
                    val delivered = periods.filter { it.state == PeriodState.DELIVERED }.sortedByDescending { it.turnNumber }
                    _state.update { it.copy(isLoading = false, cycle = cycle, periods = delivered) }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }
}

/**
 * US32: every closed period with who collected, how much was delivered and when, the final state of each
 * member's contribution and, when the organizer attached it, the transfer of the pot (US36).
 */
@Composable
fun PastPeriodsScreen(onBack: () -> Unit, viewModel: PastPeriodsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = rememberMemberPhotos(viewModel.groupId)
    var proofOf by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { PozzoTopBar(title = "Períodos anteriores", onBack = onBack) }) { padding ->
        if (state.cycle == null) {
            LoadingOrError(state.isLoading, state.errorMessage, viewModel::load, Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.periods.isEmpty()) {
                item { InfoBanner("Aún no se entrega ningún pozo en este ciclo.", Icons.Outlined.Info) }
            }
            items(state.periods, key = { it.id }) { period ->
                PeriodCard(period = period, photos = photos, onSeeProof = { proofOf = period.id })
            }
        }
    }
    proofOf?.let { periodId -> PayoutProofViewer(periodId, onDismiss = { proofOf = null }) }
}

@Composable
private fun PeriodCard(period: Period, photos: Map<String, String>, onSeeProof: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .padding(16.dp),
    ) {
        Text("Período ${period.turnNumber} · cobró ${period.payoutMemberName}", style = MaterialTheme.typography.titleMedium)
        val deliveredOn = period.deliveredAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
        Text(
            buildString {
                append("Recibió ${formatSoles(period.payoutAmount)}")
                if (period.auctionDiscount.signum() > 0) append(" (descuento de subasta ${formatSoles(period.auctionDiscount)})")
                deliveredOn?.let { append(" · entregado el ${formatMediumDate(it)}") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        period.members.forEach { member ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = member.displayName, size = 28.dp, photoUrl = photos[member.membershipId])
                Spacer(Modifier.width(8.dp))
                Text(
                    if (member.isMe) "${member.displayName} (tú)" else member.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                ContributionStateChip(member.state, isMine = member.isMe)
            }
        }
        if (period.hasPayoutProof) {
            Spacer(Modifier.height(8.dp))
            PozzoOutlinedButton(text = "Ver la transferencia del pozo", onClick = onSeeProof, icon = Icons.Outlined.Image)
        }
    }
}
