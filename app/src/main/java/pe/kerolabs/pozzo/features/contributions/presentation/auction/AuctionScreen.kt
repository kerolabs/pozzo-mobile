package pe.kerolabs.pozzo.features.contributions.presentation.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.contributions.domain.Auction
import pe.kerolabs.pozzo.features.contributions.domain.AuctionBid
import pe.kerolabs.pozzo.features.contributions.domain.AuctionResolution
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.rememberMemberPhotos

/**
 * US20: the auction of the turn in progress. Shows when it closes, the pot and the bids from the highest to
 * the lowest with what each bidder would receive; a member who has not collected places or improves a bid.
 * [organizerActions] adds what the organizer can do with the auction, such as closing it.
 */
@Composable
fun AuctionScreen(
    onBack: () -> Unit,
    viewModel: AuctionViewModel = hiltViewModel(),
    organizerActions: @Composable ColumnScope.(auction: Auction, reload: () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val photos = rememberMemberPhotos(viewModel.groupId)

    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(topBar = { PozzoTopBar(title = "Subasta del turno", onBack = onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        val turnAuction = state.turnAuction
        if (turnAuction == null) {
            LoadingOrError(state.isLoading, state.errorMessage, viewModel::load, Modifier.padding(padding))
            return@Scaffold
        }
        val auction = turnAuction.auction
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AuctionCard(auction)
                if (!auction.isOpen) {
                    AuctionResult(auction)
                }
                Text("Ofertas", style = MaterialTheme.typography.titleLarge)
                if (auction.bids.isEmpty()) {
                    InfoBanner(
                        if (auction.isOpen) "Aún no hay ofertas. Quien no ha cobrado puede ofrecer un descuento desde ${formatSoles(auction.minimumBid)}."
                        else "Nadie ofertó en esta subasta.",
                        Icons.Outlined.Info,
                    )
                }
                auction.bids.forEachIndexed { index, bid ->
                    BidRow(bid = bid, highest = index == 0, isMe = bid.membershipId == turnAuction.cycle.myMembershipId, photoUrl = photos[bid.membershipId])
                }
            }
            Column(Modifier.padding(top = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (auction.isOpen && auction.canBid) {
                    PozzoTextField(
                        label = if (auction.myBid == null) "Tu oferta de descuento" else "Mejora tu oferta (ahora ${formatSoles(auction.myBid)})",
                        value = state.bid,
                        onValueChange = viewModel::onBidChange,
                        prefix = "S/",
                        placeholder = auction.minimumBid.toPlainString(),
                        supportingText = state.bidValue?.let { "Recibirías ${formatSoles(auction.potAmount - it)} si ganas." }
                            ?: "Mínimo ${formatSoles(auction.minimumBid)} y menos que el pozo de ${formatSoles(auction.potAmount)}.",
                        errorText = state.errorMessage,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    PozzoPrimaryButton(
                        text = if (auction.myBid == null) "Ofertar" else "Mejorar oferta",
                        onClick = viewModel::bid,
                        enabled = state.bidValue != null,
                        loading = state.isBidding,
                        icon = Icons.Outlined.Gavel,
                    )
                } else if (auction.isOpen && !turnAuction.cycle.isOrganizer) {
                    Text(
                        "Ya cobraste en este ciclo, así que no puedes ofertar. Puedes seguir la subasta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (turnAuction.cycle.isOrganizer) organizerActions(auction, viewModel::load)
            }
        }
    }
}

@Composable
private fun AuctionCard(auction: Auction) {
    val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
    ) {
        Text("Turno ${auction.turnNumber} · Pozo ${formatSoles(auction.potAmount)}", style = MaterialTheme.typography.titleSmall, color = onBrand)
        val highest = auction.highestBid
        Text(
            highest?.let { formatSoles(it.amount) } ?: "Sin ofertas",
            style = MaterialTheme.typography.displaySmall,
            color = onBrand,
        )
        Text(
            if (highest != null) "mayor oferta, de ${highest.displayName}" else "oferta mínima ${formatSoles(auction.minimumBid)}",
            style = MaterialTheme.typography.bodyLarge,
            color = onBrand,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (auction.isOpen) "Abierta hasta el ${formatMediumDate(auction.closesOn)}" else "Cerrada",
            style = MaterialTheme.typography.labelLarge,
            color = onBrand,
        )
    }
}

@Composable
private fun AuctionResult(auction: Auction) {
    val winner = auction.winnerName ?: return
    val how = when (auction.resolution) {
        AuctionResolution.BID -> "con la mayor oferta, de ${auction.discount?.let(::formatSoles).orEmpty()}"
        AuctionResolution.DRAW -> "por sorteo, porque nadie ofertó"
        AuctionResolution.ORGANIZER -> "elegido por la cabeza, porque nadie ofertó"
        null -> ""
    }
    InfoBanner(
        "Cobra $winner $how. Recibe ${auction.payoutAmount?.let(::formatSoles).orEmpty()}.",
        Icons.Outlined.EmojiEvents,
    )
}

@Composable
private fun BidRow(bid: AuctionBid, highest: Boolean, isMe: Boolean, photoUrl: String?) {
    val status = PozzoThemeExtras.statusColors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        InitialsAvatar(name = bid.displayName, size = 40.dp, photoUrl = photoUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (isMe) "${bid.displayName} (tú)" else bid.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                "Ofrece ${formatSoles(bid.amount)} · recibiría ${formatSoles(bid.receives)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (highest) StatusChip("Mayor", status.successContainer, status.onSuccessContainer)
    }
}
