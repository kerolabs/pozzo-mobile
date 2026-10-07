package pe.kerolabs.pozzo.features.iam.application

import javax.inject.Inject
import android.util.Patterns
import pe.kerolabs.pozzo.features.iam.domain.AuthRepository
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.iam.domain.Profile
import pe.kerolabs.pozzo.features.iam.domain.ThemePreference

class RequestCodeUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(phoneNumber: String) = repository.requestCode(phoneNumber)
}

class VerifyCodeUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(phoneNumber: String, code: String) = repository.verifyCode(phoneNumber, code)
}

class CompleteRegistrationUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(registrationToken: String, displayName: String, termsAccepted: Boolean) =
        repository.completeRegistration(registrationToken, displayName.trim(), termsAccepted)
}

class GetProfileUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.getProfile()
}

class UpdateDisplayNameUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(displayName: String) = repository.updateProfile { it.copy(displayName = displayName.trim()) }
}

class UpdateThemeUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(theme: ThemePreference) = repository.updateProfile { it.copy(theme = theme.name) }
}

/** The Yape or Plin number: nine digits starting with 9, or empty to remove it. */
class UpdateWalletNumberUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(digits: String): Result<Profile> {
        val number = digits.filter(Char::isDigit)
        if (number.isNotEmpty() && !PhoneNumbers.isValid(number)) {
            return Result.failure(IllegalArgumentException("El número debe tener 9 dígitos y empezar con 9."))
        }
        return repository.updateProfile { it.copy(walletNumber = number.ifEmpty { null }) }
    }
}

/** The backup email, or empty to remove it. */
class UpdateBackupEmailUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Profile> {
        val value = email.trim()
        if (value.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            return Result.failure(IllegalArgumentException("Ingresa un correo válido."))
        }
        return repository.updateProfile { it.copy(backupEmail = value.ifEmpty { null }) }
    }
}

class ObserveThemeUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke() = repository.theme
}

class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}

class ObserveSessionUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke() = repository.isSignedIn
}

class ObserveDisplayNameUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke() = repository.displayName
}
