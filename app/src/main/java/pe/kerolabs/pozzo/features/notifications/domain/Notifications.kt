package pe.kerolabs.pozzo.features.notifications.domain

import java.time.Instant

/** A REMINDER goes out before a cutoff date; an ALERT says that something happened. */
enum class NotificationKind { REMINDER, ALERT }

/** A notice Pozzo sent to the member, also kept in the Avisos inbox. */
data class Notice(
    val id: String,
    val groupId: String?,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val deepLink: String?,
    val sentAt: Instant,
)

/** I3: when the automatic reminders of a group go out: days before the cutoff and the hour. */
data class ReminderPlan(
    val groupId: String,
    val offsetsInDays: List<Int>,
    val sendHour: Int,
    val enabled: Boolean,
)

interface NotificationRepository {

    /** The last notices sent to the member, newest first. */
    suspend fun getMyNotices(): Result<List<Notice>>

    suspend fun getReminderPlan(groupId: String): Result<ReminderPlan>

    /** Only the organizer of the group. */
    suspend fun saveReminderPlan(plan: ReminderPlan): Result<ReminderPlan>

    /** Registers this phone to receive push notifications; repeating it with the same token changes nothing. */
    suspend fun registerDevice(): Result<Unit>

    /** This phone stops receiving push notifications, e.g. before signing out. */
    suspend fun unregisterDevice(): Result<Unit>

    /** The moment of the newest notice the member has already seen in the inbox. */
    suspend fun lastSeenAt(): Instant?

    suspend fun markSeen(until: Instant)
}
