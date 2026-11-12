package pe.kerolabs.pozzo.features.savingsgroups.presentation.turns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.FieldLabel
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.NumberStepper
import pe.kerolabs.pozzo.core.designsystem.components.OptionChip
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.savingsgroups.application.AssignTurnsByAuctionUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.AuctionFallback
import pe.kerolabs.pozzo.features.savingsgroups.domain.AuctionRules
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.AuctionRulesRoute

/** The backend accepts closing the auction from the cutoff day itself up to 30 days before it. */
private val CLOSING_DAYS = 0..30

data class AuctionRulesUiState(
    val closesDaysBeforeCutoff: Int = 2,
    val minimumBid: String = "",
    val fallback: AuctionFallback = AuctionFallback.DRAW,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
) {
    val minimumBidValue: BigDecimal?
        get() = minimumBid.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
}

/**
 * US19: the organizer chooses to auction the turns and sets when each auction closes, the smallest discount
 * accepted and what happens when nobody bids.
 */
@HiltViewModel
class AuctionRulesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val assignTurnsByAuction: AssignTurnsByAuctionUseCase,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<AuctionRulesRoute>().groupId

    private val _state = MutableStateFlow(AuctionRulesUiState())
    val state: StateFlow<AuctionRulesUiState> = _state.asStateFlow()

    fun onClosingDaysChange(value: Int) =
        _state.update { it.copy(closesDaysBeforeCutoff = value.coerceIn(CLOSING_DAYS), errorMessage = null) }

    fun onMinimumBidChange(value: String) =
        _state.update { it.copy(minimumBid = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10), errorMessage = null) }

    fun onFallbackChange(value: AuctionFallback) = _state.update { it.copy(fallback = value) }

    fun save() {
        val current = _state.value
        val minimumBid = current.minimumBidValue ?: return
        if (current.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            assignTurnsByAuction(groupId, AuctionRules(current.closesDaysBeforeCutoff, minimumBid, current.fallback))
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }
}

/**
 * US19: the rules of the auction of the turns. Nothing is assigned in advance: each period opens its own
 * auction among the members who have not collected yet.
 */
@Composable
fun AuctionRulesScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: AuctionRulesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(topBar = { PozzoTopBar(title = "Turnos por subasta", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                InfoBanner(
                    "Cada período abre una subasta entre quienes aún no cobran. Gana quien ofrece el mayor descuento " +
                        "sobre el pozo, y ese descuento se reparte entre los demás en su siguiente aporte.",
                    Icons.Outlined.Gavel,
                )
                Column {
                    FieldLabel("¿Cuántos días antes del corte cierra cada subasta?")
                    Spacer(Modifier.height(8.dp))
                    NumberStepper(
                        value = state.closesDaysBeforeCutoff,
                        onValueChange = viewModel::onClosingDaysChange,
                        range = CLOSING_DAYS,
                    )
                    Text(
                        if (state.closesDaysBeforeCutoff == 0) "Cierra el mismo día del corte."
                        else "Cierra ${state.closesDaysBeforeCutoff} ${if (state.closesDaysBeforeCutoff == 1) "día" else "días"} antes del corte.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PozzoTextField(
                    label = "Oferta mínima",
                    value = state.minimumBid,
                    onValueChange = viewModel::onMinimumBidChange,
                    prefix = "S/",
                    placeholder = "20.00",
                    supportingText = "El descuento más pequeño que se acepta sobre el pozo.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Column {
                    FieldLabel("Si nadie oferta")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionChip(
                            text = "Sorteo",
                            selected = state.fallback == AuctionFallback.DRAW,
                            onClick = { viewModel.onFallbackChange(AuctionFallback.DRAW) },
                        )
                        OptionChip(
                            text = "Elijo yo",
                            selected = state.fallback == AuctionFallback.ORGANIZER,
                            onClick = { viewModel.onFallbackChange(AuctionFallback.ORGANIZER) },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.fallback == AuctionFallback.DRAW) "El turno se sortea entre quienes aún no cobran, sin descuento."
                        else "Tú eliges quién cobra entre quienes aún no cobran, sin descuento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))
            PozzoPrimaryButton(
                text = "Confirmar subasta",
                onClick = viewModel::save,
                enabled = state.minimumBidValue != null,
                loading = state.isSaving,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
