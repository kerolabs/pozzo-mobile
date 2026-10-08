package pe.kerolabs.pozzo.features.notifications.infrastructure.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.features.iam.application.ObserveSessionUseCase
import pe.kerolabs.pozzo.features.notifications.application.RegisterDeviceUseCase

/**
 * Receives the pushes of Firebase Cloud Messaging. A new token is registered in the backend right away
 * when there is a session; a push that arrives with the app open is shown here.
 */
@AndroidEntryPoint
class PozzoMessagingService : FirebaseMessagingService() {

    @Inject lateinit var observeSession: ObserveSessionUseCase

    @Inject lateinit var registerDevice: RegisterDeviceUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            if (observeSession().first()) registerDevice()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        PushNotifications.show(
            context = this,
            id = message.data["notificationId"] ?: message.messageId.orEmpty(),
            title = notification.title.orEmpty(),
            body = notification.body.orEmpty(),
            deepLink = message.data[PushNotifications.EXTRA_DEEP_LINK],
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
