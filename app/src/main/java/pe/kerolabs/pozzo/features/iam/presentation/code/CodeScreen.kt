package pe.kerolabs.pozzo.features.iam.presentation.code

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.iam.domain.Verification
import pe.kerolabs.pozzo.features.iam.presentation.common.CodeInput
import pe.kerolabs.pozzo.features.iam.presentation.common.ResendCode

/**
 * A3: the six-digit code. A correct code opens the session, or leads to the registration for a new number.
 */
@Composable
fun CodeScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onRegistrationRequired: (Verification.RegistrationRequired) -> Unit,
    viewModel: CodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.verification) {
        when (val result = state.verification) {
            is Verification.SignedIn -> {
                viewModel.onVerificationHandled()
                onSignedIn()
            }
            is Verification.RegistrationRequired -> {
                viewModel.onVerificationHandled()
                onRegistrationRequired(result)
            }
            null -> Unit
        }
    }

    Scaffold(topBar = { PozzoTopBar(title = "Verificar número", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Ingresa el código", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Enviamos un código de 6 dígitos por SMS al ${PhoneNumbers.display(state.phone)}.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            PozzoTextButton(
                text = "Cambiar número",
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.weight(1f))
            PozzoPrimaryButton(
                text = "Verificar",
                onClick = viewModel::verify,
                enabled = state.canVerify,
                loading = state.isLoading,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

