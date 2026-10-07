package pe.kerolabs.pozzo.features.iam.application

import javax.inject.Inject
import pe.kerolabs.pozzo.features.iam.domain.AuthRepository
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

class UpdateProfileUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(displayName: String, theme: ThemePreference) =
        repository.updateProfile(displayName.trim(), theme)
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
