package pe.kerolabs.pozzo.features.iam.presentation.code

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.iam.domain.Verification

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
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }
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
            BasicTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.focusRequester(focus),
                decorationBox = { CodeBoxes(state.code, isError = state.errorMessage != null) },
            )
            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            if (state.secondsToResend > 0) {
                Text(
                    "Reenviar código en %d:%02d".format(state.secondsToResend / 60, state.secondsToResend % 60),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PozzoTextButton(
                    text = "Reenviar código",
                    onClick = viewModel::resend,
                    enabled = state.canResend,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
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

@Composable
private fun CodeBoxes(code: String, isError: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(CodeUiState.CODE_LENGTH) { index ->
            val focused = index == code.length
            val borderColor = when {
                isError -> MaterialTheme.colorScheme.error
                focused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f)
                    .border(if (focused) 2.dp else 1.dp, borderColor, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Text(code.getOrNull(index)?.toString().orEmpty(), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
