package pe.kerolabs.pozzo.features.compliancehistory.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.compliancehistory.presentation.HistoryScreen
import pe.kerolabs.pozzo.features.compliancehistory.presentation.MyHistoryScreen

@Serializable
data object HistoryRoute

@Serializable
data object MyHistoryRoute

/**
 * The History destination. [onGroupContributions] opens the member's contributions in a group (G1),
 * which belong to the contributions context.
 */
fun NavGraphBuilder.complianceHistoryNavGraph(navController: NavController, onGroupContributions: (groupId: String) -> Unit) {
    composable<HistoryRoute> {
        HistoryScreen(
            onMyHistory = { navController.navigate(MyHistoryRoute) },
            onGroupContributions = onGroupContributions,
        )
    }
    composable<MyHistoryRoute> {
        MyHistoryScreen(onBack = { navController.popBackStack() }, onGroupContributions = onGroupContributions)
    }
}
