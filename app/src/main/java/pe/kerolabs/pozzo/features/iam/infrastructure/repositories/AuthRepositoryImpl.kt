package pe.kerolabs.pozzo.features.iam.infrastructure.repositories

import android.os.Build
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.iam.domain.AuthRepository
import pe.kerolabs.pozzo.features.iam.domain.CodeRequest
import pe.kerolabs.pozzo.features.iam.domain.Profile
import pe.kerolabs.pozzo.features.iam.domain.Verification
import pe.kerolabs.pozzo.features.iam.infrastructure.local.SessionManager
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.AuthService
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.ProfileDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.RegisterRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.RequestCodeRequestDto
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.VerifyCodeRequestDto

class AuthRepositoryImpl @Inject constructor(
    private val service: AuthService,
    private val sessionManager: SessionManager,
) : AuthRepository {

    private val deviceLabel: String = "${Build.MANUFACTURER} ${Build.MODEL}".take(80)

    override val isSignedIn: Flow<Boolean> = sessionManager.isSignedIn

    override val displayName: Flow<String?> = sessionManager.displayName

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
                sessionManager.save(session.token, session.profile.displayName)
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
            sessionManager.save(dto.token, dto.profile.displayName)
            dto.profile.toDomain()
        }

    override suspend fun getProfile(): Result<Profile> =
        apiCall { service.getProfile() }.map { dto ->
            sessionManager.updateDisplayName(dto.displayName)
            dto.toDomain()
        }

    override suspend fun signOut(): Result<Unit> {
        // The session is closed on the phone even if the backend cannot be reached.
        val result = apiCall { service.signOut() }
        sessionManager.clear()
        return result.map { }
    }

    private fun ProfileDto.toDomain() = Profile(accountId, phoneNumber, displayName, photoUrl, theme)
}
