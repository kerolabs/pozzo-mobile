package pe.kerolabs.pozzo.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import pe.kerolabs.pozzo.features.compliancehistory.presentation.navigation.MyHistoryRoute
import pe.kerolabs.pozzo.features.compliancehistory.presentation.navigation.complianceHistoryNavGraph
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.MyContributionsRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.PotRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.ReviewsRoute
import pe.kerolabs.pozzo.features.contributions.presentation.navigation.contributionsNavGraph
import pe.kerolabs.pozzo.features.iam.presentation.navigation.IamNavGraphRoute
import pe.kerolabs.pozzo.features.iam.presentation.navigation.ProfileRoute
import pe.kerolabs.pozzo.features.iam.presentation.navigation.iamNavGraph
import pe.kerolabs.pozzo.features.iam.presentation.navigation.profileNavGraph
import pe.kerolabs.pozzo.features.notifications.presentation.PushViewModel
import pe.kerolabs.pozzo.features.notifications.presentation.navigation.NoticesRoute
import pe.kerolabs.pozzo.features.notifications.presentation.navigation.notificationsNavGraph
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
    NOTICES("Avisos", NoticesRoute, NoticesRoute::class, Icons.Outlined.Notifications, Icons.Filled.Notifications),
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

/**
 * Opens the screen a notice points to: "pozzo://groups/{id}" is the pot of the group,
 * "pozzo://groups/{id}/detail" its members and turns before it starts, "pozzo://groups/{id}/reviews" the
 * receipts to review, "pozzo://groups" the list of groups and "pozzo://compliance" the member's history.
 */
private fun NavController.openDeepLink(deepLink: String) {
    val parts = deepLink.removePrefix("pozzo://").split('/').filter { it.isNotEmpty() }
    when {
        parts.size == 3 && parts[0] == "groups" && parts[2] == "reviews" -> navigate(ReviewsRoute(parts[1]))
        parts.size == 3 && parts[0] == "groups" && parts[2] == "detail" ->
            navigate(GroupDetailRoute(parts[1])) { launchSingleTop = true }
        parts.size == 1 && parts[0] == "groups" -> navigate(MyGroupsRoute) { popUpTo<MyGroupsRoute> { inclusive = true } }
        parts.size == 2 && parts[0] == "groups" -> navigate(PotRoute(parts[1]))
        parts.firstOrNull() == "compliance" -> navigate(MyHistoryRoute)
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    sessionViewModel: SessionViewModel = hiltViewModel(),
    pushViewModel: PushViewModel = hiltViewModel(),
) {
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
        if (session == SessionStatus.SignedIn) pushViewModel.onSignedIn() else pushViewModel.onSignedOut()
    }

    if (session == SessionStatus.SignedIn) AskForNotificationPermission()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination.topDestination()
    val unseen by pushViewModel.unseen.collectAsStateWithLifecycle()

    // The dot of Avisos is counted again whenever the member moves between destinations or comes back.
    LaunchedEffect(current) { if (session == SessionStatus.SignedIn && current != null) pushViewModel.refreshUnseen() }
    LifecycleResumeEffect(session) {
        if (session == SessionStatus.SignedIn) pushViewModel.refreshUnseen()
        onPauseOrDispose { }
    }

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
                            icon = {
                                BadgedBox(badge = {
                                    if (destination == TopDestination.NOTICES && unseen > 0 && !selected) Badge()
                                }) {
                                    Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null)
                                }
                            },
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
            notificationsNavGraph(onOpenDeepLink = { navController.openDeepLink(it) })
            profileNavGraph(navController)
        }
    }

    // A tapped notification opens its screen once there is a session to show it.
    LaunchedEffect(deepLink, session) {
        if (deepLink != null && session == SessionStatus.SignedIn) {
            navController.openDeepLink(deepLink)
            onDeepLinkHandled()
        }
    }
}

/** Android 13 and later ask the member before showing notifications; it is asked once per launch. */
@Composable
private fun AskForNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
