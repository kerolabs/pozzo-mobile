package pe.kerolabs.pozzo.features.iam.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthService {

    @POST("auth/codes")
    suspend fun requestCode(@Body request: RequestCodeRequestDto): Response<CodeRequestedDto>

    @POST("auth/codes/verify")
    suspend fun verifyCode(@Body request: VerifyCodeRequestDto): Response<VerificationDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthenticatedDto>

    @POST("auth/sign-out")
    suspend fun signOut(): Response<Unit>

    @GET("members/me/profile")
    suspend fun getProfile(): Response<ProfileDto>

    @PUT("members/me/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): Response<ProfileDto>
}
