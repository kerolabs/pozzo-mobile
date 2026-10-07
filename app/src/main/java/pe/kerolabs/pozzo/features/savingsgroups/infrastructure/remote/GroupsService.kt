package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote

import retrofit2.Response
import retrofit2.http.GET

interface GroupsService {

    @GET("members/me/groups")
    suspend fun getMyGroups(): Response<List<GroupDto>>
}
