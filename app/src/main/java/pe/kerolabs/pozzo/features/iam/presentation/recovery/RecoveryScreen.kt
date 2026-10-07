package pe.kerolabs.pozzo.features.iam.presentation.recovery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.iam.presentation.common.CodeInput
import pe.kerolabs.pozzo.features.iam.presentation.common.ResendCode

/**
 * "¿Perdiste tu número?": the member proves the account with the code sent to the backup email and
 * links a new number with an SMS code. The session opens at the end.
 */
@Composable
fun RecoveryScreen(onBack: () -> Unit, onSignedIn: () -> Unit, viewModel: RecoveryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { if (!viewModel.back()) onBack() }
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    Scaffold(topBar = { PozzoTopBar(title = "Recuperar mi cuenta", onBack = { if (!viewModel.back()) onBack() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            when (state.step) {
                RecoveryStep.EMAIL -> EmailStep(state, viewModel)
                RecoveryStep.EMAIL_CODE -> CodeStep(
                    title = "Revisa tu correo",
                    message = "Si ${state.email} es el correo de respaldo de una cuenta, te enviamos un código de 6 dígitos. " +
                        "Revisa también la carpeta de spam.",
                    state = state,
                    viewModel = viewModel,
                )
                RecoveryStep.PHONE -> PhoneStep(state, viewModel)
                RecoveryStep.PHONE_CODE -> CodeStep(
                    title = "Ingresa el código",
                    message = "Enviamos un código de 6 dígitos por SMS al ${PhoneNumbers.display(state.phone)}.",
                    state = state,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.EmailStep(state: RecoveryUiState, viewModel: RecoveryViewModel) {
    Text("¿Perdiste tu número?", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        "Ingresa el correo de respaldo de tu perfil. Te enviaremos un código para confirmar que la cuenta es tuya " +
            "y luego asociarás tu número nuevo. Tus juntas y tu historial se mantienen.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    PozzoTextField(
        label = "Correo de respaldo",
        value = state.email,
        onValueChange = viewModel::onEmailChange,
        placeholder = "anna@ejemplo.pe",
        errorText = state.errorMessage,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
    )
    Spacer(Modifier.weight(1f))
    PozzoPrimaryButton(
        text = "Enviar código",
        onClick = viewModel::sendEmailCode,
        enabled = state.canSendEmail,
        loading = state.isLoading,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ColumnScope.PhoneStep(state: RecoveryUiState, viewModel: RecoveryViewModel) {
    Text("Tu número nuevo", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        "Confirmamos que la cuenta es tuya. Ingresa el celular que usarás desde ahora; te enviaremos un código por SMS.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    PozzoTextField(
        label = "Número de celular",
        value = state.phone,
        onValueChange = viewModel::onPhoneChange,
        prefix = "+51",
        placeholder = "999 000 123",
        errorText = state.errorMessage,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
    )
    Spacer(Modifier.height(16.dp))
    InfoBanner("Al terminar se cerrará la sesión en el celular que perdiste.", Icons.Outlined.Info)
    Spacer(Modifier.weight(1f))
    PozzoPrimaryButton(
        text = "Enviar código",
        onClick = viewModel::sendPhoneCode,
        enabled = state.canSendPhone,
        loading = state.isLoading,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ColumnScope.CodeStep(title: String, message: String, state: RecoveryUiState, viewModel: RecoveryViewModel) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    CodeInput(state.code, viewModel::onCodeChange, isError = state.errorMessage != null)
    state.errorMessage?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(20.dp))
    ResendCode(
        secondsToResend = state.secondsToResend,
        enabled = state.canResend,
        onResend = viewModel::resend,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Spacer(Modifier.weight(1f))
    PozzoPrimaryButton(text = "Verificar", onClick = viewModel::verify, enabled = state.canVerify, loading = state.isLoading)
    Spacer(Modifier.height(8.dp))
}
