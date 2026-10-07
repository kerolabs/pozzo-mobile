package pe.kerolabs.pozzo.features.iam.presentation.code

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.application.RequestCodeUseCase
import pe.kerolabs.pozzo.features.iam.application.VerifyCodeUseCase
import pe.kerolabs.pozzo.features.iam.domain.Verification
import pe.kerolabs.pozzo.features.iam.presentation.navigation.CodeRoute

data class CodeUiState(
    val phone: String,
    val code: String = "",
    val secondsToResend: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val verification: Verification? = null,
) {
    val canVerify: Boolean get() = code.length == CODE_LENGTH && !isLoading
    val canResend: Boolean get() = secondsToResend <= 0 && !isLoading

    companion object {
        const val CODE_LENGTH = 6
    }
}

@HiltViewModel
class CodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyCode: VerifyCodeUseCase,
    private val requestCode: RequestCodeUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<CodeRoute>()

    private val _state = MutableStateFlow(CodeUiState(phone = route.phone))
    val state: StateFlow<CodeUiState> = _state.asStateFlow()

    private var countdown: Job? = null

    init {
        startCountdown(Instant.ofEpochMilli(route.resendAvailableAtMillis))
    }

    fun onCodeChange(value: String) {
        val digits = value.filter(Char::isDigit).take(CodeUiState.CODE_LENGTH)
        _state.update { it.copy(code = digits, errorMessage = null) }
        if (digits.length == CodeUiState.CODE_LENGTH) verify()
    }

    fun verify() {
        val current = _state.value
        if (!current.canVerify) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            verifyCode(current.phone, current.code)
                .onSuccess { result -> _state.update { it.copy(isLoading = false, verification = result) } }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, code = "", errorMessage = error.userMessage()) }
                }
        }
    }

    fun resend() {
        if (!_state.value.canResend) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, code = "") }
            requestCode(_state.value.phone)
                .onSuccess { request ->
                    _state.update { it.copy(isLoading = false) }
                    startCountdown(request.resendAvailableAt)
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onVerificationHandled() {
        _state.update { it.copy(verification = null) }
    }

    private fun startCountdown(until: Instant) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            while (true) {
                val seconds = Duration.between(Instant.now(), until).seconds.coerceAtLeast(0)
                _state.update { it.copy(secondsToResend = seconds) }
                if (seconds == 0L) break
                delay(1_000)
            }
        }
    }
}
