package pe.kerolabs.pozzo.features.savingsgroups.presentation.turns

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.features.savingsgroups.domain.TurnMethod

/**
 * E3: how the collection order is decided. Auctions are shown but not available yet.
 */
@Composable
fun AssignTurnsScreen(
    seats: Int,
    onBack: () -> Unit,
    onDraw: () -> Unit,
    onAgreedOrder: () -> Unit,
) {
    var method by rememberSaveable { mutableStateOf(TurnMethod.DRAW) }
    Scaffold(topBar = { PozzoTopBar(title = "Asignar turnos", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("¿Cómo se reparte el orden de cobro?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Elige el método que ya acordó tu grupo. Los turnos quedan visibles para todos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MethodCard(
                icon = Icons.Outlined.Shuffle,
                title = "Sorteo",
                description = "El orden se define al azar entre los $seats integrantes.",
                selected = method == TurnMethod.DRAW,
                onSelect = { method = TurnMethod.DRAW },
            )
            MethodCard(
                icon = Icons.Outlined.FormatListNumbered,
                title = "Orden acordado",
                description = "Tú ordenas los turnos según lo conversado con el grupo.",
                selected = method == TurnMethod.AGREED,
                onSelect = { method = TurnMethod.AGREED },
            )
            MethodCard(
                icon = Icons.Outlined.Gavel,
                title = "Subasta",
                description = "Cada turno se asigna a quien ofrece el mayor descuento.",
                selected = false,
                onSelect = {},
                enabled = false,
            )
            Spacer(Modifier.weight(1f))
            PozzoPrimaryButton(
                text = "Continuar",
                onClick = { if (method == TurnMethod.DRAW) onDraw() else onAgreedOrder() },
            )
            Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
private fun MethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.surfaceContainerLow)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) colors.primary else colors.outlineVariant,
                MaterialTheme.shapes.extraLarge,
            )
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.7f),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (selected) colors.primaryContainer else colors.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (!enabled) {
                    Spacer(Modifier.width(8.dp))
                    StatusChip("Próximamente", colors.surfaceContainerHighest, colors.onSurfaceVariant)
                }
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = null, enabled = enabled)
    }
}
