package pe.kerolabs.pozzo.features.iam.presentation.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.application.RecoverAccountUseCase
import pe.kerolabs.pozzo.features.iam.application.RequestRecoveryCodeUseCase
import pe.kerolabs.pozzo.features.iam.application.RequestRecoveryPhoneCodeUseCase
import pe.kerolabs.pozzo.features.iam.application.VerifyRecoveryCodeUseCase
import pe.kerolabs.pozzo.features.iam.application.isEmail
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.iam.presentation.common.ResendCountdown
import pe.kerolabs.pozzo.features.iam.presentation.common.VERIFICATION_CODE_LENGTH

/** The backup email, its code, the new number and its SMS code. */
enum class RecoveryStep { EMAIL, EMAIL_CODE, PHONE, PHONE_CODE }

data class RecoveryUiState(
    val step: RecoveryStep = RecoveryStep.EMAIL,
    val email: String = "",
    val phone: String = "",
    val code: String = "",
    val recoveryToken: String? = null,
    val secondsToResend: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedIn: Boolean = false,
) {
    val canSendEmail: Boolean get() = isEmail(email) && !isLoading
    val canSendPhone: Boolean get() = PhoneNumbers.isValid(phone) && !isLoading
    val canVerify: Boolean get() = code.length == VERIFICATION_CODE_LENGTH && !isLoading
    val canResend: Boolean get() = secondsToResend <= 0 && !isLoading
}

/**
 * Recovery of an account whose phone number was lost: a code to the backup email proves the account,
 * and an SMS code to the new number links it. The groups and the history stay with the account.
 */
@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val requestRecoveryCode: RequestRecoveryCodeUseCase,
    private val verifyRecoveryCode: VerifyRecoveryCodeUseCase,
    private val requestRecoveryPhoneCode: RequestRecoveryPhoneCodeUseCase,
    private val recoverAccount: RecoverAccountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RecoveryUiState())
    val state: StateFlow<RecoveryUiState> = _state.asStateFlow()

    private val countdown = ResendCountdown(viewModelScope) { seconds -> _state.update { it.copy(secondsToResend = seconds) } }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value.trim().take(120), errorMessage = null) }

    fun onPhoneChange(value: String) =
        _state.update { it.copy(phone = value.filter(Char::isDigit).take(9), errorMessage = null) }

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value, errorMessage = null) }
        if (value.length == VERIFICATION_CODE_LENGTH) verify()
    }

    fun sendEmailCode() {
        val current = _state.value
        if (!current.canSendEmail) return
        launchStep {
            requestRecoveryCode(current.email).onSuccess { request ->
                countdown.start(request.resendAvailableAt)
                _state.update { it.copy(step = RecoveryStep.EMAIL_CODE, code = "") }
            }
        }
    }

    fun sendPhoneCode() {
        val current = _state.value
        val token = current.recoveryToken ?: return
        if (!current.canSendPhone) {
            _state.update { it.copy(errorMessage = "Ingresa un celular de nueve dígitos que empiece con 9.") }
            return
        }
        launchStep {
            requestRecoveryPhoneCode(token, current.phone).onSuccess { request ->
                countdown.start(request.resendAvailableAt)
                _state.update { it.copy(step = RecoveryStep.PHONE_CODE, code = "") }
            }
        }
    }

    fun resend() {
        if (!_state.value.canResend) return
        when (_state.value.step) {
            RecoveryStep.EMAIL_CODE -> sendEmailCode()
            RecoveryStep.PHONE_CODE -> sendPhoneCode()
            else -> Unit
        }
    }

    fun verify() {
        val current = _state.value
        if (!current.canVerify) return
        when (current.step) {
            RecoveryStep.EMAIL_CODE -> launchStep(clearCodeOnError = true) {
                verifyRecoveryCode(current.email, current.code).onSuccess { token ->
                    _state.update { it.copy(step = RecoveryStep.PHONE, recoveryToken = token, code = "", secondsToResend = 0) }
                }
            }
            RecoveryStep.PHONE_CODE -> launchStep(clearCodeOnError = true) {
                recoverAccount(current.recoveryToken.orEmpty(), current.phone, current.code)
                    .onSuccess { _state.update { it.copy(signedIn = true) } }
            }
            else -> Unit
        }
    }

    /** Goes back one step; returns false on the first one, where back leaves the recovery. */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            RecoveryStep.EMAIL -> return false
            RecoveryStep.EMAIL_CODE -> RecoveryStep.EMAIL
            // The email was already proved: going back only changes the new number.
            RecoveryStep.PHONE -> return false
            RecoveryStep.PHONE_CODE -> RecoveryStep.PHONE
        }
        _state.update { it.copy(step = previous, code = "", errorMessage = null) }
        return true
    }

    private fun launchStep(clearCodeOnError: Boolean = false, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = block()
            _state.update { state ->
                val error = result.exceptionOrNull()
                state.copy(
                    isLoading = false,
                    errorMessage = error?.userMessage(),
                    code = if (error != null && clearCodeOnError) "" else state.code,
                )
            }
        }
    }
}
