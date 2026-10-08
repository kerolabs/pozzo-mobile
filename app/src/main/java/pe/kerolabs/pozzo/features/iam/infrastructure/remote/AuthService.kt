package pe.kerolabs.pozzo.features.iam.infrastructure.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface AuthService {

    @POST("auth/codes")
    suspend fun requestCode(@Body request: RequestCodeRequestDto): Response<CodeRequestedDto>

    @POST("auth/codes/verify")
    suspend fun verifyCode(@Body request: VerifyCodeRequestDto): Response<VerificationDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthenticatedDto>

    @POST("auth/sign-out")
    suspend fun signOut(): Response<Unit>

    @POST("auth/recovery/codes")
    suspend fun requestRecoveryCode(@Body request: RecoveryEmailRequestDto): Response<RecoveryCodeRequestedDto>

    @POST("auth/recovery/codes/verify")
    suspend fun verifyRecoveryCode(@Body request: VerifyRecoveryCodeRequestDto): Response<RecoveryTokenDto>

    @POST("auth/recovery/phone-number/codes")
    suspend fun requestRecoveryPhoneCode(@Body request: RecoveryPhoneCodeRequestDto): Response<CodeRequestedDto>

    @POST("auth/recovery/phone-number")
    suspend fun recoverAccount(@Body request: RecoverAccountRequestDto): Response<AuthenticatedDto>

    @GET("members/me/profile")
    suspend fun getProfile(): Response<ProfileDto>

    @PUT("members/me/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): Response<ProfileDto>

    @Multipart
    @PUT("members/me/profile/photo")
    suspend fun changePhoto(@Part photo: MultipartBody.Part): Response<ProfileDto>

    @DELETE("members/me/profile/photo")
    suspend fun removePhoto(): Response<ProfileDto>

    @POST("members/me/phone-number/codes")
    suspend fun requestPhoneChangeCode(@Body request: RequestCodeRequestDto): Response<CodeRequestedDto>

    @PUT("members/me/phone-number")
    suspend fun changePhoneNumber(@Body request: ChangePhoneNumberRequestDto): Response<ProfileDto>
}
