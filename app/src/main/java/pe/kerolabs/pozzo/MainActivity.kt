package pe.kerolabs.pozzo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoTheme
import pe.kerolabs.pozzo.features.iam.domain.ThemePreference
import pe.kerolabs.pozzo.navigation.AppNavHost
import pe.kerolabs.pozzo.navigation.AppThemeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: AppThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val theme by themeViewModel.theme.collectAsStateWithLifecycle()
            val dark = when (theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            PozzoTheme(darkTheme = dark) {
                AppNavHost(navController)
            }
        }
    }
}
