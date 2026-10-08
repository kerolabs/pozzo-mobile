package pe.kerolabs.pozzo.features.iam.infrastructure.remote

data class RequestCodeRequestDto(val phoneNumber: String)

data class CodeRequestedDto(val phoneNumber: String, val expiresAt: String, val resendAvailableAt: String)

data class VerifyCodeRequestDto(val phoneNumber: String, val code: String, val deviceLabel: String?)

data class VerificationDto(
    val registrationRequired: Boolean,
    val session: AuthenticatedDto?,
    val registrationToken: String?,
    val registrationTokenExpiresAt: String?,
)

data class RegisterRequestDto(
    val registrationToken: String,
    val displayName: String,
    val photoUrl: String?,
    val termsAccepted: Boolean,
    val deviceLabel: String?,
)

data class AuthenticatedDto(val token: String, val expiresAt: String, val profile: ProfileDto)

data class UpdateProfileRequestDto(
    val displayName: String,
    val photoUrl: String?,
    val theme: String,
    val walletNumber: String?,
    val backupEmail: String?,
)

data class ProfileDto(
    val accountId: String,
    val phoneNumber: String,
    val displayName: String,
    val photoUrl: String?,
    val theme: String,
    val walletNumber: String?,
    val backupEmail: String?,
)

data class RecoveryEmailRequestDto(val email: String)

data class RecoveryCodeRequestedDto(val email: String, val expiresAt: String, val resendAvailableAt: String)

data class VerifyRecoveryCodeRequestDto(val email: String, val code: String)

data class RecoveryTokenDto(val recoveryToken: String, val expiresAt: String)

data class RecoveryPhoneCodeRequestDto(val recoveryToken: String, val phoneNumber: String)

data class RecoverAccountRequestDto(
    val recoveryToken: String,
    val phoneNumber: String,
    val code: String,
    val deviceLabel: String?,
)

data class ChangePhoneNumberRequestDto(val phoneNumber: String, val code: String)
