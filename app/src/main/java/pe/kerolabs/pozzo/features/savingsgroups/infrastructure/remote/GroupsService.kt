package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GroupsService {

    @GET("members/me/groups")
    suspend fun getMyGroups(): Response<List<GroupDto>>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequestDto): Response<GroupDto>

    @GET("groups/{groupId}")
    suspend fun getGroup(@Path("groupId") groupId: String): Response<GroupDto>

    @PUT("groups/{groupId}/rules")
    suspend fun updateRules(@Path("groupId") groupId: String, @Body request: UpdateRulesRequestDto): Response<GroupDto>

    @PATCH("groups/{groupId}/destination")
    suspend fun defineDestination(@Path("groupId") groupId: String, @Body request: DestinationDto): Response<GroupDto>

    @DELETE("groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: String): Response<Unit>

    @POST("groups/{groupId}/start")
    suspend fun startGroup(@Path("groupId") groupId: String): Response<GroupDto>

    @POST("groups/{groupId}/invitations")
    suspend fun generateInvitation(@Path("groupId") groupId: String): Response<InvitationDto>

    @GET("groups/{groupId}/invitations/active")
    suspend fun getActiveInvitation(@Path("groupId") groupId: String): Response<InvitationDto>

    @GET("invitations/{code}")
    suspend fun previewInvitation(@Path("code") code: String): Response<GroupPreviewDto>

    @POST("invitations/{code}/join")
    suspend fun joinGroup(@Path("code") code: String): Response<GroupDto>

    @GET("groups/{groupId}/members")
    suspend fun getMembers(@Path("groupId") groupId: String): Response<List<MemberDto>>

    @POST("groups/{groupId}/members/manual")
    suspend fun addManualMember(
        @Path("groupId") groupId: String,
        @Body request: AddManualMemberRequestDto,
    ): Response<List<MemberDto>>

    @DELETE("groups/{groupId}/members/{membershipId}")
    suspend fun removeMember(
        @Path("groupId") groupId: String,
        @Path("membershipId") membershipId: String,
    ): Response<Unit>

    @GET("groups/{groupId}/turns")
    suspend fun getTurns(@Path("groupId") groupId: String): Response<TurnCalendarDto>

    @POST("groups/{groupId}/turns/draw")
    suspend fun drawTurns(@Path("groupId") groupId: String): Response<TurnCalendarDto>

    @POST("groups/{groupId}/turns/agreed")
    suspend fun assignAgreedTurns(
        @Path("groupId") groupId: String,
        @Body request: AgreedTurnsRequestDto,
    ): Response<TurnCalendarDto>
}
