package pe.kerolabs.pozzo.features.iam.application

import android.util.Patterns
import javax.inject.Inject
import pe.kerolabs.pozzo.features.iam.domain.AuthRepository

/** True for something that looks like an email; the backend checks it again. */
fun isEmail(value: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

class RequestRecoveryCodeUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String) = repository.requestRecoveryCode(email.trim().lowercase())
}

class VerifyRecoveryCodeUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String) = repository.verifyRecoveryCode(email.trim().lowercase(), code)
}

class RequestRecoveryPhoneCodeUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(recoveryToken: String, phoneNumber: String) =
        repository.requestRecoveryPhoneCode(recoveryToken, phoneNumber)
}

class RecoverAccountUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(recoveryToken: String, phoneNumber: String, code: String) =
        repository.recoverAccount(recoveryToken, phoneNumber, code)
}

class RequestPhoneChangeCodeUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(phoneNumber: String) = repository.requestPhoneChangeCode(phoneNumber)
}

class ChangePhoneNumberUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(phoneNumber: String, code: String) = repository.changePhoneNumber(phoneNumber, code)
}
