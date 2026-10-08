package pe.kerolabs.pozzo.features.iam.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.ClearLocalContributionsUseCase
import pe.kerolabs.pozzo.features.iam.application.GetProfileUseCase
import pe.kerolabs.pozzo.features.iam.application.ObserveThemeUseCase
import pe.kerolabs.pozzo.features.iam.application.SignOutUseCase
import pe.kerolabs.pozzo.features.iam.application.UpdateBackupEmailUseCase
import pe.kerolabs.pozzo.features.iam.application.UpdateDisplayNameUseCase
import pe.kerolabs.pozzo.features.iam.application.UpdateThemeUseCase
import pe.kerolabs.pozzo.features.iam.application.UpdateWalletNumberUseCase
import pe.kerolabs.pozzo.features.iam.domain.Profile
import pe.kerolabs.pozzo.features.notifications.application.UnregisterDeviceUseCase
import pe.kerolabs.pozzo.features.iam.domain.ThemePreference
import pe.kerolabs.pozzo.features.savingsgroups.application.ClearLocalGroupsUseCase

data class ProfileUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

/** I1: the member's data and preferences, and the way out of the session. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfile: GetProfileUseCase,
    private val updateDisplayName: UpdateDisplayNameUseCase,
    private val updateWalletNumber: UpdateWalletNumberUseCase,
    private val updateBackupEmail: UpdateBackupEmailUseCase,
    private val signOut: SignOutUseCase,
    private val clearLocalGroups: ClearLocalGroupsUseCase,
    private val clearLocalContributions: ClearLocalContributionsUseCase,
    private val unregisterDevice: UnregisterDeviceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.profile == null, errorMessage = null) }
            getProfile()
                .onSuccess { profile -> _state.update { it.copy(profile = profile, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun rename(displayName: String) {
        if (displayName.isBlank()) return
        save { updateDisplayName(displayName) }
    }

    fun saveWalletNumber(digits: String) = save { updateWalletNumber(digits) }

    fun saveBackupEmail(email: String) = save { updateBackupEmail(email) }

    private fun save(update: suspend () -> Result<Profile>) {
        if (_state.value.profile == null || _state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            update()
                .onSuccess { updated -> _state.update { it.copy(profile = updated, isSaving = false) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // While the session is still open, so this phone stops receiving the pushes of the account.
            unregisterDevice()
            clearLocalGroups()
            clearLocalContributions()
            signOut.invoke()
        }
    }
}

/** I2: the visual theme, applied as soon as it is chosen. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeTheme: ObserveThemeUseCase,
    private val updateTheme: UpdateThemeUseCase,
) : ViewModel() {

    val theme: StateFlow<ThemePreference> =
        observeTheme().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference.SYSTEM)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun choose(theme: ThemePreference) {
        viewModelScope.launch {
            _errorMessage.value = null
            updateTheme(theme).onFailure { _errorMessage.value = it.userMessage() }
        }
    }
}
