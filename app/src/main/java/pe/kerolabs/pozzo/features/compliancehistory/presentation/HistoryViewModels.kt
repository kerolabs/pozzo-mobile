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

/** A started group the member organizes, to choose in the organizer's history. */
data class OrganizedGroup(val id: String, val name: String, val isClosed: Boolean)

data class HistoryUiState(
    val isResolving: Boolean = true,
    val organizedGroups: List<OrganizedGroup> = emptyList(),
    val selectedGroupId: String? = null,
    val cycle: Cycle? = null,
    val periods: List<Period> = emptyList(),
    val members: List<MemberCompliance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** The organizer sees the history of their groups (K1 and K2); everyone else, their own (G3). */
    val isOrganizerView: Boolean get() = organizedGroups.isNotEmpty()
    val selectedGroup: OrganizedGroup? get() = organizedGroups.firstOrNull { it.id == selectedGroupId }
}

/**
 * Root of the History destination: decides between the organizer's view and the member's own history,
 * and loads the contributions by period and the compliance of the members of the chosen group.
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
            val organized = observeMyGroups().first()
                .filter { it.isOrganizer && it.isStarted }
                .sortedBy { it.status == GroupStatus.CLOSED }
                .map(SavingsGroup::toOrganized)
            _state.update { it.copy(isResolving = false, organizedGroups = organized) }
            organized.firstOrNull()?.let { select(it.id) }
        }
    }

    fun select(groupId: String) {
        _state.update { it.copy(selectedGroupId = groupId, cycle = null, periods = emptyList(), members = emptyList()) }
        load()
    }

    fun load() {
        val groupId = _state.value.selectedGroupId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val history = async { getCycleHistory(groupId) }
            val compliance = async { getGroupCompliance(groupId) }
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

private fun SavingsGroup.toOrganized() = OrganizedGroup(id, name, status == GroupStatus.CLOSED)

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
