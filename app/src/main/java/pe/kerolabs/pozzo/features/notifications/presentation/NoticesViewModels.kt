package pe.kerolabs.pozzo.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.GetPotStateUseCase
import pe.kerolabs.pozzo.features.contributions.domain.isSettled
import pe.kerolabs.pozzo.features.notifications.application.CountUnseenNoticesUseCase
import pe.kerolabs.pozzo.features.notifications.application.GetLastSeenNoticeUseCase
import pe.kerolabs.pozzo.features.notifications.application.GetMyNoticesUseCase
import pe.kerolabs.pozzo.features.notifications.application.GetReminderPlanUseCase
import pe.kerolabs.pozzo.features.notifications.application.MarkNoticesSeenUseCase
import pe.kerolabs.pozzo.features.notifications.application.RegisterDeviceUseCase
import pe.kerolabs.pozzo.features.notifications.application.SaveReminderPlanUseCase
import pe.kerolabs.pozzo.features.notifications.domain.Notice
import pe.kerolabs.pozzo.features.notifications.domain.ReminderPlan
import pe.kerolabs.pozzo.features.savingsgroups.application.ObserveMyGroupsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupStatus

/** I3: the automatic reminders of a group the member organizes. */
data class ReminderSettings(val groupId: String, val groupName: String, val plan: ReminderPlan, val isSaving: Boolean = false)

/** J3: the reminders of a period stopped because the member's contribution is settled. */
data class StoppedReminders(val groupId: String, val groupName: String, val turnNumber: Int)

data class NoticesUiState(
    val notices: List<Notice> = emptyList(),
    val unseenAfter: Instant? = null,
    val reminders: List<ReminderSettings> = emptyList(),
    val stopped: List<StoppedReminders> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    fun isUnseen(notice: Notice): Boolean = unseenAfter == null || notice.sentAt.isAfter(unseenAfter)
}

/**
 * The Avisos destination: the reminders the organizer controls, the reminders already stopped and the
 * notices Pozzo sent. Opening it marks the notices as seen.
 */
@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val getMyNotices: GetMyNoticesUseCase,
    private val getLastSeen: GetLastSeenNoticeUseCase,
    private val markSeen: MarkNoticesSeenUseCase,
    private val observeMyGroups: ObserveMyGroupsUseCase,
    private val getReminderPlan: GetReminderPlanUseCase,
    private val saveReminderPlan: SaveReminderPlanUseCase,
    private val getPotState: GetPotStateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NoticesUiState())
    val state: StateFlow<NoticesUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.notices.isEmpty() && it.reminders.isEmpty(), errorMessage = null) }
            val started = observeMyGroups().first().filter { it.status == GroupStatus.STARTED }
            val noticesCall = async { getMyNotices() }
            val remindersCall = started.filter { it.isOrganizer }.map { group ->
                async { getReminderPlan(group.id).getOrNull()?.let { ReminderSettings(group.id, group.name, it) } }
            }
            val stoppedCall = started.map { group ->
                async {
                    getPotState(group.id).getOrNull()
                        ?.takeIf { (_, period) -> period.myState?.isSettled == true }
                        ?.let { (_, period) -> StoppedReminders(group.id, group.name, period.turnNumber) }
                }
            }
            val lastSeen = getLastSeen()
            val notices = noticesCall.await()
            _state.update {
                it.copy(
                    notices = notices.getOrDefault(it.notices),
                    // The dots of this visit compare with what was seen before opening it.
                    unseenAfter = if (it.notices.isEmpty()) lastSeen else it.unseenAfter,
                    reminders = remindersCall.awaitAll().filterNotNull(),
                    stopped = stoppedCall.awaitAll().filterNotNull(),
                    isLoading = false,
                    errorMessage = notices.exceptionOrNull()?.userMessage(),
                )
            }
            notices.getOrNull()?.firstOrNull()?.let { newest -> markSeen(newest.sentAt) }
        }
    }

    fun setRemindersEnabled(groupId: String, enabled: Boolean) {
        val settings = _state.value.reminders.firstOrNull { it.groupId == groupId } ?: return
        updateReminders(groupId) { it.copy(plan = it.plan.copy(enabled = enabled), isSaving = true) }
        viewModelScope.launch {
            saveReminderPlan(settings.plan.copy(enabled = enabled))
                .onSuccess { saved -> updateReminders(groupId) { it.copy(plan = saved, isSaving = false) } }
                .onFailure { error ->
                    updateReminders(groupId) { settings }
                    _state.update { it.copy(errorMessage = error.userMessage()) }
                }
        }
    }

    private fun updateReminders(groupId: String, change: (ReminderSettings) -> ReminderSettings) =
        _state.update { state -> state.copy(reminders = state.reminders.map { if (it.groupId == groupId) change(it) else it }) }
}

/**
 * App-wide: registers this phone for pushes once there is a session, and counts the notices not seen
 * yet for the dot of the Avisos destination.
 */
@HiltViewModel
class PushViewModel @Inject constructor(
    private val registerDevice: RegisterDeviceUseCase,
    private val countUnseen: CountUnseenNoticesUseCase,
) : ViewModel() {

    private val _unseen = MutableStateFlow(0)
    val unseen: StateFlow<Int> = _unseen.asStateFlow()

    private var registered = false

    fun onSignedIn() {
        if (registered) return
        registered = true
        viewModelScope.launch { registerDevice().onFailure { registered = false } }
    }

    fun onSignedOut() {
        registered = false
        _unseen.value = 0
    }

    fun refreshUnseen() {
        viewModelScope.launch { countUnseen().onSuccess { _unseen.value = it } }
    }
}
