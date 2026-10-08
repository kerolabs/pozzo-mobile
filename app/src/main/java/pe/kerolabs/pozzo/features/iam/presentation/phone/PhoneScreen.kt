package pe.kerolabs.pozzo.features.iam.presentation.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.features.iam.domain.CodeRequest

/**
 * A2: the phone number that receives the code by SMS.
 */
@Composable
fun PhoneScreen(
    onBack: () -> Unit,
    onCodeSent: (CodeRequest) -> Unit,
    onLostNumber: () -> Unit,
    viewModel: PhoneViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.codeSent) {
        state.codeSent?.let {
            viewModel.onNavigatedToCode()
            onCodeSent(it)
        }
    }

    Scaffold(topBar = { PozzoTopBar(title = "Ingresar a Pozzo", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("¿Cuál es tu número de celular?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Te enviaremos un código por SMS para confirmar que el número es tuyo. No necesitas contraseña.",
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
                supportingText = "Solo usamos tu número para verificar tu identidad.",
                errorText = state.errorMessage,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            )
            Spacer(Modifier.weight(1f))
            PozzoTextButton(
                text = "¿Perdiste tu número? Recupera tu cuenta",
                onClick = onLostNumber,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            PozzoPrimaryButton(
                text = "Enviar código",
                onClick = viewModel::sendCode,
                enabled = state.canSend,
                loading = state.isLoading,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
