package pe.kerolabs.pozzo.features.iam.domain

import kotlinx.coroutines.flow.Flow

/**
 * Contract defining operations for Identity and Access Management (IAM).
 *
 * Encapsulates authentication flows via phone SMS challenges, user registration,
 * profile retrieval and updates, persistent session tracking, avatar management,
 * and multi-step account recovery via verified email addresses.
 */
interface AuthRepository {

    /** True while there is an open session on this phone. */
    val isSignedIn: Flow<Boolean>

    /** The name of the member signed in, kept locally to greet them without a request. */
    val displayName: Flow<String?>

    /** The photo of the member, kept locally to show it without a request. */
    val photoUrl: Flow<String?>

    /** The visual theme chosen by the member, kept locally so the app opens with it. */
    val theme: Flow<ThemePreference>

    /**
     * Dispatches an SMS verification challenge code to the given [phoneNumber].
     *
     * @param phoneNumber Nine-digit local phone number.
     * @return Result containing code expiry and resend cooldown timestamps.
     */
    suspend fun requestCode(phoneNumber: String): Result<CodeRequest>

    /**
     * Validates the received verification code for the phone number.
     *
     * @param phoneNumber Nine-digit local phone number.
     * @param code Six-digit numeric OTP code.
     * @return Verification status indicating whether registration is needed or user is signed in.
     */
    suspend fun verifyCode(phoneNumber: String, code: String): Result<Verification>

    /**
     * Completes user registration with display name and accepted terms.
     *
     * @param registrationToken Temporary token issued during verification.
     * @param displayName Member's full or preferred name.
     * @param termsAccepted Whether legal terms were accepted.
     */
    suspend fun completeRegistration(registrationToken: String, displayName: String, termsAccepted: Boolean): Result<Profile>

    /**
     * Retrieves the current authenticated member's profile.
     */
    suspend fun getProfile(): Result<Profile>

    /** Reads the current profile, applies [change] and saves the result. */
    suspend fun updateProfile(change: (Profile) -> Profile): Result<Profile>

    /**
     * Closes the active session on this device and informs the server.
     */
    suspend fun signOut(): Result<Unit>

    /** Uploads a JPEG as the profile photo; the previous one is deleted. */
    suspend fun changePhoto(jpeg: ByteArray): Result<Profile>

    suspend fun removePhoto(): Result<Profile>

    /** Recovery, step 1: a code to the backup email. The answer is the same if no account has it. */
    suspend fun requestRecoveryCode(email: String): Result<RecoveryCodeRequest>

    /** Recovery, step 2: returns the token that allows linking a new number. */
    suspend fun verifyRecoveryCode(email: String, code: String): Result<String>

    /** Recovery, step 3: an SMS code to the new number. */
    suspend fun requestRecoveryPhoneCode(recoveryToken: String, phoneNumber: String): Result<CodeRequest>

    /** Recovery, step 4: links the new number, closes the sessions of the lost phone and opens one here. */
    suspend fun recoverAccount(recoveryToken: String, phoneNumber: String, code: String): Result<Profile>

    /** Change of number with the session open: an SMS code to the new number. */
    suspend fun requestPhoneChangeCode(phoneNumber: String): Result<CodeRequest>

    suspend fun changePhoneNumber(phoneNumber: String, code: String): Result<Profile>
}
