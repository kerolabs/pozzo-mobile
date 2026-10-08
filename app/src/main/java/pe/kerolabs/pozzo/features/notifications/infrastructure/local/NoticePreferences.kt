package pe.kerolabs.pozzo.features.notifications.infrastructure.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.noticesDataStore by preferencesDataStore(name = "notices")

/**
 * What the phone remembers about the notices: the id of this device in the backend, to remove it when
 * signing out, and the newest notice already seen in the inbox, to mark the new ones.
 */
@Singleton
class NoticePreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val LAST_SEEN_AT = longPreferencesKey("last_seen_at")
    }

    suspend fun deviceId(): String? = context.noticesDataStore.data.first()[DEVICE_ID]

    suspend fun saveDeviceId(id: String) {
        context.noticesDataStore.edit { it[DEVICE_ID] = id }
    }

    suspend fun lastSeenAt(): Instant? = context.noticesDataStore.data.first()[LAST_SEEN_AT]?.let(Instant::ofEpochMilli)

    suspend fun saveLastSeenAt(at: Instant) {
        context.noticesDataStore.edit { it[LAST_SEEN_AT] = at.toEpochMilli() }
    }

    suspend fun clear() {
        context.noticesDataStore.edit { it.clear() }
    }
}
