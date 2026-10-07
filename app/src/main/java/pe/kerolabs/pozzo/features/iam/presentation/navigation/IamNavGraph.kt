package pe.kerolabs.pozzo.features.iam.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import pe.kerolabs.pozzo.features.iam.presentation.code.CodeScreen
import pe.kerolabs.pozzo.features.iam.presentation.phone.PhoneScreen
import pe.kerolabs.pozzo.features.iam.presentation.register.RegisterScreen
import pe.kerolabs.pozzo.features.iam.presentation.welcome.WelcomeScreen

@Serializable
data object IamNavGraphRoute

@Serializable
data object WelcomeRoute

@Serializable
data object PhoneRoute

@Serializable
data class CodeRoute(val phone: String, val resendAvailableAtMillis: Long)

@Serializable
data class RegisterRoute(val phone: String, val registrationToken: String)

/**
 * Access to Pozzo: welcome, phone number, code and, for a new number, the profile.
 *
 * @param onSignedIn called once a session is open; the caller leaves this graph
 */
fun NavGraphBuilder.iamNavGraph(navController: NavController, onSignedIn: () -> Unit) {

    navigation<IamNavGraphRoute>(startDestination = WelcomeRoute) {

        composable<WelcomeRoute> {
            WelcomeScreen(
                onContinueWithPhone = { navController.navigate(PhoneRoute) },
                onHaveInvitationCode = { navController.navigate(PhoneRoute) },
            )
        }

        composable<PhoneRoute> {
            PhoneScreen(
                onBack = { navController.popBackStack() },
                onCodeSent = { request ->
                    navController.navigate(CodeRoute(request.phoneNumber, request.resendAvailableAt.toEpochMilli()))
                },
            )
        }

        composable<CodeRoute> {
            CodeScreen(
                onBack = { navController.popBackStack() },
                onSignedIn = onSignedIn,
                onRegistrationRequired = { result ->
                    navController.navigate(RegisterRoute(result.phoneNumber, result.registrationToken)) {
                        popUpTo<PhoneRoute>()
                    }
                },
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = onSignedIn,
            )
        }
    }
}
