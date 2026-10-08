package pe.kerolabs.pozzo.features.notifications.infrastructure.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pe.kerolabs.pozzo.MainActivity
import pe.kerolabs.pozzo.R

/**
 * The notification channel of Pozzo and the notices shown while the app is open. When the app is in the
 * background, Firebase shows them by itself in the same channel and opens [MainActivity] with the same
 * [EXTRA_DEEP_LINK].
 */
object PushNotifications {

    const val CHANNEL_ID = "pozzo_notices"

    /** Key of the screen to open, as the backend sends it in the data of each push. */
    const val EXTRA_DEEP_LINK = "deepLink"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Recordatorios y avisos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Recordatorios antes del corte y avisos de tus juntas"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, id: String, title: String, body: String, deepLink: String?) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK, deepLink)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
    }
}
