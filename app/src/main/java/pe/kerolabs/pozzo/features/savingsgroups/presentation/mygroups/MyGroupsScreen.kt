package pe.kerolabs.pozzo.features.savingsgroups.presentation.mygroups

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatShortDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupStatus
import pe.kerolabs.pozzo.features.savingsgroups.domain.Periodicity
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup

/**
 * B1, B2 and B3: the groups of the member, as organizer or participant, or the empty state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsScreen(
    onCreateGroup: () -> Unit,
    onJoinWithCode: () -> Unit,
    onOpenGroup: (SavingsGroup) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: MyGroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.groups.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onCreateGroup,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Crear junta", style = MaterialTheme.typography.titleMedium) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && state.loadedOnce,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Header(
                        firstName = state.firstName,
                        displayName = state.displayName,
                        photoUrl = state.photoUrl,
                        onOpenProfile = onOpenProfile,
                    )
                }
                if (state.showEmptyState) {
                    item { EmptyState(onCreateGroup, onJoinWithCode) }
                } else {
                    items(state.groups, key = { it.id }) { group ->
                        GroupCard(group = group, onClick = { onOpenGroup(group) })
                    }
                    if (state.groups.isNotEmpty()) {
                        item {
                            PozzoOutlinedButton(
                                text = "Unirme con un código",
                                onClick = onJoinWithCode,
                                icon = Icons.Outlined.Key,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(firstName: String, displayName: String, photoUrl: String?, onOpenProfile: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            if (firstName.isNotBlank()) {
                Text(
                    "Hola, $firstName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Mis juntas", style = MaterialTheme.typography.headlineLarge)
        }
        InitialsAvatar(
            name = displayName.ifBlank { "?" },
            modifier = Modifier.clip(CircleShape).clickable(onClickLabel = "Ver mi perfil", onClick = onOpenProfile),
            size = 56.dp,
            background = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            photoUrl = photoUrl,
        )
    }
}

@Composable
private fun GroupCard(group: SavingsGroup, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(group.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (group.isOrganizer) {
                StatusChip(
                    "Cabeza de junta",
                    container = colors.secondaryContainer,
                    content = colors.onSecondaryContainer,
                    icon = Icons.Outlined.StarOutline,
                )
            } else {
                StatusChip("Participante", container = colors.surfaceContainerHighest, content = colors.onSurfaceVariant)
            }
            when (group.status) {
                GroupStatus.STARTED -> StatusChip("En curso", status.successContainer, status.onSuccessContainer)
                GroupStatus.CLOSED -> StatusChip("Cerrada", colors.surfaceContainerHighest, colors.onSurfaceVariant)
                else -> StatusChip("Por iniciar", status.warningContainer, status.onWarningContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text("Aporte ${periodicityLabel(group.periodicity)}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(formatSoles(group.contributionAmount), style = MaterialTheme.typography.headlineSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Pozo por turno", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(formatSoles(group.potAmount), style = MaterialTheme.typography.headlineSmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(cardSummary(group), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}

private fun periodicityLabel(periodicity: Periodicity) = when (periodicity) {
    Periodicity.WEEKLY -> "semanal"
    Periodicity.BIWEEKLY -> "quincenal"
    Periodicity.MONTHLY -> "mensual"
}

private fun cardSummary(group: SavingsGroup): String = when (group.status) {
    GroupStatus.STARTED, GroupStatus.CLOSED -> {
        val turn = group.myTurnNumber
        val date = group.myTurnDate
        if (turn != null && date != null) "Te toca cobrar en el turno $turn, el ${formatShortDate(date)}."
        else "${group.seats} integrantes."
    }
    else -> when {
        !group.readiness.groupFull -> "${group.seats} cupos. Faltan integrantes para iniciar."
        !group.readiness.turnsAssigned -> "Grupo completo. Falta definir los turnos para iniciar."
        !group.readiness.destinationDefined -> "Falta definir a dónde se envían los aportes."
        else -> "Todo listo para iniciar."
    }
}

@Composable
private fun EmptyState(onCreateGroup: () -> Unit, onJoinWithCode: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(160.dp).background(colors.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Groups, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(72.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text("Aún no tienes juntas", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Crea una junta para tu grupo o únete con el código que te compartió tu cabeza de junta.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PozzoPrimaryButton(text = "Crear una junta", onClick = onCreateGroup, icon = Icons.Outlined.Add)
        Spacer(Modifier.height(12.dp))
        PozzoOutlinedButton(text = "Unirme con un código", onClick = onJoinWithCode, icon = Icons.Outlined.Key)
    }
}
