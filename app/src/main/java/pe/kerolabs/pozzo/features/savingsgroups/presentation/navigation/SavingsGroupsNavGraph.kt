package pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupStatus
import pe.kerolabs.pozzo.features.savingsgroups.presentation.creategroup.CreateGroupScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.groupdetail.GroupDetailScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.invitation.InvitationScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup.JoinCodeScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup.JoinPreviewScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup.JoinedScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.mygroups.MyGroupsScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.turns.AgreedOrderScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.turns.AssignTurnsScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.turns.DrawResultScreen
import pe.kerolabs.pozzo.features.savingsgroups.presentation.turns.StartGroupScreen

@Serializable
data object SavingsGroupsNavGraphRoute

@Serializable
data object MyGroupsRoute

@Serializable
data object CreateGroupRoute

/**
 * @param fromDetail true when it was opened from the detail of the group, so "done" goes back there
 */
@Serializable
data class InvitationRoute(
    val groupId: String,
    val groupName: String,
    val code: String,
    val link: String,
    val fromDetail: Boolean = false,
)

@Serializable
data object JoinCodeRoute

@Serializable
data class JoinPreviewRoute(val code: String)

@Serializable
data class JoinedRoute(
    val groupId: String,
    val groupName: String,
    val organizerName: String,
    val membersCount: Int,
    val seats: Int,
)

@Serializable
data class GroupDetailRoute(val groupId: String)

@Serializable
data class AssignTurnsRoute(val groupId: String, val seats: Int)

@Serializable
data class DrawResultRoute(val groupId: String)

@Serializable
data class AgreedOrderRoute(val groupId: String)

@Serializable
data class StartGroupRoute(val groupId: String)

/**
 * The groups of the member: the list, creating and joining a group, its detail, its turns and its start.
 */
fun NavGraphBuilder.savingsGroupsNavGraph(navController: NavController, onOpenPot: (groupId: String) -> Unit) {

    val openDetail: (String) -> Unit = { groupId ->
        navController.navigate(GroupDetailRoute(groupId)) { popUpTo<MyGroupsRoute>() }
    }
    val backToDetail: () -> Unit = { navController.popBackStack<GroupDetailRoute>(inclusive = false) }

    navigation<SavingsGroupsNavGraphRoute>(startDestination = MyGroupsRoute) {

        composable<MyGroupsRoute> {
            MyGroupsScreen(
                onCreateGroup = { navController.navigate(CreateGroupRoute) },
                onJoinWithCode = { navController.navigate(JoinCodeRoute) },
                onOpenGroup = { group ->
                    if (group.status == GroupStatus.STARTED) onOpenPot(group.id) else navController.navigate(GroupDetailRoute(group.id))
                },
            )
        }

        composable<CreateGroupRoute> {
            CreateGroupScreen(
                onClose = { navController.popBackStack() },
                onCreated = { group, invitation ->
                    navController.navigate(InvitationRoute(group.id, group.name, invitation.code, invitation.link)) {
                        popUpTo<CreateGroupRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<InvitationRoute> { entry ->
            val route = entry.toRoute<InvitationRoute>()
            InvitationScreen(
                groupName = route.groupName,
                code = route.code,
                link = route.link,
                doneLabel = if (route.fromDetail) "Volver a la junta" else "Ir a mi junta",
                onDone = { if (route.fromDetail) navController.popBackStack() else openDetail(route.groupId) },
            )
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
                        JoinedRoute(preview.groupId, preview.name, preview.organizerName, preview.membersCount + 1, preview.seats),
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
                onDone = { openDetail(route.groupId) },
            )
        }

        composable<GroupDetailRoute> {
            GroupDetailScreen(
                onBack = { navController.popBackStack() },
                onAssignTurns = { groupId, seats -> navController.navigate(AssignTurnsRoute(groupId, seats)) },
                onStartGroup = { groupId -> navController.navigate(StartGroupRoute(groupId)) },
                onOpenPot = onOpenPot,
                onShowInvitation = { groupName, invitation ->
                    val groupId = it.toRoute<GroupDetailRoute>().groupId
                    navController.navigate(InvitationRoute(groupId, groupName, invitation.code, invitation.link, fromDetail = true))
                },
            )
        }

        composable<AssignTurnsRoute> { entry ->
            val route = entry.toRoute<AssignTurnsRoute>()
            AssignTurnsScreen(
                seats = route.seats,
                onBack = { navController.popBackStack() },
                onDraw = { navController.navigate(DrawResultRoute(route.groupId)) },
                onAgreedOrder = { navController.navigate(AgreedOrderRoute(route.groupId)) },
            )
        }

        composable<DrawResultRoute> {
            DrawResultScreen(onBack = { navController.popBackStack() }, onConfirm = backToDetail)
        }

        composable<AgreedOrderRoute> {
            AgreedOrderScreen(onBack = { navController.popBackStack() }, onSaved = backToDetail)
        }

        composable<StartGroupRoute> {
            StartGroupScreen(onBack = { navController.popBackStack() }, onStarted = backToDetail)
        }
    }
}
