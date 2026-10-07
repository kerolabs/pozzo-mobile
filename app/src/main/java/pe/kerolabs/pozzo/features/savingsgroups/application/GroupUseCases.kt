package pe.kerolabs.pozzo.features.savingsgroups.application

import javax.inject.Inject
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
