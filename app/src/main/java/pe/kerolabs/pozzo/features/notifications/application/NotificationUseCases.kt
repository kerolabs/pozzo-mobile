package pe.kerolabs.pozzo.features.notifications.application

import java.time.Instant
import javax.inject.Inject
import pe.kerolabs.pozzo.features.notifications.domain.NotificationRepository
import pe.kerolabs.pozzo.features.notifications.domain.ReminderPlan

class GetMyNoticesUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke() = repository.getMyNotices()
}

class GetReminderPlanUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(groupId: String) = repository.getReminderPlan(groupId)
}

class SaveReminderPlanUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(plan: ReminderPlan) = repository.saveReminderPlan(plan)
}

class RegisterDeviceUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke() = repository.registerDevice()
}

class UnregisterDeviceUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke() = repository.unregisterDevice()
}

/** How many notices arrived after the member last opened the inbox. */
class CountUnseenNoticesUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(): Result<Int> {
        val lastSeen = repository.lastSeenAt()
        return repository.getMyNotices().map { notices -> notices.count { lastSeen == null || it.sentAt.isAfter(lastSeen) } }
    }
}

class MarkNoticesSeenUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(until: Instant) = repository.markSeen(until)
}

class GetLastSeenNoticeUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke() = repository.lastSeenAt()
}
