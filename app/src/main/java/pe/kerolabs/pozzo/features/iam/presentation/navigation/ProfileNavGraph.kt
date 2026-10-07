package pe.kerolabs.pozzo.features.iam.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.iam.presentation.profile.ProfileScreen
import pe.kerolabs.pozzo.features.iam.presentation.profile.ThemeScreen

@Serializable
data object ProfileRoute

@Serializable
data object ThemeRoute

/** I1 and I2, outside the access flow: they need a session. */
fun NavGraphBuilder.profileNavGraph(navController: NavController) {
    composable<ProfileRoute> {
        ProfileScreen(onTheme = { navController.navigate(ThemeRoute) })
    }
    composable<ThemeRoute> {
        ThemeScreen(onBack = { navController.popBackStack() })
    }
}
