package pe.kerolabs.pozzo.features.iam.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.application.ChangePhoneNumberUseCase
import pe.kerolabs.pozzo.features.iam.application.RequestPhoneChangeCodeUseCase
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.iam.presentation.common.CodeInput
import pe.kerolabs.pozzo.features.iam.presentation.common.ResendCode
import pe.kerolabs.pozzo.features.iam.presentation.common.ResendCountdown
import pe.kerolabs.pozzo.features.iam.presentation.common.VERIFICATION_CODE_LENGTH

data class ChangePhoneUiState(
    val phone: String = "",
    val code: String = "",
    val codeSent: Boolean = false,
    val secondsToResend: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val changed: Boolean = false,
) {
    val canSend: Boolean get() = PhoneNumbers.isValid(phone) && !isLoading
    val canVerify: Boolean get() = code.length == VERIFICATION_CODE_LENGTH && !isLoading
    val canResend: Boolean get() = secondsToResend <= 0 && !isLoading
}

/** The member switches the account to a new number, proved with an SMS code. Groups and history stay. */
@HiltViewModel
class ChangePhoneViewModel @Inject constructor(
    private val requestPhoneChangeCode: RequestPhoneChangeCodeUseCase,
    private val changePhoneNumber: ChangePhoneNumberUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePhoneUiState())
    val state: StateFlow<ChangePhoneUiState> = _state.asStateFlow()

    private val countdown = ResendCountdown(viewModelScope) { seconds -> _state.update { it.copy(secondsToResend = seconds) } }

    fun onPhoneChange(value: String) =
        _state.update { it.copy(phone = value.filter(Char::isDigit).take(9), errorMessage = null) }

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value, errorMessage = null) }
        if (value.length == VERIFICATION_CODE_LENGTH) verify()
    }

    fun sendCode() {
        val current = _state.value
        if (!current.canSend && !current.codeSent) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, code = "") }
            requestPhoneChangeCode(current.phone)
                .onSuccess { request ->
                    countdown.start(request.resendAvailableAt)
                    _state.update { it.copy(isLoading = false, codeSent = true) }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun verify() {
        val current = _state.value
        if (!current.canVerify) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            changePhoneNumber(current.phone, current.code)
                .onSuccess { _state.update { it.copy(isLoading = false, changed = true) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, code = "", errorMessage = error.userMessage()) } }
        }
    }

    /** Back from the code returns to the number; returns false when there is nothing to go back to. */
    fun back(): Boolean {
        if (!_state.value.codeSent) return false
        _state.update { it.copy(codeSent = false, code = "", errorMessage = null) }
        return true
    }
}

@Composable
fun ChangePhoneScreen(onBack: () -> Unit, onChanged: () -> Unit, viewModel: ChangePhoneViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { if (!viewModel.back()) onBack() }
    LaunchedEffect(state.changed) { if (state.changed) onChanged() }

    Scaffold(topBar = { PozzoTopBar(title = "Cambiar número", onBack = { if (!viewModel.back()) onBack() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (!state.codeSent) {
                Text("Tu número nuevo", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Te enviaremos un código por SMS al número nuevo. Tus juntas, aportes e historial se mantienen.",
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
                Spacer(Modifier.weight(1f))
                PozzoPrimaryButton(text = "Enviar código", onClick = viewModel::sendCode, enabled = state.canSend, loading = state.isLoading)
            } else {
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
                    onResend = viewModel::sendCode,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.weight(1f))
                PozzoPrimaryButton(text = "Cambiar número", onClick = viewModel::verify, enabled = state.canVerify, loading = state.isLoading)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
