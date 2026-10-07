package pe.kerolabs.pozzo.features.iam.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** True while there is an open session on this phone. */
    val isSignedIn: Flow<Boolean>

    /** The name of the member signed in, kept locally to greet them without a request. */
    val displayName: Flow<String?>

    /** The visual theme chosen by the member, kept locally so the app opens with it. */
    val theme: Flow<ThemePreference>

    suspend fun requestCode(phoneNumber: String): Result<CodeRequest>

    suspend fun verifyCode(phoneNumber: String, code: String): Result<Verification>

    suspend fun completeRegistration(registrationToken: String, displayName: String, termsAccepted: Boolean): Result<Profile>

    suspend fun getProfile(): Result<Profile>

    suspend fun updateProfile(displayName: String, theme: ThemePreference): Result<Profile>

    suspend fun signOut(): Result<Unit>
}
