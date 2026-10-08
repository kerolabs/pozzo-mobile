package pe.kerolabs.pozzo.features.contributions.presentation.pot

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.format.formatShortDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.PeriodState
import pe.kerolabs.pozzo.features.contributions.presentation.common.ContributionStateChip
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.rememberMemberPhotos

/**
 * F1: the pot of the period in progress, the same for the organizer and the participants, with the
 * state of every member's contribution. The actions depend on the role and on what is missing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotScreen(
    onBack: () -> Unit,
    onContribute: (groupId: String) -> Unit,
    onReviews: (groupId: String) -> Unit,
    onCash: (groupId: String) -> Unit,
    onCover: (groupId: String) -> Unit,
    onDeliver: (groupId: String) -> Unit,
    onGroupDetail: (groupId: String) -> Unit,
    onCalendar: (groupId: String) -> Unit,
    viewModel: PotViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    val cycle = state.cycle
    val period = state.period
    val groupId = viewModel.groupId
    val photos = rememberMemberPhotos(groupId)

    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        cycle?.groupName.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = { onCalendar(groupId) }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendario de turnos")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Detalle de la junta") }, onClick = {
                                menuOpen = false
                                onGroupDetail(groupId)
                            })
                            if (cycle?.isOrganizer == true && period?.state == PeriodState.OPEN) {
                                DropdownMenuItem(text = { Text("Registrar aporte en efectivo") }, onClick = {
                                    menuOpen = false
                                    onCash(groupId)
                                })
                                DropdownMenuItem(text = { Text("Cubrir un aporte") }, onClick = {
                                    menuOpen = false
                                    onCover(groupId)
                                })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (cycle != null && period != null) {
                PotActions(
                    cycle = cycle,
                    period = period,
                    onContribute = { onContribute(groupId) },
                    onReviews = { onReviews(groupId) },
                    onDeliver = { onDeliver(groupId) },
                )
            }
        },
    ) { padding ->
        if (cycle == null || period == null) {
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
                PotCard(period)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Estado de los aportes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${period.settledCount} de ${period.membersCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            items(period.members, key = { it.membershipId }) { member ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(name = member.displayName, size = 40.dp, photoUrl = photos[member.membershipId])
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (member.isMe) "${member.displayName} (tú)" else member.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (member.collects) {
                            Text("Cobra en este período", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ContributionStateChip(member.state, isMine = member.isMe)
                }
            }
        }
    }
}

@Composable
private fun PotCard(period: Period) {
    val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
    ) {
        Text(
            "Período ${period.turnNumber} de ${period.totalTurns} · Cobra ${period.payoutMemberName}",
            style = MaterialTheme.typography.titleSmall,
            color = onBrand,
        )
        Text(formatSoles(period.collectedAmount), style = MaterialTheme.typography.displayMedium, color = onBrand)
        Text("de ${formatSoles(period.potAmount)} reunidos", style = MaterialTheme.typography.bodyLarge, color = onBrand)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { period.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = onBrand,
            trackColor = onBrand.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(10.dp))
        Row {
            val missing = when (period.state) {
                PeriodState.OPEN -> "Faltan ${formatSoles(period.missingAmount)}"
                PeriodState.POT_COMPLETE -> "Pozo completo"
                PeriodState.DELIVERED -> "Pozo entregado"
            }
            Text(missing, style = MaterialTheme.typography.labelLarge, color = onBrand, modifier = Modifier.weight(1f))
            val days = when {
                period.daysToCutoff > 1 -> "faltan ${period.daysToCutoff} días"
                period.daysToCutoff == 1L -> "falta 1 día"
                period.daysToCutoff == 0L -> "es hoy"
                else -> "venció"
            }
            Text("Corte ${formatShortDate(period.cutoffDate)} · $days", style = MaterialTheme.typography.labelLarge, color = onBrand)
        }
    }
}

@Composable
private fun PotActions(cycle: Cycle, period: Period, onContribute: () -> Unit, onReviews: () -> Unit, onDeliver: () -> Unit) {
    val mine = period.myState
    val canContribute = period.state == PeriodState.OPEN &&
        (mine == ContributionState.PENDING || mine == ContributionState.LATE)
    val reviews = cycle.isOrganizer && period.inReviewCount > 0
    val deliver = cycle.isOrganizer && period.state == PeriodState.POT_COMPLETE
    if (!canContribute && !reviews && !deliver) return
    Column(
        Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (deliver) PozzoPrimaryButton(text = "Entregar el pozo a ${period.payoutMemberName.substringBefore(' ')}", onClick = onDeliver)
        if (reviews) {
            PozzoOutlinedButton(text = "Revisar aportes (${period.inReviewCount})", onClick = onReviews, icon = Icons.Outlined.FactCheck)
        }
        if (canContribute) {
            PozzoPrimaryButton(
                text = "Registrar mi aporte de ${formatSoles(period.contributionAmount)}",
                onClick = onContribute,
                icon = Icons.Outlined.Upload,
            )
        }
    }
}
