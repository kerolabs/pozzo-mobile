package pe.kerolabs.pozzo.features.compliancehistory.application

import javax.inject.Inject
import pe.kerolabs.pozzo.features.compliancehistory.domain.ComplianceRepository

class GetMyHistoryUseCase @Inject constructor(private val repository: ComplianceRepository) {
    suspend operator fun invoke() = repository.getMyHistory()
}

class GetGroupComplianceUseCase @Inject constructor(private val repository: ComplianceRepository) {
    suspend operator fun invoke(groupId: String) = repository.getGroupCompliance(groupId)
}

class ShareMyHistoryUseCase @Inject constructor(private val repository: ComplianceRepository) {
    suspend operator fun invoke() = repository.shareMyHistory()
}

class GetMemberSummaryUseCase @Inject constructor(private val repository: ComplianceRepository) {
    suspend operator fun invoke(accountId: String) = repository.getMemberSummary(accountId)
}
