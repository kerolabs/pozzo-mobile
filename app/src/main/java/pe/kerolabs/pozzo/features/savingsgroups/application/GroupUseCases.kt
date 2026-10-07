package pe.kerolabs.pozzo.features.savingsgroups.application

import javax.inject.Inject
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
