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

/** The theme the member chose (I2), applied to the whole app. */
@HiltViewModel
class AppThemeViewModel @Inject constructor(observeTheme: ObserveThemeUseCase) : ViewModel() {
    val theme: StateFlow<ThemePreference> =
        observeTheme().stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)
}
