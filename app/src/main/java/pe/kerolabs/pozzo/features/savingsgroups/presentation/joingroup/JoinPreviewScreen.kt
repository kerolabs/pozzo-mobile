package pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupPreview
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.cutoffLabel
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.periodicityTitle

/**
 * D2: the group behind the code, before joining. It never shows the members nor the destination.
 */
@Composable
fun JoinPreviewScreen(
    onBack: () -> Unit,
    onJoined: (GroupPreview) -> Unit,
    viewModel: JoinPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.joined) {
        val preview = state.preview
        if (state.joined && preview != null) {
            viewModel.onJoinedHandled()
            onJoined(preview)
        }
    }

    Scaffold(topBar = { PozzoTopBar(title = "Revisar la junta", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val preview = state.preview
            when {
                state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                preview == null -> Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Text(
                        state.errorMessage ?: "No pudimos cargar la junta.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    PozzoOutlinedButton(text = "Reintentar", onClick = viewModel::load)
                }
                else -> Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PreviewCard(preview)
                    InfoBanner(
                        text = "Pozzo no maneja el dinero. Los aportes se transfieren directamente a ${preview.organizerName.substringBefore(' ')}.",
                        icon = Icons.Outlined.Info,
                    )
                    state.errorMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (preview != null) {
                Spacer(Modifier.height(12.dp))
                PozzoPrimaryButton(
                    text = "Unirme a la junta",
                    onClick = viewModel::join,
                    enabled = preview.freeSeats > 0,
                    loading = state.isJoining,
                )
                Spacer(Modifier.height(8.dp))
                PozzoTextButton(text = "Ahora no", onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun PreviewCard(preview: GroupPreview) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(preview.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (preview.freeSeats > 0) {
                StatusChip("Por iniciar", status.warningContainer, status.onWarningContainer)
            } else {
                StatusChip("Completa", colors.surfaceContainerHighest, colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(name = preview.organizerName)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(preview.organizerName, style = MaterialTheme.typography.titleMedium)
                Text("Cabeza de junta", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(vertical = 16.dp))
        Row {
            Metric("Aporte", formatSoles(preview.contributionAmount), Modifier.weight(1f))
            Metric("Periodicidad", periodicityTitle(preview.periodicity), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Metric("Integrantes", "${preview.membersCount} de ${preview.seats}", Modifier.weight(1f))
            Metric("Pozo por turno", formatSoles(preview.potAmount), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Detail("Fecha de corte", cutoffLabel(preview.periodicity, preview.firstContributionDate))
        Spacer(Modifier.height(8.dp))
        Detail("Primer aporte", formatMediumDate(preview.firstContributionDate))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
