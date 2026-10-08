package pe.kerolabs.pozzo.features.iam.presentation.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.application.CompleteRegistrationUseCase
import pe.kerolabs.pozzo.features.iam.presentation.navigation.RegisterRoute

data class RegisterUiState(
    val phone: String,
    val displayName: String = "",
    val termsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registered: Boolean = false,
) {
    val canSubmit: Boolean get() = displayName.isNotBlank() && termsAccepted && !isLoading
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val completeRegistration: CompleteRegistrationUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RegisterRoute>()

    private val _state = MutableStateFlow(RegisterUiState(phone = route.phone))
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.update { it.copy(displayName = value.take(80), errorMessage = null) }
    }

    fun onTermsChange(accepted: Boolean) {
        _state.update { it.copy(termsAccepted = accepted) }
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            completeRegistration(route.registrationToken, current.displayName, current.termsAccepted)
                .onSuccess { _state.update { it.copy(isLoading = false, registered = true) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }
}
