package pe.kerolabs.pozzo.features.compliancehistory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.compliancehistory.application.GetGroupComplianceUseCase
import pe.kerolabs.pozzo.features.compliancehistory.application.GetMyHistoryUseCase
import pe.kerolabs.pozzo.features.compliancehistory.application.ShareMyHistoryUseCase
import pe.kerolabs.pozzo.features.compliancehistory.domain.MemberCompliance
import pe.kerolabs.pozzo.features.compliancehistory.domain.MyHistory
import pe.kerolabs.pozzo.features.contributions.application.GetCycleHistoryUseCase
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.savingsgroups.application.ObserveMyGroupsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.RefreshMyGroupsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupStatus
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup

/** A started group of the member, to choose in the history of their groups. */
data class HistoryGroup(val id: String, val name: String, val isClosed: Boolean)

/** The groups the history lists: the ones the member organizes or the ones they take part in. */
enum class HistoryRole(val label: String) { ORGANIZER("Cabeza de junta"), PARTICIPANT("Participante") }

data class HistoryUiState(
    val isResolving: Boolean = true,
    val role: HistoryRole = HistoryRole.ORGANIZER,
    val organizedGroups: List<HistoryGroup> = emptyList(),
    val joinedGroups: List<HistoryGroup> = emptyList(),
    val selectedGroupId: String? = null,
    val cycle: Cycle? = null,
    val periods: List<Period> = emptyList(),
    val members: List<MemberCompliance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** With a started group the history shows its periods (K1); without any, the member's own history (G3). */
    val hasGroups: Boolean get() = organizedGroups.isNotEmpty() || joinedGroups.isNotEmpty()
    val groups: List<HistoryGroup> get() = groupsOf(role)
    val selectedGroup: HistoryGroup? get() = groups.firstOrNull { it.id == selectedGroupId }

    /** Only the organizer reads how every member of the group complies (K2). */
    val showsMembers: Boolean get() = role == HistoryRole.ORGANIZER

    fun groupsOf(role: HistoryRole): List<HistoryGroup> =
        if (role == HistoryRole.ORGANIZER) organizedGroups else joinedGroups
}

/**
 * Root of the History destination: lists the started groups the member organizes or takes part in, and
 * loads the contributions by period of the chosen one and, for the organizer, the compliance of its members.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeMyGroups: ObserveMyGroupsUseCase,
    private val refreshMyGroups: RefreshMyGroupsUseCase,
    private val getCycleHistory: GetCycleHistoryUseCase,
    private val getGroupCompliance: GetGroupComplianceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            refreshMyGroups()
            val started = observeMyGroups().first()
                .filter { it.isStarted }
                .sortedBy { it.status == GroupStatus.CLOSED }
            val organized = started.filter { it.isOrganizer }.map(SavingsGroup::toHistoryGroup)
            val joined = started.filterNot { it.isOrganizer }.map(SavingsGroup::toHistoryGroup)
            val role = if (organized.isEmpty() && joined.isNotEmpty()) HistoryRole.PARTICIPANT else HistoryRole.ORGANIZER
            _state.update { it.copy(isResolving = false, organizedGroups = organized, joinedGroups = joined, role = role) }
            _state.value.groups.firstOrNull()?.let { select(it.id) }
        }
    }

    /** Lists the groups of the other role and opens the first one. */
    fun selectRole(role: HistoryRole) {
        val first = _state.value.groupsOf(role).firstOrNull() ?: return
        _state.update { it.copy(role = role) }
        select(first.id)
    }

    fun select(groupId: String) {
        _state.update { it.copy(selectedGroupId = groupId, cycle = null, periods = emptyList(), members = emptyList()) }
        load()
    }

    fun load() {
        val groupId = _state.value.selectedGroupId ?: return
        val withMembers = _state.value.showsMembers
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val history = async { getCycleHistory(groupId) }
            val compliance = async { if (withMembers) getGroupCompliance(groupId) else Result.success(emptyList()) }
            val error = history.await().exceptionOrNull() ?: compliance.await().exceptionOrNull()
            _state.update { current ->
                if (current.selectedGroupId != groupId) return@update current
                val (cycle, periods) = history.await().getOrNull() ?: (null to emptyList())
                current.copy(
                    isLoading = false,
                    cycle = cycle,
                    periods = periods,
                    members = compliance.await().getOrDefault(emptyList()),
                    errorMessage = error?.userMessage(),
                )
            }
        }
    }
}

private fun SavingsGroup.toHistoryGroup() = HistoryGroup(id, name, status == GroupStatus.CLOSED)

data class MyHistoryUiState(
    val history: MyHistory? = null,
    val isLoading: Boolean = true,
    val isSharing: Boolean = false,
    val shareUrl: String? = null,
    val errorMessage: String? = null,
)

/** G3: the member's compliance across every group, and a link to share it. */
@HiltViewModel
class MyHistoryViewModel @Inject constructor(
    private val getMyHistory: GetMyHistoryUseCase,
    private val shareMyHistory: ShareMyHistoryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MyHistoryUiState())
    val state: StateFlow<MyHistoryUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.history == null, errorMessage = null) }
            getMyHistory()
                .onSuccess { history -> _state.update { it.copy(history = history, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun share() {
        if (_state.value.isSharing) return
        viewModelScope.launch {
            _state.update { it.copy(isSharing = true, errorMessage = null) }
            shareMyHistory()
                .onSuccess { link -> _state.update { it.copy(isSharing = false, shareUrl = link.url) } }
                .onFailure { error -> _state.update { it.copy(isSharing = false, errorMessage = error.userMessage()) } }
        }
    }

    fun onShared() = _state.update { it.copy(shareUrl = null) }
}
