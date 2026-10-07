package pe.kerolabs.pozzo.features.iam.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.iam.presentation.code.CodeScreen
import pe.kerolabs.pozzo.features.iam.presentation.phone.PhoneScreen
import pe.kerolabs.pozzo.features.iam.presentation.recovery.RecoveryScreen
import pe.kerolabs.pozzo.features.iam.presentation.register.RegisterScreen
import pe.kerolabs.pozzo.features.iam.presentation.welcome.WelcomeScreen

@Serializable
data object IamNavGraphRoute

@Serializable
data object WelcomeRoute

/**
 * @param joinAfterSignIn true when the member came with an invitation code, so the app asks for it next
 */
@Serializable
data class PhoneRoute(val joinAfterSignIn: Boolean = false)

@Serializable
data class CodeRoute(val phone: String, val resendAvailableAtMillis: Long, val joinAfterSignIn: Boolean = false)

@Serializable
data object RecoveryRoute

@Serializable
data class RegisterRoute(val phone: String, val registrationToken: String, val joinAfterSignIn: Boolean = false)

/**
 * Access to Pozzo: welcome, phone number, code and, for a new number, the profile; or the recovery of an
 * account whose number was lost.
 *
 * @param onSignedIn called once a session is open, with whether to ask for an invitation code next;
 *   the caller leaves this graph
 */
fun NavGraphBuilder.iamNavGraph(navController: NavController, onSignedIn: (joinAfterSignIn: Boolean) -> Unit) {

    navigation<IamNavGraphRoute>(startDestination = WelcomeRoute) {

        composable<WelcomeRoute> {
            WelcomeScreen(
                onContinueWithPhone = { navController.navigate(PhoneRoute()) },
                onHaveInvitationCode = { navController.navigate(PhoneRoute(joinAfterSignIn = true)) },
            )
        }

        composable<PhoneRoute> { entry ->
            val join = entry.toRoute<PhoneRoute>().joinAfterSignIn
            PhoneScreen(
                onBack = { navController.popBackStack() },
                onCodeSent = { request ->
                    navController.navigate(
                        CodeRoute(request.phoneNumber, request.resendAvailableAt.toEpochMilli(), join),
                    )
                },
                onLostNumber = { navController.navigate(RecoveryRoute) },
            )
        }

        composable<CodeRoute> { entry ->
            val join = entry.toRoute<CodeRoute>().joinAfterSignIn
            CodeScreen(
                onBack = { navController.popBackStack() },
                onSignedIn = { onSignedIn(join) },
                onRegistrationRequired = { result ->
                    navController.navigate(RegisterRoute(result.phoneNumber, result.registrationToken, join)) {
                        popUpTo<PhoneRoute>()
                    }
                },
            )
        }

        composable<RecoveryRoute> {
            RecoveryScreen(onBack = { navController.popBackStack() }, onSignedIn = { onSignedIn(false) })
        }

        composable<RegisterRoute> { entry ->
            val join = entry.toRoute<RegisterRoute>().joinAfterSignIn
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = { onSignedIn(join) },
            )
        }
    }
}
