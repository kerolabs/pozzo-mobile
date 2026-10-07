package pe.kerolabs.pozzo.features.iam.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers

/**
 * A4: a new number completes its profile with the name the group will see.
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status = PozzoThemeExtras.statusColors

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }

    Scaffold(topBar = { PozzoTopBar(title = "Tu perfil", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Casi listo", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Solo necesitamos tu nombre para que tu grupo te reconozca.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(status.successContainer, MaterialTheme.shapes.large)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = status.onSuccessContainer)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Número verificado: ${PhoneNumbers.display(state.phone)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = status.onSuccessContainer,
                )
            }
            Spacer(Modifier.height(24.dp))
            PozzoTextField(
                label = "Tu nombre",
                value = state.displayName,
                onValueChange = viewModel::onNameChange,
                placeholder = "Anna Weber",
                errorText = state.errorMessage,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    value = state.termsAccepted,
                    role = Role.Checkbox,
                    onValueChange = viewModel::onTermsChange,
                ),
            ) {
                Checkbox(checked = state.termsAccepted, onCheckedChange = null)
                Text(
                    "Acepto los Términos y Condiciones y la Política de Privacidad de Pozzo.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(Modifier.weight(1f))
            PozzoPrimaryButton(
                text = "Empezar",
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                loading = state.isLoading,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
