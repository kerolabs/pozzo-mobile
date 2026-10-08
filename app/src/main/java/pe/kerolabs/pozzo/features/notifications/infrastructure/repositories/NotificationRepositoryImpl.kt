package pe.kerolabs.pozzo.features.notifications.infrastructure.repositories

import com.google.firebase.messaging.FirebaseMessaging
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.notifications.domain.Notice
import pe.kerolabs.pozzo.features.notifications.domain.NotificationKind
import pe.kerolabs.pozzo.features.notifications.domain.NotificationRepository
import pe.kerolabs.pozzo.features.notifications.domain.ReminderPlan
import pe.kerolabs.pozzo.features.notifications.infrastructure.local.NoticePreferences
import pe.kerolabs.pozzo.features.notifications.infrastructure.remote.ConfigureReminderPlanRequestDto
import pe.kerolabs.pozzo.features.notifications.infrastructure.remote.NotificationsService
import pe.kerolabs.pozzo.features.notifications.infrastructure.remote.RegisterDeviceRequestDto
import pe.kerolabs.pozzo.features.notifications.infrastructure.remote.ReminderPlanDto

class NotificationRepositoryImpl @Inject constructor(
    private val service: NotificationsService,
    private val preferences: NoticePreferences,
) : NotificationRepository {

    override suspend fun getMyNotices(): Result<List<Notice>> =
        apiCall { service.getMyNotifications() }.map { list ->
            list.map {
                Notice(
                    id = it.id,
                    groupId = it.groupId,
                    kind = if (it.kind == "REMINDER") NotificationKind.REMINDER else NotificationKind.ALERT,
                    title = it.title,
                    body = it.body,
                    deepLink = it.deepLink,
                    sentAt = Instant.parse(it.sentAt),
                )
            }
        }

    override suspend fun getReminderPlan(groupId: String): Result<ReminderPlan> =
        apiCall { service.getReminderPlan(groupId) }.map { it.toDomain() }

    override suspend fun saveReminderPlan(plan: ReminderPlan): Result<ReminderPlan> =
        apiCall {
            service.saveReminderPlan(
                plan.groupId,
                ConfigureReminderPlanRequestDto(plan.offsetsInDays, plan.sendHour, plan.enabled),
            )
        }.map { it.toDomain() }

    override suspend fun registerDevice(): Result<Unit> {
        val token = runCatching { currentPushToken() }.getOrElse { return Result.failure(it) }
        return apiCall { service.registerDevice(RegisterDeviceRequestDto(token, "ANDROID")) }
            .map { device -> preferences.saveDeviceId(device.id) }
    }

    override suspend fun unregisterDevice(): Result<Unit> {
        val deviceId = preferences.deviceId() ?: return Result.success(Unit)
        val result = apiCall { service.unregisterDevice(deviceId) }.map { }
        // Without a session the device no longer belongs to anyone; it is forgotten either way.
        preferences.clear()
        return result
    }

    override suspend fun lastSeenAt(): Instant? = preferences.lastSeenAt()

    override suspend fun markSeen(until: Instant) = preferences.saveLastSeenAt(until)

    private suspend fun currentPushToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    private fun ReminderPlanDto.toDomain() = ReminderPlan(groupId, offsetsInDays.sortedDescending(), sendHour, enabled)
}
