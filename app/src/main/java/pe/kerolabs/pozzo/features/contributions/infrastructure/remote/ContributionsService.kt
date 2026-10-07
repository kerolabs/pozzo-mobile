package pe.kerolabs.pozzo.features.contributions.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ContributionsService {

    @GET("groups/{groupId}/cycle")
    suspend fun getCycleOfGroup(@Path("groupId") groupId: String): Response<CycleDto>

    @GET("cycles/{cycleId}/periods/current")
    suspend fun getCurrentPeriod(@Path("cycleId") cycleId: String): Response<PeriodStatusDto>

    @POST("periods/{periodId}/contributions")
    suspend fun registerContribution(@Path("periodId") periodId: String, @Body receipt: ReceiptDto): Response<ContributionDto>

    @POST("periods/{periodId}/contributions/cash")
    suspend fun registerCash(@Path("periodId") periodId: String, @Body request: RegisterCashRequestDto): Response<ContributionDto>

    @POST("periods/{periodId}/contributions/coverage")
    suspend fun registerCoverage(
        @Path("periodId") periodId: String,
        @Body request: RegisterCoverageRequestDto,
    ): Response<ContributionDto>

    @GET("periods/{periodId}/contributions/pending-review")
    suspend fun getPendingReviews(@Path("periodId") periodId: String): Response<List<ContributionDto>>

    @PATCH("contributions/{contributionId}/review")
    suspend fun review(@Path("contributionId") contributionId: String, @Body request: ReviewRequestDto): Response<ContributionDto>

    @POST("periods/{periodId}/payout")
    suspend fun deliverPot(@Path("periodId") periodId: String): Response<PotDeliveryDto>
}
