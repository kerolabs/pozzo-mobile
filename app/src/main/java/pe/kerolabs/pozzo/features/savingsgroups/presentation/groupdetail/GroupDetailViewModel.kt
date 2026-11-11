package pe.kerolabs.pozzo.features.savingsgroups.presentation.groupdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.ApiException
import pe.kerolabs.pozzo.core.push.IncomingPushes
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.savingsgroups.application.AddManualMemberUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.DeleteGroupUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.GetGroupDetailUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.GetInvitationUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.RegisterDropoutUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.RemoveMemberUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.ReplaceMemberUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupDetail
import pe.kerolabs.pozzo.features.savingsgroups.domain.Invitation
import pe.kerolabs.pozzo.features.savingsgroups.domain.Member
import pe.kerolabs.pozzo.features.savingsgroups.domain.Turn
import pe.kerolabs.pozzo.features.savingsgroups.presentation.navigation.GroupDetailRoute

enum class GroupDetailTab(val label: String) { MEMBERS("Integrantes"), TURNS("Turnos"), RULES("Reglas") }

data class AddMemberForm(
    val name: String = "",
    val phone: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && (phone.isEmpty() || PhoneNumbers.isValid(phone)) && !isSaving
}

/** The replacement of a member who dropped out, for the turn they held. */
data class ReplaceForm(
    val droppedMembershipId: String,
    val droppedName: String,
    val turnNumber: Int,
    val name: String = "",
    val phone: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && (phone.isEmpty() || PhoneNumbers.isValid(phone)) && !isSaving
}

data class GroupDetailUiState(
    val detail: GroupDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val message: String? = null,
    val tab: GroupDetailTab = GroupDetailTab.MEMBERS,
    val addMember: AddMemberForm? = null,
    val invitation: Invitation? = null,
    val confirmingDelete: Boolean = false,
    val isDeleting: Boolean = false,
    /** The group no longer exists: the organizer deleted it, here or on another phone. */
    val closed: Boolean = false,
    /** A member removed before the start, still shown as gone while the removal can be undone. */
    val pendingRemoval: Member? = null,
    /** A member of a started group whose dropout waits for the organizer's confirmation. */
    val confirmingDropout: Member? = null,
    val isRegisteringDropout: Boolean = false,
    val replace: ReplaceForm? = null,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupDetail: GetGroupDetailUseCase,
    private val addManualMember: AddManualMemberUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val registerDropout: RegisterDropoutUseCase,
    private val replaceMember: ReplaceMemberUseCase,
    private val getInvitation: GetInvitationUseCase,
    private val deleteGroup: DeleteGroupUseCase,
    incomingPushes: IncomingPushes,
) : ViewModel() {

    val groupId: String = savedStateHandle.toRoute<GroupDetailRoute>().groupId

    private val _state = MutableStateFlow(GroupDetailUiState())
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    init {
        // A push about this group, such as a member who joined, refreshes it on screen right away.
        viewModelScope.launch {
            incomingPushes.deepLinks.collect { link -> if (groupId in link) load() }
        }
    }

    /**
     * Called every time the screen comes back, while it waits for the group to start, and on each push
     * about the group, so members who join, turns assigned or a start elsewhere show up.
     */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.detail == null, errorMessage = null) }
            getGroupDetail(groupId)
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { error ->
                    if (error is ApiException && error.status == 404) {
                        _state.update { it.copy(isLoading = false, closed = true) }
                    } else {
                        _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) }
                    }
                }
        }
    }

    fun askToDelete() = _state.update { it.copy(confirmingDelete = true) }

    fun dismissDelete() = _state.update { it.copy(confirmingDelete = false) }

    fun delete() {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            deleteGroup(groupId)
                .onSuccess { _state.update { it.copy(isDeleting = false, confirmingDelete = false, closed = true) } }
                .onFailure { error ->
                    _state.update { it.copy(isDeleting = false, confirmingDelete = false, message = error.userMessage()) }
                }
        }
    }

    fun selectTab(tab: GroupDetailTab) = _state.update { it.copy(tab = tab) }

    fun openAddMember() = _state.update { it.copy(addMember = AddMemberForm()) }

    fun closeAddMember() = _state.update { it.copy(addMember = null) }

    fun onNewMemberName(value: String) =
        _state.update { it.copy(addMember = it.addMember?.copy(name = value.take(80), errorMessage = null)) }

    fun onNewMemberPhone(value: String) =
        _state.update { it.copy(addMember = it.addMember?.copy(phone = value.filter(Char::isDigit).take(9))) }

    fun saveNewMember() {
        val form = _state.value.addMember ?: return
        if (!form.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(addMember = form.copy(isSaving = true)) }
            addManualMember(groupId, form.name, form.phone)
                .onSuccess {
                    _state.update { it.copy(addMember = null, message = "${form.name.trim()} se agregó a la junta.") }
                    load()
                }
                .onFailure { error ->
                    _state.update { it.copy(addMember = form.copy(isSaving = false, errorMessage = error.userMessage())) }
                }
        }
    }

    /**
     * US15: the member disappears from the list right away, but the removal is only sent once the few seconds
     * to undo it are over; see [undoRemove] and [confirmRemove].
     */
    fun remove(member: Member) {
        _state.value.pendingRemoval?.let { confirmRemove() }
        _state.update { it.copy(pendingRemoval = member) }
    }

    fun undoRemove() = _state.update { it.copy(pendingRemoval = null) }

    fun confirmRemove() {
        val member = _state.value.pendingRemoval ?: return
        _state.update { it.copy(pendingRemoval = null) }
        viewModelScope.launch {
            removeMember(groupId, member.membershipId)
                .onSuccess { load() }
                .onFailure { error -> _state.update { it.copy(message = error.userMessage()) } }
        }
    }

    fun askDropout(member: Member) = _state.update { it.copy(confirmingDropout = member) }

    fun dismissDropout() = _state.update { it.copy(confirmingDropout = null) }

    /** US38: the member left the group in progress; their turn waits for a replacement. */
    fun confirmDropout() {
        val member = _state.value.confirmingDropout ?: return
        if (_state.value.isRegisteringDropout) return
        viewModelScope.launch {
            _state.update { it.copy(isRegisteringDropout = true) }
            registerDropout(groupId, member.membershipId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isRegisteringDropout = false,
                            confirmingDropout = null,
                            tab = GroupDetailTab.TURNS,
                            message = "${member.displayName} dejó la junta. Asigna a alguien en su turno.",
                        )
                    }
                    load()
                }
                .onFailure { error ->
                    _state.update { it.copy(isRegisteringDropout = false, confirmingDropout = null, message = error.userMessage()) }
                }
        }
    }

    fun openReplace(turn: Turn) =
        _state.update { it.copy(replace = ReplaceForm(turn.membershipId, turn.displayName, turn.turnNumber)) }

    fun closeReplace() = _state.update { it.copy(replace = null) }

    fun onReplaceName(value: String) =
        _state.update { it.copy(replace = it.replace?.copy(name = value.take(80), errorMessage = null)) }

    fun onReplacePhone(value: String) =
        _state.update { it.copy(replace = it.replace?.copy(phone = value.filter(Char::isDigit).take(9), errorMessage = null)) }

    /** US38: the replacement takes the turn; with the number of a Pozzo account they join with it. */
    fun saveReplacement() {
        val form = _state.value.replace ?: return
        if (!form.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(replace = form.copy(isSaving = true)) }
            replaceMember(groupId, form.droppedMembershipId, form.name, form.phone)
                .onSuccess {
                    _state.update { it.copy(replace = null, message = "${form.name.trim()} ocupa el turno ${form.turnNumber}.") }
                    load()
                }
                .onFailure { error ->
                    _state.update { it.copy(replace = form.copy(isSaving = false, errorMessage = error.userMessage())) }
                }
        }
    }

    fun invite() {
        viewModelScope.launch {
            getInvitation(groupId)
                .onSuccess { invitation -> _state.update { it.copy(invitation = invitation) } }
                .onFailure { error -> _state.update { it.copy(message = error.userMessage()) } }
        }
    }

    fun onInvitationHandled() = _state.update { it.copy(invitation = null) }

    fun onMessageShown() = _state.update { it.copy(message = null) }
}
