package pe.kerolabs.pozzo.features.contributions.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.contributions.presentation.mycontributions.MyContributionsScreen
import pe.kerolabs.pozzo.features.contributions.presentation.mycontributions.TurnCalendarScreen
import pe.kerolabs.pozzo.features.contributions.presentation.organizer.CashScreen
import pe.kerolabs.pozzo.features.contributions.presentation.organizer.CoverScreen
import pe.kerolabs.pozzo.features.contributions.presentation.organizer.CycleClosedScreen
import pe.kerolabs.pozzo.features.contributions.presentation.organizer.DeliverScreen
import pe.kerolabs.pozzo.features.contributions.presentation.organizer.ReviewDetailScreen
import pe.kerolabs.pozzo.features.contributions.presentation.organizer.ReviewsScreen
import pe.kerolabs.pozzo.features.contributions.presentation.pot.PotScreen
import pe.kerolabs.pozzo.features.contributions.presentation.register.RegisterContributionScreen

@Serializable
data class PotRoute(val groupId: String)

@Serializable
data class RegisterContributionRoute(val groupId: String)

@Serializable
data class ReviewsRoute(val groupId: String)

@Serializable
data class ReviewDetailRoute(val groupId: String, val contributionId: String)

@Serializable
data class CashRoute(val groupId: String)

@Serializable
data class CoverRoute(val groupId: String)

@Serializable
data class DeliverRoute(val groupId: String)

@Serializable
data class MyContributionsRoute(val groupId: String)

@Serializable
data class TurnCalendarRoute(val groupId: String)

@Serializable
data class CycleClosedRoute(val groupName: String, val totalTurns: Int, val potAmount: String)

/**
 * The screens of a cycle in progress: the pot, the member's contribution and the organizer's tasks.
 * [onGroupDetail] opens the group in the savings groups context; [onHome] goes back to the list of groups.
 */
fun NavGraphBuilder.contributionsNavGraph(
    navController: NavController,
    onGroupDetail: (groupId: String) -> Unit,
    onHome: () -> Unit,
) {
    val backToPot: () -> Unit = { navController.popBackStack<PotRoute>(inclusive = false) }

    composable<PotRoute> {
        PotScreen(
            onBack = { navController.popBackStack() },
            onContribute = { navController.navigate(RegisterContributionRoute(it)) },
            onReviews = { navController.navigate(ReviewsRoute(it)) },
            onCash = { navController.navigate(CashRoute(it)) },
            onCover = { navController.navigate(CoverRoute(it)) },
            onDeliver = { navController.navigate(DeliverRoute(it)) },
            onGroupDetail = onGroupDetail,
            onCalendar = { navController.navigate(TurnCalendarRoute(it)) },
        )
    }

    composable<RegisterContributionRoute> { entry ->
        val groupId = entry.toRoute<RegisterContributionRoute>().groupId
        RegisterContributionScreen(
            onClose = { navController.popBackStack() },
            onMyContributions = {
                navController.navigate(MyContributionsRoute(groupId)) {
                    popUpTo<RegisterContributionRoute> { inclusive = true }
                }
            },
        )
    }

    composable<MyContributionsRoute> {
        MyContributionsScreen(onBack = { navController.popBackStack() })
    }

    composable<TurnCalendarRoute> {
        TurnCalendarScreen(onBack = { navController.popBackStack() })
    }

    composable<ReviewsRoute> { entry ->
        val groupId = entry.toRoute<ReviewsRoute>().groupId
        ReviewsScreen(
            onBack = { navController.popBackStack() },
            onOpen = { navController.navigate(ReviewDetailRoute(groupId, it)) },
        )
    }

    composable<ReviewDetailRoute> {
        ReviewDetailScreen(
            onBack = { navController.popBackStack() },
            onDecided = { wasLast -> if (wasLast) backToPot() else navController.popBackStack() },
        )
    }

    composable<CashRoute> {
        CashScreen(onBack = { navController.popBackStack() }, onDone = backToPot)
    }

    composable<CoverRoute> {
        CoverScreen(onBack = { navController.popBackStack() }, onDone = backToPot)
    }

    composable<DeliverRoute> {
        DeliverScreen(
            onBack = { navController.popBackStack() },
            onNextPeriod = backToPot,
            onCycleClosed = { groupName, totalTurns, potAmount ->
                navController.navigate(CycleClosedRoute(groupName, totalTurns, potAmount)) {
                    popUpTo<PotRoute> { inclusive = true }
                }
            },
        )
    }

    composable<CycleClosedRoute> { entry ->
        val route = entry.toRoute<CycleClosedRoute>()
        CycleClosedScreen(route.groupName, route.totalTurns, route.potAmount, onDone = onHome)
    }
}
