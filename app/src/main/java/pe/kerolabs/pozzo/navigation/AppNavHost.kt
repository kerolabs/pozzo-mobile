package pe.kerolabs.pozzo.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlin.reflect.KClass
import pe.kerolabs.pozzo.core.designsystem.components.PozzoLogo
import pe.kerolabs.pozzo.features.compliancehistory.presentation.navigation.HistoryRoute
import pe.kerolabs.pozzo.features.compliancehistory.presentation.navigation.complianceHistoryNavGraph
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.MyContributionsRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.PotRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.contributionsNavGraph
import pe.kerolabs.pozzo.features.iam.presentation.navigation.IamNavGraphRoute
import pe.kerolabs.pozzo.features.iam.presentation.navigation.ProfileRoute
import pe.kerolabs.pozzo.features.iam.presentation.navigation.iamNavGraph
import pe.kerolabs.pozzo.features.iam.presentation.navigation.profileNavGraph
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.GroupDetailRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.JoinCodeRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.MyGroupsRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.SavingsGroupsNavGraphRoute
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.savingsGroupsNavGraph

/** The destinations of the bottom bar, shown only on their root screens. */
private enum class TopDestination(
    val label: String,
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    GROUPS("Juntas", MyGroupsRoute, MyGroupsRoute::class, Icons.Outlined.Groups, Icons.Filled.Groups),
    HISTORY("Historial", HistoryRoute, HistoryRoute::class, Icons.Outlined.History, Icons.Filled.History),
    PROFILE("Perfil", ProfileRoute, ProfileRoute::class, Icons.Outlined.Person, Icons.Filled.Person),
}

private fun NavDestination?.topDestination(): TopDestination? =
    this?.let { destination -> TopDestination.entries.firstOrNull { destination.hasRoute(it.routeClass) } }

/** Switches destination keeping the state of each one, as the bottom bar of Material does. */
private fun NavController.navigateToTop(destination: TopDestination) = navigate(destination.route) {
    popUpTo<MyGroupsRoute> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination.topDestination()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (current != null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TopDestination.entries.forEach { destination ->
                        val selected = destination == current
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (!selected) navController.navigateToTop(destination) },
                            icon = { Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null) },
                            label = { Text(destination.label, style = MaterialTheme.typography.labelLarge) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            // The bar already covers the system navigation bar, so the screens above it must not pad it again.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            iamNavGraph(navController) { joinAfterSignIn ->
                navController.navigate(SavingsGroupsNavGraphRoute) { popUpTo(0) }
                if (joinAfterSignIn) navController.navigate(JoinCodeRoute)
            }
            savingsGroupsNavGraph(
                navController,
                onOpenPot = { navController.navigate(PotRoute(it)) },
                onOpenProfile = { navController.navigateToTop(TopDestination.PROFILE) },
            )
            contributionsNavGraph(
                navController,
                onGroupDetail = { navController.navigate(GroupDetailRoute(it)) { launchSingleTop = true } },
                onHome = { navController.navigate(MyGroupsRoute) { popUpTo<MyGroupsRoute> { inclusive = true } } },
            )
            complianceHistoryNavGraph(navController, onGroupContributions = { navController.navigate(MyContributionsRoute(it)) })
            profileNavGraph(navController)
        }
    }
}
