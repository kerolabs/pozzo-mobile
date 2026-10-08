package pe.kerolabs.pozzo.features.notifications.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.notifications.presentation.NoticesScreen

@Serializable
data object NoticesRoute

/** The Avisos destination. [onOpenDeepLink] opens the screen a notice points to, in another context. */
fun NavGraphBuilder.notificationsNavGraph(onOpenDeepLink: (String) -> Unit) {
    composable<NoticesRoute> {
        NoticesScreen(onOpen = onOpenDeepLink)
    }
}
