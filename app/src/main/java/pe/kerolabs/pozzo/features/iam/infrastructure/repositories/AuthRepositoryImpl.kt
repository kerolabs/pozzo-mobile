package pe.kerolabs.pozzo.features.iam.infrastructure.repositories

import android.os.Build
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.iam.domain.AuthRepository
import pe.kerolabs.pozzo.features.iam.domain.CodeRequest
import pe.kerolabs.pozzo.features.iam.domain.Profile
import pe.kerolabs.pozzo.features.iam.domain.RecoveryCodeRequest
import pe.kerolabs.pozzo.features.iam.domain.ThemePreference
import pe.kerolabs.pozzo.features.iam.domain.Verification
import pe.kerolabs.pozzo.features.iam.infrastructure.local.SessionManager
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.AuthService
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.ChangePhoneNumberRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.ProfileDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.RecoverAccountRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.RecoveryEmailRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.RecoveryPhoneCodeRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.RegisterRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.RequestCodeRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.UpdateProfileRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.VerifyCodeRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.VerifyRecoveryCodeRequestDto

class AuthRepositoryImpl @Inject constructor(
    private val service: AuthService,
    private val sessionManager: SessionManager,
) : AuthRepository {

    private val deviceLabel: String = "${Build.MANUFACTURER} ${Build.MODEL}".take(80)

    override val isSignedIn: Flow<Boolean> = sessionManager.isSignedIn

    override val displayName: Flow<String?> = sessionManager.displayName

    override val theme: Flow<ThemePreference> = sessionManager.theme.map(ThemePreference::of)

    override val photoUrl: Flow<String?> = sessionManager.photoUrl

    override suspend fun requestCode(phoneNumber: String): Result<CodeRequest> =
        apiCall { service.requestCode(RequestCodeRequestDto(phoneNumber)) }.map { dto ->
            // The backend answers in E.164; the app keeps the nine digits it sends back to verify.
            CodeRequest(phoneNumber, Instant.parse(dto.expiresAt), Instant.parse(dto.resendAvailableAt))
        }

    override suspend fun verifyCode(phoneNumber: String, code: String): Result<Verification> =
        apiCall { service.verifyCode(VerifyCodeRequestDto(phoneNumber, code, deviceLabel)) }.map { dto ->
            val session = dto.session
            if (dto.registrationRequired || session == null) {
                Verification.RegistrationRequired(phoneNumber, dto.registrationToken.orEmpty())
            } else {
                sessionManager.save(session.token, session.profile.displayName, session.profile.theme)
                sessionManager.updateProfile(session.profile.displayName, session.profile.theme, session.profile.photoUrl)
                Verification.SignedIn(session.profile.toDomain())
            }
        }

    override suspend fun completeRegistration(
        registrationToken: String,
        displayName: String,
        termsAccepted: Boolean,
    ): Result<Profile> =
        apiCall {
            service.register(RegisterRequestDto(registrationToken, displayName, null, termsAccepted, deviceLabel))
        }.map { dto ->
            sessionManager.save(dto.token, dto.profile.displayName, dto.profile.theme)
            dto.profile.keep()
        }

    override suspend fun getProfile(): Result<Profile> =
        apiCall { service.getProfile() }.map { it.keep() }

    override suspend fun updateProfile(change: (Profile) -> Profile): Result<Profile> {
        // The backend replaces the whole profile, so the fields that do not change are sent back as they are.
        val updated = change(getProfile().getOrElse { return Result.failure(it) })
        val request = UpdateProfileRequestDto(
            displayName = updated.displayName,
            photoUrl = updated.photoUrl,
            theme = updated.theme,
            walletNumber = updated.walletNumber?.ifBlank { null },
            backupEmail = updated.backupEmail?.ifBlank { null },
        )
        return apiCall { service.updateProfile(request) }.map { it.keep() }
    }

    override suspend fun changePhoto(jpeg: ByteArray): Result<Profile> {
        val part = MultipartBody.Part.createFormData(
            "photo",
            "photo.jpg",
            jpeg.toRequestBody("image/jpeg".toMediaType()),
        )
        return apiCall { service.changePhoto(part) }.map { it.keep() }
    }

    override suspend fun removePhoto(): Result<Profile> = apiCall { service.removePhoto() }.map { it.keep() }

    /** Keeps on the phone what is shown without a request: name, theme and photo. */
    private suspend fun ProfileDto.keep(): Profile {
        sessionManager.updateProfile(displayName, theme, photoUrl)
        return toDomain()
    }

    override suspend fun requestRecoveryCode(email: String): Result<RecoveryCodeRequest> =
        apiCall { service.requestRecoveryCode(RecoveryEmailRequestDto(email)) }.map { dto ->
            RecoveryCodeRequest(dto.email, Instant.parse(dto.expiresAt), Instant.parse(dto.resendAvailableAt))
        }

    override suspend fun verifyRecoveryCode(email: String, code: String): Result<String> =
        apiCall { service.verifyRecoveryCode(VerifyRecoveryCodeRequestDto(email, code)) }.map { it.recoveryToken }

    override suspend fun requestRecoveryPhoneCode(recoveryToken: String, phoneNumber: String): Result<CodeRequest> =
        apiCall { service.requestRecoveryPhoneCode(RecoveryPhoneCodeRequestDto(recoveryToken, phoneNumber)) }.map { dto ->
            CodeRequest(phoneNumber, Instant.parse(dto.expiresAt), Instant.parse(dto.resendAvailableAt))
        }

    override suspend fun recoverAccount(recoveryToken: String, phoneNumber: String, code: String): Result<Profile> =
        apiCall {
            service.recoverAccount(RecoverAccountRequestDto(recoveryToken, phoneNumber, code, deviceLabel))
        }.map { dto ->
            sessionManager.save(dto.token, dto.profile.displayName, dto.profile.theme)
            dto.profile.keep()
        }

    override suspend fun requestPhoneChangeCode(phoneNumber: String): Result<CodeRequest> =
        apiCall { service.requestPhoneChangeCode(RequestCodeRequestDto(phoneNumber)) }.map { dto ->
            CodeRequest(phoneNumber, Instant.parse(dto.expiresAt), Instant.parse(dto.resendAvailableAt))
        }

    override suspend fun changePhoneNumber(phoneNumber: String, code: String): Result<Profile> =
        apiCall { service.changePhoneNumber(ChangePhoneNumberRequestDto(phoneNumber, code)) }.map { it.keep() }

    override suspend fun signOut(): Result<Unit> {
        // The session is closed on the phone even if the backend cannot be reached.
        val result = apiCall { service.signOut() }
        sessionManager.clear()
        return result.map { }
    }

    private fun ProfileDto.toDomain() =
        Profile(accountId, phoneNumber, displayName, photoUrl, theme, walletNumber, backupEmail)
}
