package pe.kerolabs.pozzo.features.contributions.presentation.auction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.GetTurnAuctionUseCase
import pe.kerolabs.pozzo.features.contributions.application.PlaceBidUseCase
import pe.kerolabs.pozzo.features.contributions.application.TurnAuction
import pe.kerolabs.pozzo.features.contributions.domain.Auction
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.AuctionRoute

data class AuctionUiState(
    val turnAuction: TurnAuction? = null,
    val bid: String = "",
    val isLoading: Boolean = true,
    val isBidding: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
) {
    val bidValue: BigDecimal? get() = bid.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
}

/**
 * US20: the auction of the turn in progress. A member who has not collected offers a discount on the pot;
 * every member sees the bids from the highest to the lowest.
 */
@HiltViewModel
class AuctionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTurnAuction: GetTurnAuctionUseCase,
    private val placeBid: PlaceBidUseCase,
) : ViewModel() {

    val groupId: String = savedStateHandle.toRoute<AuctionRoute>().groupId

    private val _state = MutableStateFlow(AuctionUiState())
    val state: StateFlow<AuctionUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.turnAuction == null, errorMessage = null) }
            getTurnAuction(groupId)
                .onSuccess { turnAuction -> _state.update { it.copy(isLoading = false, turnAuction = turnAuction) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onBidChange(value: String) =
        _state.update { it.copy(bid = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10), errorMessage = null) }

    fun bid() {
        val current = _state.value
        val turnAuction = current.turnAuction ?: return
        val amount = current.bidValue ?: return
        if (current.isBidding) return
        viewModelScope.launch {
            _state.update { it.copy(isBidding = true, errorMessage = null) }
            placeBid(turnAuction.period.id, amount)
                .onSuccess { auction ->
                    _state.update {
                        it.copy(
                            isBidding = false,
                            bid = "",
                            turnAuction = turnAuction.copy(auction = auction),
                            message = if (auction.myBidIsHighest) "Tu oferta es la mayor por ahora." else "Registramos tu oferta.",
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isBidding = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    /** Shows the auction as an action of the organizer left it, such as closed with its winner. */
    fun onAuctionChanged(auction: Auction) =
        _state.update { state -> state.copy(turnAuction = state.turnAuction?.copy(auction = auction)) }
}
