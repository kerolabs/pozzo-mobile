package pe.kerolabs.pozzo.features.notifications.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationsService {

    @GET("members/me/notifications")
    suspend fun getMyNotifications(): Response<List<NotificationDto>>

    @POST("members/me/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequestDto): Response<DeviceDto>

    @DELETE("members/me/devices/{deviceId}")
    suspend fun unregisterDevice(@Path("deviceId") deviceId: String): Response<Unit>

    @GET("groups/{groupId}/reminder-plan")
    suspend fun getReminderPlan(@Path("groupId") groupId: String): Response<ReminderPlanDto>

    @PUT("groups/{groupId}/reminder-plan")
    suspend fun saveReminderPlan(
        @Path("groupId") groupId: String,
        @Body request: ConfigureReminderPlanRequestDto,
    ): Response<ReminderPlanDto>
}

data class NotificationDto(
    val id: String,
    val groupId: String?,
    val periodId: String?,
    val kind: String,
    val title: String,
    val body: String,
    val deepLink: String?,
    val sentAt: String,
)

data class RegisterDeviceRequestDto(val pushToken: String, val platform: String)

data class DeviceDto(val id: String, val platform: String, val registeredAt: String, val active: Boolean)

data class ReminderPlanDto(
    val groupId: String,
    val offsetsInDays: List<Int>,
    val sendHour: Int,
    val enabled: Boolean,
    val updatedAt: String?,
)

data class ConfigureReminderPlanRequestDto(val offsetsInDays: List<Int>, val sendHour: Int, val enabled: Boolean)
