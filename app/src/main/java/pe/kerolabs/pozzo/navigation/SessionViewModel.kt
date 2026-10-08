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
 * Whether there is a session decides where the app starts and where it goes when the session ends.
 */
sealed interface SessionStatus {
    data object Unknown : SessionStatus
    data object SignedIn : SessionStatus
    data object SignedOut : SessionStatus
}

@HiltViewModel
class SessionViewModel @Inject constructor(observeSession: ObserveSessionUseCase) : ViewModel() {

    val status: StateFlow<SessionStatus> = observeSession()
        .map { signedIn -> if (signedIn) SessionStatus.SignedIn else SessionStatus.SignedOut }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionStatus.Unknown)
}
