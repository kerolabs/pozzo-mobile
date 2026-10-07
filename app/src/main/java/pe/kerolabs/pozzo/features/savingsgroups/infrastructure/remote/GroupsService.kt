package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GroupsService {

    @GET("members/me/groups")
    suspend fun getMyGroups(): Response<List<GroupDto>>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequestDto): Response<GroupDto>

    @POST("groups/{groupId}/invitations")
    suspend fun generateInvitation(@Path("groupId") groupId: String): Response<InvitationDto>

    @GET("invitations/{code}")
    suspend fun previewInvitation(@Path("code") code: String): Response<GroupPreviewDto>

    @POST("invitations/{code}/join")
    suspend fun joinGroup(@Path("code") code: String): Response<GroupDto>
}
