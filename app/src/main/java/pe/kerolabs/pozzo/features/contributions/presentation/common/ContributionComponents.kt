package pe.kerolabs.pozzo.features.contributions.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import pe.kerolabs.pozzo.core.designsystem.components.FieldLabel
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.format.formatLongDate
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState

/**
 * The state of a contribution with its fixed color, icon and word, as the style guide defines:
 * validated in green, in review in orange, pending in neutral or orange when it is one's own.
 */
@Composable
fun ContributionStateChip(state: ContributionState, isMine: Boolean = false, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    when (state) {
        ContributionState.VALIDATED ->
            StatusChip("Validado", status.successContainer, status.onSuccessContainer, modifier, Icons.Outlined.Check)
        ContributionState.IN_REVIEW ->
            StatusChip("En revisión", status.warningContainer, status.onWarningContainer, modifier, Icons.Outlined.HourglassTop)
        ContributionState.PENDING ->
            if (isMine) StatusChip("Pendiente", status.warningContainer, status.onWarningContainer, modifier, Icons.Outlined.Schedule)
            else StatusChip("Pendiente", colors.surfaceContainerHighest, colors.onSurfaceVariant, modifier, Icons.Outlined.Schedule)
        ContributionState.LATE ->
            StatusChip("Atrasado", colors.errorContainer, colors.onErrorContainer, modifier, Icons.Outlined.WarningAmber)
        ContributionState.COVERED ->
            StatusChip("Cubierto", colors.surfaceContainerHighest, colors.onSurfaceVariant, modifier, Icons.Outlined.VolunteerActivism)
    }
}

/** "Monto distinto", "Destinatario" or "Fecha", for a field of the receipt that did not match. */
fun inconsistencyLabel(field: String): String = when (field) {
    "AMOUNT" -> "Monto distinto"
    "PAYEE" -> "Destinatario"
    "DATE" -> "Fuera de fecha"
    else -> field
}

/** "Monto", "Enviado a" or "Fecha", for the comparison table. */
fun fieldLabel(field: String): String = when (field) {
    "AMOUNT" -> "Monto"
    "PAYEE" -> "Enviado a"
    "DATE" -> "Fecha"
    else -> field
}

@Composable
fun LoadingOrError(isLoading: Boolean, errorMessage: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    errorMessage ?: "No pudimos cargar la información.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PozzoOutlinedButton(text = "Reintentar", onClick = onRetry)
            }
        }
    }
}

/** A read-only date field that opens the date picker; dates after today cannot be chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: LocalDate, onValueChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    Column(modifier) {
        FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedTextField(
                value = formatLongDate(value),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            )
            Box(Modifier.matchParentSize().clickable { picking = true })
        }
    }
    if (picking) {
        val today = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    picking = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        ) { DatePicker(state = picker) }
    }
}

/** A member to choose with a radio mark, for the cash and coverage forms. */
@Composable
fun MemberOption(
    name: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    isMe: Boolean = false,
    enabled: Boolean = true,
    photoUrl: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(name = name, size = 40.dp, photoUrl = photoUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (isMe) "$name (tú)" else name, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(
            if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
