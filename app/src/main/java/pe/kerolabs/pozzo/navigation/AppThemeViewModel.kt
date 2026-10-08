package pe.kerolabs.pozzo.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pe.kerolabs.pozzo.features.iam.application.ObserveThemeUseCase
import pe.kerolabs.pozzo.features.iam.domain.ThemePreference

/**
 * ViewModel responsible for managing application-wide UI theme preferences (I2).
 *
 * Exposes a reactive [ThemePreference] stream observed by the root theme provider
 * to toggle between Light, Dark, or System default modes across all screens.
 *
 * @param observeTheme Use case observing persistent theme choices from DataStore.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(observeTheme: ObserveThemeUseCase) : ViewModel() {
    val theme: StateFlow<ThemePreference> =
        observeTheme().stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)
}
