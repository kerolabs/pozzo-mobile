package pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.savingsgroups.presentation.mygroups.MyGroupsScreen

@Serializable
data object SavingsGroupsNavGraphRoute

@Serializable
data object MyGroupsRoute

/**
 * The groups of the member. Creating, joining and the detail of a group come next.
 *
 * @param onNotAvailableYet shows a message for the actions not built yet
 */
fun NavGraphBuilder.savingsGroupsNavGraph(navController: NavController, onNotAvailableYet: (String) -> Unit) {

    navigation<SavingsGroupsNavGraphRoute>(startDestination = MyGroupsRoute) {

        composable<MyGroupsRoute> {
            MyGroupsScreen(
                onCreateGroup = { onNotAvailableYet("Crear junta") },
                onJoinWithCode = { onNotAvailableYet("Unirme con un código") },
                onOpenGroup = { onNotAvailableYet("Detalle de la junta") },
            )
        }
    }
}
