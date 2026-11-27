package pe.kerolabs.pozzo

import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoTheme
import pe.kerolabs.pozzo.features.iam.domain.ThemePreference
import pe.kerolabs.pozzo.features.notifications.infrastructure.push.PushNotifications
import pe.kerolabs.pozzo.navigation.AppNavHost
import pe.kerolabs.pozzo.navigation.AppThemeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: AppThemeViewModel by viewModels()

    /** The screen a tapped notification asks to open, until the navigation takes it. */
    private val pendingDeepLink = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) takeDeepLink(intent)
        setContent {
            val navController = rememberNavController()
            val theme by themeViewModel.theme.collectAsStateWithLifecycle()
            val deepLink by pendingDeepLink.collectAsStateWithLifecycle()
            val dark = when (theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            PozzoTheme(darkTheme = dark) {
                AppNavHost(navController, deepLink = deepLink, onDeepLinkHandled = { pendingDeepLink.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeDeepLink(intent)
    }

    private fun takeDeepLink(intent: Intent?) {
        intent?.getStringExtra(PushNotifications.EXTRA_DEEP_LINK)?.let { pendingDeepLink.value = it }
        invitationCode(intent)?.let { pendingDeepLink.value = "pozzo://invitations/$it" }
    }

    /** The code of an invitation link the app was opened with, as kerolabs.github.io/unirme/?c=JB7K4M. */
    private fun invitationCode(intent: Intent?): String? {
        val data = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
        if (data.host != "kerolabs.github.io" || data.path?.startsWith("/unirme") != true) return null
        return data.getQueryParameter("c")?.filter(Char::isLetterOrDigit)?.uppercase()?.takeIf { it.isNotEmpty() }
    }
}
