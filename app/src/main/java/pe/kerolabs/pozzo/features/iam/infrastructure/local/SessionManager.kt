package pe.kerolabs.pozzo.features.iam.infrastructure.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pe.kerolabs.pozzo.core.network.AccessTokenProvider

/**
 * Keeps the session on the phone with DataStore: the token and the name of the member. The token is
 * also kept in memory so the network layer reads it without waiting.
 */
@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AccessTokenProvider {

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var loaded = false

    val isSignedIn: Flow<Boolean> = dataStore.data.map { it[TOKEN] != null }

    val displayName: Flow<String?> = dataStore.data.map { it[DISPLAY_NAME] }

    suspend fun save(token: String, displayName: String) {
        dataStore.edit {
            it[TOKEN] = token
            it[DISPLAY_NAME] = displayName
        }
        cachedToken = token
        loaded = true
    }

    suspend fun updateDisplayName(displayName: String) {
        dataStore.edit { it[DISPLAY_NAME] = displayName }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
        cachedToken = null
        loaded = true
    }

    override fun currentToken(): String? {
        if (!loaded) {
            // Only the first request after the app starts reads the disk.
            cachedToken = runBlocking { dataStore.data.first()[TOKEN] }
            loaded = true
        }
        return cachedToken
    }

    override fun onUnauthorized() {
        scope.launch { clear() }
    }
}
