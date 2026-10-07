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
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.savingsgroups.application.AddManualMemberUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.GetGroupDetailUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.GetInvitationUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.RemoveMemberUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupDetail
import pe.kerolabs.pozzo.features.savingsgroups.domain.Invitation
import pe.kerolabs.pozzo.features.savingsgroups.domain.Member
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

data class GroupDetailUiState(
    val detail: GroupDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val message: String? = null,
    val tab: GroupDetailTab = GroupDetailTab.MEMBERS,
    val addMember: AddMemberForm? = null,
    val invitation: Invitation? = null,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupDetail: GetGroupDetailUseCase,
    private val addManualMember: AddManualMemberUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val getInvitation: GetInvitationUseCase,
) : ViewModel() {

    val groupId: String = savedStateHandle.toRoute<GroupDetailRoute>().groupId

    private val _state = MutableStateFlow(GroupDetailUiState())
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    /** Called every time the screen comes back, so turns assigned or a start elsewhere show up. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.detail == null, errorMessage = null) }
            getGroupDetail(groupId)
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
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

    fun remove(member: Member) {
        viewModelScope.launch {
            removeMember(groupId, member.membershipId)
                .onSuccess {
                    _state.update { it.copy(message = "${member.displayName} ya no es parte de la junta.") }
                    load()
                }
                .onFailure { error -> _state.update { it.copy(message = error.userMessage()) } }
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
