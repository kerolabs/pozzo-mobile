package pe.kerolabs.pozzo.features.iam.presentation.phone

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
import pe.kerolabs.pozzo.features.iam.application.RequestCodeUseCase
import pe.kerolabs.pozzo.features.iam.domain.CodeRequest
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers

/**
 * UI state for the phone authentication entry screen (A2).
 *
 * @property phone Current numeric phone input string entered by the user.
 * @property isLoading Indicates whether an SMS verification code request is in-flight.
 * @property errorMessage Feedback message explaining validation errors or network failures.
 * @property codeSent Details of the dispatched verification code request upon success.
 */
data class PhoneUiState(
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val codeSent: CodeRequest? = null,
) {
    val canSend: Boolean get() = PhoneNumbers.isValid(phone) && !isLoading
}

/**
 * ViewModel handling phone number validation and SMS verification code dispatch during login/registration.
 *
 * Validates Peru mobile phone number format rules and invokes [RequestCodeUseCase]
 * to dispatch the SMS challenge.
 *
 * @param requestCode Use case initiating OTP verification via SMS.
 */
@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val requestCode: RequestCodeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PhoneUiState())
    val state: StateFlow<PhoneUiState> = _state.asStateFlow()

    fun onPhoneChange(value: String) {
        val digits = value.filter(Char::isDigit).take(9)
        _state.update { it.copy(phone = digits, errorMessage = null) }
    }

    fun sendCode() {
        val phone = _state.value.phone
        if (!PhoneNumbers.isValid(phone)) {
            _state.update { it.copy(errorMessage = "Ingresa un celular de nueve dígitos que empiece con 9.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            requestCode(phone)
                .onSuccess { request -> _state.update { it.copy(isLoading = false, codeSent = request) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onNavigatedToCode() {
        _state.update { it.copy(codeSent = null) }
    }
}
