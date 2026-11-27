package pe.kerolabs.pozzo.features.savingsgroups.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupPlan

/** What each plan includes, as the business model of the report describes it. */
fun planTitle(plan: GroupPlan) = when (plan) {
    GroupPlan.FREE -> "Plan gratuito"
    GroupPlan.PAID -> "Plan de pago · ${formatSoles(GroupPlan.PAID_PRICE)} por junta"
}

fun planDescription(plan: GroupPlan) = when (plan) {
    GroupPlan.FREE -> "Una junta activa de hasta ${GroupPlan.FREE_MAX_SEATS} integrantes, con turnos por sorteo u orden acordado."
    GroupPlan.PAID -> "Más de ${GroupPlan.FREE_MAX_SEATS} integrantes, varias juntas a la vez y subasta de turnos. " +
        "Se paga una vez y cubre todo el ciclo."
}

/** The demonstration notice: the paid plan is activated without any real charge. */
const val PLAN_DEMO_NOTICE = "Versión de demostración: al activar el plan de pago no se hace ningún cobro."

/**
 * The two plans to choose from. When [freeUnavailableReason] is not null the free plan cannot be chosen and the
 * reason shows under it.
 */
@Composable
fun PlanChoice(selected: GroupPlan, freeUnavailableReason: String?, onSelect: (GroupPlan) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupPlan.entries.forEach { plan ->
            val enabled = plan == GroupPlan.PAID || freeUnavailableReason == null
            PlanOption(
                plan = plan,
                selected = selected == plan,
                enabled = enabled,
                note = if (!enabled) freeUnavailableReason else null,
                onClick = { onSelect(plan) },
            )
        }
        Text(PLAN_DEMO_NOTICE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlanOption(plan: GroupPlan, selected: Boolean, enabled: Boolean, note: String?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerLow, MaterialTheme.shapes.large)
            .border(1.dp, if (selected) colors.primary else colors.outlineVariant, MaterialTheme.shapes.large)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            val content = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f)
            Text(planTitle(plan), style = MaterialTheme.typography.titleMedium, color = content)
            Text(planDescription(plan), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
        }
    }
}

/**
 * Asks the organizer to move a group to the paid plan, because what they tried to do needs it ([reason]).
 */
@Composable
fun PaidPlanDialog(reason: String, activating: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!activating) onDismiss() },
        title = { Text("Activar el plan de pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(reason)
                Text(planDescription(GroupPlan.PAID))
                Text(PLAN_DEMO_NOTICE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !activating) {
                Text(if (activating) "Activando…" else "Activar por ${formatSoles(GroupPlan.PAID_PRICE)}")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !activating) { Text("Ahora no") } },
    )
}
