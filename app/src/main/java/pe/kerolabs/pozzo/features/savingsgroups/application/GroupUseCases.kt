package pe.kerolabs.pozzo.features.savingsgroups.application

import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupDetail
import pe.kerolabs.pozzo.features.savingsgroups.domain.Invitation
import pe.kerolabs.pozzo.features.savingsgroups.domain.InvitationCodes
import pe.kerolabs.pozzo.features.savingsgroups.domain.NewGroup
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroupRepository

class ObserveMyGroupsUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    operator fun invoke() = repository.observeMyGroups()
}

class ClearLocalGroupsUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke() = repository.clearLocalGroups()
}

class RefreshMyGroupsUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke() = repository.refreshMyGroups()
}

class CreateGroupUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    /** Creates the group and its first invitation, ready to share. */
    suspend operator fun invoke(group: NewGroup): Result<Pair<SavingsGroup, Invitation>> =
        repository.createGroup(group.copy(name = group.name.trim())).mapCatching { created ->
            created to repository.generateInvitation(created.id).getOrThrow()
        }
}

class PreviewInvitationUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(code: String) = repository.previewInvitation(InvitationCodes.normalize(code))
}

class JoinGroupUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(code: String) = repository.joinGroup(InvitationCodes.normalize(code))
}

class GetGroupDetailUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    /** The group, its members and its collection order, read together for the detail screen. */
    suspend operator fun invoke(groupId: String): Result<GroupDetail> = runCatching {
        coroutineScope {
            val group = async { repository.getGroup(groupId).getOrThrow() }
            val members = async { repository.getMembers(groupId).getOrThrow() }
            val turns = async { repository.getTurns(groupId).getOrThrow() }
            GroupDetail(group.await(), members.await(), turns.await())
        }
    }
}

class GetInvitationUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String) = repository.getOrGenerateInvitation(groupId)
}

class AddManualMemberUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String, displayName: String, phoneNumber: String?) =
        repository.addManualMember(groupId, displayName.trim(), phoneNumber?.takeIf { it.isNotBlank() })
}

class RemoveMemberUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String, membershipId: String) = repository.removeMember(groupId, membershipId)
}

class DrawTurnsUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String) = repository.drawTurns(groupId)
}

class AssignAgreedTurnsUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String, order: List<String>) = repository.assignAgreedTurns(groupId, order)
}

/** The photo of each member of a group, by membership; an empty map if it cannot be read. */
class GetMemberPhotosUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String): Map<String, String> =
        repository.getMembers(groupId).getOrNull().orEmpty()
            .mapNotNull { member -> member.photoUrl?.let { member.membershipId to it } }
            .toMap()
}

class GetTurnsUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String) = repository.getTurns(groupId)
}

class StartGroupUseCase @Inject constructor(private val repository: SavingsGroupRepository) {
    suspend operator fun invoke(groupId: String) = repository.startGroup(groupId)
}
