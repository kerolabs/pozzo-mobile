package pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup

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
import pe.kerolabs.pozzo.features.savingsgroups.application.JoinGroupUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.PreviewInvitationUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupPreview
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.JoinPreviewRoute

data class JoinPreviewUiState(
    val preview: GroupPreview? = null,
    val isLoading: Boolean = true,
    val isJoining: Boolean = false,
    val errorMessage: String? = null,
    val joined: Boolean = false,
)

@HiltViewModel
class JoinPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val previewInvitation: PreviewInvitationUseCase,
    private val joinGroup: JoinGroupUseCase,
) : ViewModel() {

    private val code = savedStateHandle.toRoute<JoinPreviewRoute>().code

    private val _state = MutableStateFlow(JoinPreviewUiState())
    val state: StateFlow<JoinPreviewUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            previewInvitation(code)
                .onSuccess { preview -> _state.update { it.copy(isLoading = false, preview = preview) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun join() {
        if (_state.value.isJoining) return
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, errorMessage = null) }
            joinGroup(code)
                .onSuccess { _state.update { it.copy(isJoining = false, joined = true) } }
                .onFailure { error -> _state.update { it.copy(isJoining = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onJoinedHandled() = _state.update { it.copy(joined = false) }
}
