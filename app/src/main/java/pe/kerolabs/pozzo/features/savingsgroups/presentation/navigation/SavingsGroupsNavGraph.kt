package pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.savingsgroups.presentation.creategroup.CreateGroupScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.invitation.InvitationScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup.JoinCodeScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup.JoinPreviewScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup.JoinedScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.mygroups.MyGroupsScreen

@Serializable
data object SavingsGroupsNavGraphRoute

@Serializable
data object MyGroupsRoute

@Serializable
data object CreateGroupRoute

@Serializable
data class InvitationRoute(val groupName: String, val code: String, val link: String)

@Serializable
data object JoinCodeRoute

@Serializable
data class JoinPreviewRoute(val code: String)

@Serializable
data class JoinedRoute(val groupName: String, val organizerName: String, val membersCount: Int, val seats: Int)

/**
 * The groups of the member: the list, creating a group with its invitation, and joining one with a code.
 *
 * @param onNotAvailableYet shows a message for the actions not built yet
 */
fun NavGraphBuilder.savingsGroupsNavGraph(navController: NavController, onNotAvailableYet: (String) -> Unit) {

    val backToMyGroups: () -> Unit = {
        navController.navigate(MyGroupsRoute) { popUpTo<MyGroupsRoute> { inclusive = true } }
    }

    navigation<SavingsGroupsNavGraphRoute>(startDestination = MyGroupsRoute) {

        composable<MyGroupsRoute> {
            MyGroupsScreen(
                onCreateGroup = { navController.navigate(CreateGroupRoute) },
                onJoinWithCode = { navController.navigate(JoinCodeRoute) },
                onOpenGroup = { onNotAvailableYet("Detalle de la junta") },
            )
        }

        composable<CreateGroupRoute> {
            CreateGroupScreen(
                onClose = { navController.popBackStack() },
                onCreated = { group, invitation ->
                    navController.navigate(InvitationRoute(group.name, invitation.code, invitation.link)) {
                        popUpTo<CreateGroupRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<InvitationRoute> { entry ->
            val route = entry.toRoute<InvitationRoute>()
            InvitationScreen(groupName = route.groupName, code = route.code, link = route.link, onDone = backToMyGroups)
        }

        composable<JoinCodeRoute> {
            JoinCodeScreen(
                onBack = { navController.popBackStack() },
                onGroupFound = { code -> navController.navigate(JoinPreviewRoute(code)) },
            )
        }

        composable<JoinPreviewRoute> {
            JoinPreviewScreen(
                onBack = { navController.popBackStack() },
                onJoined = { preview ->
                    navController.navigate(
                        JoinedRoute(preview.name, preview.organizerName, preview.membersCount + 1, preview.seats),
                    ) { popUpTo<JoinCodeRoute> { inclusive = true } }
                },
            )
        }

        composable<JoinedRoute> { entry ->
            val route = entry.toRoute<JoinedRoute>()
            JoinedScreen(
                groupName = route.groupName,
                organizerName = route.organizerName,
                membersCount = route.membersCount,
                seats = route.seats,
                onDone = backToMyGroups,
            )
        }
    }
}
