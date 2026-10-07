package pe.kerolabs.pozzo.features.savingsgroups.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras

data class ChecklistItem(val done: Boolean, val title: String, val detail: String)

/**
 * The conditions to start a group, each one with a check or an empty circle and a short explanation.
 */
@Composable
fun ReadinessChecklist(items: List<ChecklistItem>, modifier: Modifier = Modifier, title: String? = null) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (item.done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (item.done) "Cumplido" else "Pendiente",
                    tint = if (item.done) PozzoThemeExtras.statusColors.success else colors.outline,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
