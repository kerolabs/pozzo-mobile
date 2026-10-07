package pe.kerolabs.pozzo.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import pe.kerolabs.pozzo.core.designsystem.components.PozzoLogo
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.PotRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.contributionsNavGraph
import pe.kerolabs.pozzo.features.iam.presentation.navigation.IamNavGraphRoute
import pe.kerolabs.pozzo.features.iam.presentation.navigation.iamNavGraph
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.GroupDetailRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.JoinCodeRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.MyGroupsRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.SavingsGroupsNavGraphRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.savingsGroupsNavGraph

@Composable
fun AppNavHost(navController: NavHostController, sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.status.collectAsStateWithLifecycle()

    if (session == SessionStatus.Unknown) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            PozzoLogo()
        }
        return
    }

    // Decided once, when the session is first known; later changes navigate instead of rebuilding the graph.
    val startDestination: Any = remember {
        if (session == SessionStatus.SignedIn) SavingsGroupsNavGraphRoute else IamNavGraphRoute
    }

    // When the session ends (sign out, or a token the backend no longer accepts) the app goes back to the welcome.
    LaunchedEffect(session) {
        val inAccessFlow = navController.currentBackStackEntry?.destination?.hierarchy
            ?.any { it.hasRoute<IamNavGraphRoute>() } == true
        if (session == SessionStatus.SignedOut && !inAccessFlow) {
            navController.navigate(IamNavGraphRoute) { popUpTo(0) }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        iamNavGraph(navController) { joinAfterSignIn ->
            navController.navigate(SavingsGroupsNavGraphRoute) { popUpTo(0) }
            if (joinAfterSignIn) navController.navigate(JoinCodeRoute)
        }
        savingsGroupsNavGraph(navController, onOpenPot = { navController.navigate(PotRoute(it)) })
        contributionsNavGraph(
            navController,
            onGroupDetail = { navController.navigate(GroupDetailRoute(it)) { launchSingleTop = true } },
            onHome = { navController.navigate(MyGroupsRoute) { popUpTo<MyGroupsRoute> { inclusive = true } } },
        )
    }
}
