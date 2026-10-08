package pe.kerolabs.pozzo.features.iam.domain

import java.time.Instant

/**
 * The member signed in: the name the group sees, the phone, the visual theme and the contact data only
 * the member sees, the Yape or Plin number (nine digits) and a backup email.
 */
data class Profile(
    val accountId: String,
    val phoneNumber: String,
    val displayName: String,
    val photoUrl: String?,
    val theme: String,
    val walletNumber: String?,
    val backupEmail: String?,
) {
    val firstName: String get() = displayName.trim().substringBefore(' ')
}

/** I2: how the app looks; SYSTEM follows the phone. */
enum class ThemePreference {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun of(value: String?): ThemePreference = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/**
 * A code sent by SMS: until when it is valid and from when another one can be requested.
 */
data class CodeRequest(
    val phoneNumber: String,
    val expiresAt: Instant,
    val resendAvailableAt: Instant,
)

/**
 * A recovery code was requested for a backup email; it only arrives if an account has that email.
 */
data class RecoveryCodeRequest(val email: String, val expiresAt: Instant, val resendAvailableAt: Instant)

/**
 * What happens after a correct code: a registered number opens a session; a new one must complete
 * the registration with the token it received.
 */
sealed interface Verification {
    data class SignedIn(val profile: Profile) : Verification
    data class RegistrationRequired(val phoneNumber: String, val registrationToken: String) : Verification
}

/**
 * A Peruvian mobile number: nine digits starting with 9.
 */
object PhoneNumbers {
    fun isValid(digits: String): Boolean = digits.length == 9 && digits.first() == '9' && digits.all(Char::isDigit)

    /** "999000123" as "999 000 123". */
    fun grouped(digits: String): String = digits.chunked(3).joinToString(" ")

    /** "999000123" as "+51 999 000 123". */
    fun display(phone: String): String {
        val digits = phone.filter(Char::isDigit).removePrefix("51").takeLast(9)
        return "+51 " + digits.chunked(3).joinToString(" ")
    }
}
