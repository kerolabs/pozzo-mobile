package pe.kerolabs.pozzo.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras

/**
 * "Paso 1 de 3 · Reglas" with a segmented bar.
 */
@Composable
fun StepProgress(current: Int, total: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Paso $current de $total · $label",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One option of a single choice, such as "Mensual" or "Yape": outlined, or filled with a check when chosen.
 */
@Composable
fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.secondaryContainer else colors.surface)
            .border(1.dp, if (selected) colors.secondaryContainer else colors.outline, MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.onSecondaryContainer)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * A number with buttons to lower or raise it, such as the number of members.
 */
@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val buttonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = { onValueChange(value - 1) },
            enabled = value > range.first,
            colors = buttonColors,
            modifier = Modifier.size(48.dp),
        ) { Icon(Icons.Outlined.Remove, contentDescription = "Menos") }
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp),
        )
        FilledIconButton(
            onClick = { onValueChange(value + 1) },
            enabled = value < range.last,
            colors = buttonColors,
            modifier = Modifier.size(48.dp),
        ) { Icon(Icons.Outlined.Add, contentDescription = "Más") }
    }
}

/**
 * A bordered card with label and value rows separated by dividers, as in the summaries.
 */
@Composable
fun SummaryCard(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        rows.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
            if (index < rows.lastIndex) HorizontalDivider(color = colors.outlineVariant)
        }
    }
}

/**
 * A soft panel with an icon and a short message, e.g. "Pozzo no maneja el dinero".
 */
@Composable
fun InfoBanner(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceContainerHigh)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
    }
}

/**
 * The large circle with an icon at the top of a result screen.
 */
@Composable
fun ResultBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    val status = PozzoThemeExtras.statusColors
    Box(
        modifier = modifier.size(112.dp).background(status.successContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = status.onSuccessContainer, modifier = Modifier.size(48.dp))
    }
}

/**
 * The label above a field or a group of options.
 */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
