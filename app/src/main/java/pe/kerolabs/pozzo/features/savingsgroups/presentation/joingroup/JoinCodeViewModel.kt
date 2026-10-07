package pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.ApiException
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.savingsgroups.application.PreviewInvitationUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.InvitationCodes

data class JoinCodeUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val foundCode: String? = null,
) {
    val canSearch: Boolean get() = InvitationCodes.isComplete(code) && !isLoading
}

@HiltViewModel
class JoinCodeViewModel @Inject constructor(
    private val previewInvitation: PreviewInvitationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(JoinCodeUiState())
    val state: StateFlow<JoinCodeUiState> = _state.asStateFlow()

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = InvitationCodes.normalize(value), errorMessage = null) }
    }

    fun search() {
        val code = _state.value.code
        if (!InvitationCodes.isComplete(code)) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            previewInvitation(code)
                .onSuccess { _state.update { it.copy(isLoading = false, foundCode = code) } }
                .onFailure { error ->
                    val message = if (error is ApiException && error.status == 404) {
                        "No encontramos una junta con ese código. Revisa que esté bien escrito o pide uno nuevo."
                    } else {
                        error.userMessage()
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }

    fun onFoundHandled() = _state.update { it.copy(foundCode = null) }
}
