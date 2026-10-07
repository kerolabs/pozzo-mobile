package pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.ResultBadge
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras

/**
 * D3: the member joined; the organizer will assign the turns before starting.
 */
@Composable
fun JoinedScreen(groupName: String, organizerName: String, membersCount: Int, seats: Int, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            ResultBadge(icon = Icons.Filled.CheckCircle)
            Spacer(Modifier.height(24.dp))
            Text("Ya eres parte de la junta", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "${organizerName.substringBefore(' ')} asignará los turnos antes de iniciar. Te avisaremos cuando la junta empiece.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
                    .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(name = groupName, background = status.successContainer, content = status.onSuccessContainer)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(groupName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Participante · $membersCount de $seats integrantes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.weight(1.5f))
            PozzoPrimaryButton(text = "Ver mi junta", onClick = onDone)
        }
    }
}
