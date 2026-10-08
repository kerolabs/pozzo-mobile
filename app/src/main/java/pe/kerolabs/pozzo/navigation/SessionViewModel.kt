package pe.kerolabs.pozzo.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.kerolabs.pozzo.features.iam.application.ObserveSessionUseCase

/**
 * Represents the current user authentication session state.
 *
 * Drives top-level navigation graphs: directs authenticated users to main app features
 * and unauthenticated users to the welcome and authentication flows.
 */
sealed interface SessionStatus {
    /** Initial state while stored session credentials are being evaluated. */
    data object Unknown : SessionStatus

    /** Valid session active with stored user credentials and auth token. */
    data object SignedIn : SessionStatus

    /** No active session, or existing credentials have expired/been revoked. */
    data object SignedOut : SessionStatus
}

/**
 * ViewModel managing application-level authentication lifecycle and session status.
 *
 * Observes token and identity state via [ObserveSessionUseCase] to broadcast
 * [SessionStatus] to the root navigation graph.
 *
 * @property observeSession Use case providing a reactive stream of the signed-in state.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(observeSession: ObserveSessionUseCase) : ViewModel() {

    val status: StateFlow<SessionStatus> = observeSession()
        .map { signedIn -> if (signedIn) SessionStatus.SignedIn else SessionStatus.SignedOut }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionStatus.Unknown)
}
