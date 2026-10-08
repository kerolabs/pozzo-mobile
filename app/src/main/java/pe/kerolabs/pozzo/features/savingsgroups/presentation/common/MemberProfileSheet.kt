package pe.kerolabs.pozzo.features.savingsgroups.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.components.SummaryCard
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.compliancehistory.application.GetMemberSummaryUseCase
import pe.kerolabs.pozzo.features.compliancehistory.domain.ComplianceSummary
import pe.kerolabs.pozzo.features.savingsgroups.application.GetMembersUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.Member
import pe.kerolabs.pozzo.features.savingsgroups.domain.MembershipKind

data class MemberProfileUiState(
    val member: Member? = null,
    val summary: ComplianceSummary? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * Reads a member of a group and, when the requester may see it (the member, or the organizer of one of
 * their groups), their punctuality across every group.
 */
@HiltViewModel
class MemberProfileViewModel @Inject constructor(
    private val getMembers: GetMembersUseCase,
    private val getMemberSummary: GetMemberSummaryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MemberProfileUiState())
    val state: StateFlow<MemberProfileUiState> = _state.asStateFlow()

    fun load(groupId: String, membershipId: String) {
        viewModelScope.launch {
            getMembers(groupId)
                .onSuccess { members ->
                    val member = members.firstOrNull { it.membershipId == membershipId }
                    _state.update { it.copy(member = member, isLoading = false) }
                    // The backend only answers to the member and to an organizer; anyone else sees no summary.
                    member?.accountId?.let { accountId ->
                        getMemberSummary(accountId).onSuccess { summary -> _state.update { it.copy(summary = summary) } }
                    }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) } }
        }
    }
}

/**
 * The profile of a member of a group, opened by tapping their photo or their row: the photo, the name,
 * their role, their turn and, for the organizer, their punctuality. [actions] adds what the screen allows,
 * such as removing the member or seeing a receipt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProfileSheet(
    groupId: String,
    membershipId: String,
    onDismiss: () -> Unit,
    actions: @Composable ColumnScope.(Member) -> Unit = {},
) {
    val viewModel: MemberProfileViewModel = hiltViewModel(key = "member-profile-$groupId-$membershipId")
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(membershipId) { viewModel.load(groupId, membershipId) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val member = state.member
            when {
                member != null -> MemberProfile(member, state.summary, actions)
                state.isLoading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> Text(
                    state.errorMessage ?: "Esta persona ya no es parte de la junta.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 32.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.MemberProfile(
    member: Member,
    summary: ComplianceSummary?,
    actions: @Composable ColumnScope.(Member) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    InitialsAvatar(name = member.displayName, size = 112.dp, photoUrl = member.photoUrl)
    Text(
        if (member.isMe) "${member.displayName} (tú)" else member.displayName,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (member.isOrganizer) StatusChip("Cabeza de junta", colors.secondaryContainer, colors.onSecondaryContainer)
        if (member.kind == MembershipKind.APP) {
            StatusChip("Usa Pozzo", colors.surfaceContainerHighest, colors.onSurfaceVariant)
        } else {
            StatusChip("Sin la app", status.warningContainer, status.onWarningContainer)
        }
    }
    val rows = buildList {
        member.turnNumber?.let { add("Turno de cobro" to "Turno $it") }
        member.phoneNumber?.let { add("Celular" to "+51 " + it.chunked(3).joinToString(" ")) }
        summary?.let {
            add("Aportes puntuales" to (it.punctuality?.let { value -> "$value %" } ?: "Nuevo, sin aportes"))
            if (it.contributions > 0) add("Aportes registrados" to "${it.onTime} de ${it.contributions} a tiempo")
            add("Ciclos completados" to it.cyclesCompleted.toString())
            if (it.dropouts > 0) add("Juntas abandonadas" to it.dropouts.toString())
        }
    }
    if (rows.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        SummaryCard(rows = rows)
    }
    if (summary != null && !member.isMe) {
        Text(
            "La puntualidad cuenta los aportes en todas sus juntas de Pozzo.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    actions(member)
}
