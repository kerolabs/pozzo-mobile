package pe.kerolabs.pozzo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import pe.kerolabs.pozzo.features.notifications.infrastructure.push.PushNotifications

@HiltAndroidApp
class PozzoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // The channel must exist before the first push, also when Firebase shows it in the background.
        PushNotifications.createChannel(this)
    }
}
