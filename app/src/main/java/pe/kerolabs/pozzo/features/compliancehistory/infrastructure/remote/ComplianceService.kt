package pe.kerolabs.pozzo.features.compliancehistory.infrastructure.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ComplianceService {

    @GET("members/me/compliance")
    suspend fun getMyHistory(): Response<MyHistoryDto>

    @GET("groups/{groupId}/compliance")
    suspend fun getGroupCompliance(@Path("groupId") groupId: String): Response<List<MemberComplianceDto>>

    @GET("members/{memberId}/compliance/summary")
    suspend fun getMemberSummary(@Path("memberId") memberId: String): Response<ComplianceSummaryDto>

    @POST("members/me/compliance/share")
    suspend fun shareMyHistory(): Response<ShareLinkDto>
}

data class ComplianceSummaryDto(
    val level: String,
    val contributions: Int,
    val onTime: Int,
    val late: Int,
    val covered: Int,
    val rejected: Int,
    val dropouts: Int,
    val cyclesCompleted: Int,
)

data class MyHistoryDto(val summary: ComplianceSummaryDto, val groups: List<GroupItemDto>)

data class GroupItemDto(
    val groupId: String,
    val groupName: String,
    val status: String,
    val onTime: Int,
    val contributions: Int,
)

data class MemberComplianceDto(
    val membershipId: String,
    val displayName: String,
    val organizer: Boolean,
    val usesApp: Boolean,
    // Members without the application have no history.
    val summary: ComplianceSummaryDto?,
)

data class ShareLinkDto(val token: String, val url: String, val expiresAt: String)
