package pe.kerolabs.pozzo.features.savingsgroups.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import pe.kerolabs.pozzo.core.format.formatMediumDate

/**
 * One turn of the collection order: its number, who collects and the cutoff date. The first turn is
 * highlighted; [trailing] holds an avatar or the controls to reorder.
 */
@Composable
fun TurnRow(
    turnNumber: Int,
    name: String,
    cutoffDate: LocalDate?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (highlighted) colors.primary else colors.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                turnNumber.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (highlighted) colors.onPrimary else colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            val detail = subtitle ?: cutoffDate?.let(::formatMediumDate)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        trailing()
    }
}
