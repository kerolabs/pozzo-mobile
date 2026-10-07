package pe.kerolabs.pozzo.features.savingsgroups.presentation.turns

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.savingsgroups.domain.cutoffDateOfTurn
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.ChecklistItem
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.ReadinessChecklist
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.TurnRow
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.periodicityTitle
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.turnMethodLabel

private val DrawMoment = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.forLanguageTag("es-PE"))

/**
 * E4: the result of the draw, with the moment it was made; it can be repeated until the group starts.
 */
@Composable
fun DrawResultScreen(onBack: () -> Unit, onConfirm: () -> Unit, viewModel: DrawResultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { PozzoTopBar(title = "Resultado del sorteo", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            val calendar = state.calendar
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when {
                    calendar == null && state.isDrawing -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    calendar == null -> Text(
                        state.errorMessage ?: "No se pudo hacer el sorteo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> {
                        val moment = calendar.assignedAt?.atZone(ZoneId.systemDefault())?.let(DrawMoment::format)?.replace(".", "")
                        InfoBanner(
                            text = "Sorteo realizado${moment?.let { " el $it" }.orEmpty()}, entre ${calendar.turns.size} integrantes.",
                            icon = Icons.Outlined.Shuffle,
                        )
                        Spacer(Modifier.height(8.dp))
                        calendar.turns.forEach { turn ->
                            TurnRow(
                                turnNumber = turn.turnNumber,
                                name = turn.displayName,
                                cutoffDate = turn.cutoffDate,
                                highlighted = turn.turnNumber == 1,
                                trailing = { InitialsAvatar(name = turn.displayName, size = 40.dp) },
                            )
                        }
                        state.errorMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PozzoPrimaryButton(text = "Confirmar turnos", onClick = onConfirm, enabled = calendar != null && !state.isDrawing)
            Spacer(Modifier.height(8.dp))
            PozzoOutlinedButton(
                text = "Repetir el sorteo",
                onClick = viewModel::draw,
                enabled = !state.isDrawing,
                icon = Icons.Outlined.Refresh,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * E5: the organizer holds a member and drags them to their turn.
 */
@Composable
fun AgreedOrderScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: AgreedOrderViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rowHeight = 72.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(topBar = { PozzoTopBar(title = "Ordenar turnos", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            InfoBanner(text = "Mantén presionado y arrastra para cambiar el orden de cobro.", icon = Icons.Outlined.Info)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState(), enabled = dragging == null)) {
                val group = state.detail?.group
                state.order.forEachIndexed { index, member ->
                    key(member.membershipId) {
                        val isDragged = dragging == member.membershipId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                                .then(
                                    if (isDragged) {
                                        Modifier
                                            .shadow(6.dp, MaterialTheme.shapes.large)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
                                    } else {
                                        Modifier
                                    },
                                )
                                .pointerInput(member.membershipId) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragging = member.membershipId
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            dragging = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            dragging = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val current = state.order.indexOfFirst { it.membershipId == member.membershipId }
                                            if (dragOffset > rowHeightPx / 2 && current < state.order.lastIndex) {
                                                viewModel.move(current, current + 1)
                                                dragOffset -= rowHeightPx
                                            } else if (dragOffset < -rowHeightPx / 2 && current > 0) {
                                                viewModel.move(current, current - 1)
                                                dragOffset += rowHeightPx
                                            }
                                        },
                                    )
                                }
                                .padding(horizontal = if (isDragged) 12.dp else 0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            TurnRow(
                                turnNumber = index + 1,
                                name = member.displayName,
                                cutoffDate = group?.cutoffDateOfTurn(index + 1),
                                subtitle = if (isDragged) "Moviendo…" else null,
                                trailing = {
                                    Icon(
                                        Icons.Outlined.DragHandle,
                                        contentDescription = "Arrastrar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
                if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))
            PozzoPrimaryButton(
                text = "Confirmar orden",
                onClick = viewModel::confirm,
                enabled = state.order.isNotEmpty(),
                loading = state.isSaving,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * E6: what is about to start and who collects first.
 */
@Composable
fun StartGroupScreen(
    onBack: () -> Unit,
    onStarted: () -> Unit,
    viewModel: StartGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors

    LaunchedEffect(state.started) { if (state.started) onStarted() }

    Scaffold(topBar = { PozzoTopBar(title = "Iniciar junta", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            val detail = state.detail
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (detail == null) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                } else {
                    val group = detail.group
                    ReadinessChecklist(
                        items = listOf(
                            ChecklistItem(group.readiness.groupFull, "Grupo completo", "${detail.activeMembers} de ${group.seats} integrantes"),
                            ChecklistItem(group.readiness.turnsAssigned, "Turnos definidos", turnMethodLabel(group.turnMethod)),
                            ChecklistItem(
                                group.readiness.destinationDefined,
                                "Reglas fijadas",
                                "${formatSoles(group.contributionAmount)} · ${periodicityTitle(group.periodicity)} · ${group.seats} integrantes",
                            ),
                        ),
                    )
                    detail.turns.turns.firstOrNull()?.let { first ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.primaryContainer, MaterialTheme.shapes.extraLarge)
                                .padding(20.dp),
                        ) {
                            Text("Primer período", style = MaterialTheme.typography.titleSmall, color = colors.onPrimaryContainer)
                            Text("Cobra ${first.displayName}", style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer)
                            Text(
                                "Corte: ${formatMediumDate(first.cutoffDate)} · Pozo ${formatSoles(group.potAmount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onPrimaryContainer,
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth().background(status.warningContainer, MaterialTheme.shapes.large).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = status.onWarningContainer)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Al iniciar, el aporte, la periodicidad y el número de integrantes ya no se pueden cambiar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = status.onWarningContainer,
                            )
                        }
                    }
                }
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                }
            }
            Spacer(Modifier.height(12.dp))
            PozzoPrimaryButton(
                text = "Iniciar junta",
                onClick = viewModel::start,
                enabled = detail?.group?.readiness?.canStart == true,
                loading = state.isStarting,
            )
            PozzoTextButton(text = "Revisar las reglas", onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
