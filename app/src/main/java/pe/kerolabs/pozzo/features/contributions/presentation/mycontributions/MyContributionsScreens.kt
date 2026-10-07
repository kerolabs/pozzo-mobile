package pe.kerolabs.pozzo.features.contributions.presentation.mycontributions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import java.time.Instant
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.components.SummaryCard
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatShortDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.contributions.domain.ContributionMethod
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.domain.MyPeriodContribution
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptSource
import pe.kerolabs.pozzo.features.contributions.presentation.common.ContributionStateChip
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError

/**
 * G1: one row per period with the state of the member's contribution; a validated one opens its receipt.
 * Without connection it shows the last copy kept on the phone and says how old it is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyContributionsScreen(onBack: () -> Unit, viewModel: MyContributionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var receiptOf by remember { mutableStateOf<MyPeriodContribution?>(null) }
    val data = state.contributions

    Scaffold(topBar = { PozzoTopBar(title = "Mis aportes", onBack = onBack) }) { padding ->
        if (data == null) {
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
            if (data.isOffline) {
                item { InfoBanner("Sin conexión. Mostrando lo último guardado ${savedAgo(data.savedAt)}.", Icons.Outlined.Info) }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
                        .padding(20.dp),
                ) {
                    Text(data.groupName, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Amount("Aportado", formatSoles(data.contributedAmount), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        Amount(
                            "Pendiente",
                            formatSoles(data.pendingAmount),
                            if (data.pendingAmount.signum() > 0) PozzoThemeExtras.statusColors.onWarningContainer
                            else MaterialTheme.colorScheme.onSurface,
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            items(data.periods, key = { it.periodId }) { period ->
                PeriodRow(period, onOpen = { receiptOf = period })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                Text(
                    "Toca un aporte validado para ver su comprobante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    receiptOf?.let { period ->
        ModalBottomSheet(onDismissRequest = { receiptOf = null }) {
            ReceiptSheet(period)
        }
    }
}

@Composable
private fun Amount(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
    }
}

@Composable
private fun PeriodRow(period: MyPeriodContribution, onOpen: () -> Unit) {
    val status = PozzoThemeExtras.statusColors
    val colors = MaterialTheme.colorScheme
    val contribution = period.contribution
    val opensReceipt = contribution != null && period.state.let { it == ContributionState.VALIDATED || it == ContributionState.COVERED }
    val (icon, container, content) = when (period.state) {
        ContributionState.VALIDATED -> Triple(Icons.Outlined.ReceiptLong, status.successContainer, status.onSuccessContainer)
        ContributionState.IN_REVIEW -> Triple(Icons.Outlined.HourglassTop, status.warningContainer, status.onWarningContainer)
        ContributionState.PENDING -> Triple(Icons.Outlined.Schedule, status.warningContainer, status.onWarningContainer)
        ContributionState.LATE -> Triple(Icons.Outlined.WarningAmber, colors.errorContainer, colors.onErrorContainer)
        ContributionState.COVERED -> Triple(Icons.Outlined.VolunteerActivism, colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
    val detail = when {
        contribution?.receipt != null -> "${formatSoles(period.amount)} · ${formatMediumDate(contribution.receipt.paidAt)}"
        contribution != null && period.state.let { it == ContributionState.VALIDATED || it == ContributionState.COVERED } ->
            "${formatSoles(period.amount)} · ${methodLabel(contribution.method)}"
        else -> "${formatSoles(period.amount)} · vence ${formatShortDate(period.cutoffDate)}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (opensReceipt) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).background(container, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = content)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Período ${period.turnNumber}", style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        ContributionStateChip(period.state, isMine = true)
        if (opensReceipt) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ver comprobante")
    }
}

@Composable
private fun ReceiptSheet(period: MyPeriodContribution) {
    val contribution = period.contribution ?: return
    val receipt = contribution.receipt
    Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
        Text("Comprobante del período ${period.turnNumber}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Registrado el ${formatMediumDate(contribution.registeredAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        SummaryCard(
            rows = buildList {
                add("Monto" to formatSoles(contribution.amount))
                add("Forma" to methodLabel(contribution.method))
                if (receipt != null) {
                    add("Fecha del pago" to formatMediumDate(receipt.paidAt))
                    add("Enviado a" to receipt.payeeName)
                    add("N.º de operación" to receipt.operationNumber)
                    add("Aplicación" to sourceLabel(receipt.source))
                }
            },
        )
    }
}

private fun methodLabel(method: ContributionMethod) = when (method) {
    ContributionMethod.TRANSFER -> "Transferencia"
    ContributionMethod.CASH -> "Efectivo"
    ContributionMethod.COVERAGE -> "Cubierto por otro integrante"
}

private fun sourceLabel(source: ReceiptSource) = when (source) {
    ReceiptSource.YAPE -> "Yape"
    ReceiptSource.PLIN -> "Plin"
    ReceiptSource.BANK -> "Banco"
}

/** "hace 2 h", "hace 5 min" or "hace 3 días", for the offline notice. */
private fun savedAgo(savedAt: Instant): String {
    val elapsed = Duration.between(savedAt, Instant.now())
    return when {
        elapsed.toMinutes() < 1 -> "hace un momento"
        elapsed.toHours() < 1 -> "hace ${elapsed.toMinutes()} min"
        elapsed.toDays() < 1 -> "hace ${elapsed.toHours()} h"
        elapsed.toDays() == 1L -> "hace 1 día"
        else -> "hace ${elapsed.toDays()} días"
    }
}

/**
 * G2: one row per period with who collects and the cutoff date; the period in progress is highlighted.
 */
@Composable
fun TurnCalendarScreen(onBack: () -> Unit, viewModel: TurnCalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cycle = state.cycle
    val calendar = state.calendar
    Scaffold(topBar = { PozzoTopBar(title = "Calendario de turnos", onBack = onBack) }) { padding ->
        if (cycle == null || calendar == null) {
            LoadingOrError(state.isLoading, state.errorMessage, viewModel::load, Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    "${cycle.groupName} · ${cycle.totalTurns} períodos · Pozo ${formatSoles(cycle.potAmount)} por turno",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(calendar.turns, key = { it.turnNumber }) { turn ->
                val collected = !cycle.isActive || turn.turnNumber < cycle.currentTurn
                val current = cycle.isActive && turn.turnNumber == cycle.currentTurn
                TurnCalendarRow(
                    number = turn.turnNumber,
                    name = if (turn.isMe) "${turn.displayName} (tú)" else turn.displayName,
                    date = formatMediumDate(turn.cutoffDate),
                    kind = when {
                        collected -> TurnKind.COLLECTED
                        current -> TurnKind.CURRENT
                        turn.isMe -> TurnKind.MINE
                        else -> TurnKind.PENDING
                    },
                )
            }
        }
    }
}

private enum class TurnKind { COLLECTED, CURRENT, MINE, PENDING }

@Composable
private fun TurnCalendarRow(number: Int, name: String, date: String, kind: TurnKind) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    val highlighted = kind == TurnKind.CURRENT
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlighted) colors.secondaryContainer else Color.Transparent, MaterialTheme.shapes.large)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (badge, onBadge) = when (kind) {
            TurnKind.COLLECTED -> status.successContainer to status.onSuccessContainer
            TurnKind.CURRENT -> colors.primary to colors.onPrimary
            else -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        }
        Box(Modifier.size(36.dp).background(badge, CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = onBadge)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = if (highlighted) FontWeight.SemiBold else null)
            Text(date, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        when (kind) {
            TurnKind.COLLECTED -> StatusChip("Cobró", status.successContainer, status.onSuccessContainer)
            TurnKind.CURRENT -> Text("Vigente", style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer)
            TurnKind.MINE -> StatusChip("Tu turno", colors.tertiaryContainer, colors.onTertiaryContainer)
            TurnKind.PENDING -> StatusChip("Pendiente", colors.surfaceContainerHighest, colors.onSurfaceVariant)
        }
    }
}
