package pe.kerolabs.pozzo.features.compliancehistory.infrastructure.repositories

import java.time.Instant
import javax.inject.Inject
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.compliancehistory.domain.ComplianceRepository
import pe.kerolabs.pozzo.features.compliancehistory.domain.ComplianceSummary
import pe.kerolabs.pozzo.features.compliancehistory.domain.GroupCompliance
import pe.kerolabs.pozzo.features.compliancehistory.domain.MemberCompliance
import pe.kerolabs.pozzo.features.compliancehistory.domain.MyHistory
import pe.kerolabs.pozzo.features.compliancehistory.domain.ShareLink
import pe.kerolabs.pozzo.features.compliancehistory.infrastructure.remote.ComplianceService
import pe.kerolabs.pozzo.features.compliancehistory.infrastructure.remote.ComplianceSummaryDto

class ComplianceRepositoryImpl @Inject constructor(
    private val service: ComplianceService,
) : ComplianceRepository {

    override suspend fun getMyHistory(): Result<MyHistory> =
        apiCall { service.getMyHistory() }.map { dto ->
            MyHistory(
                summary = dto.summary.toDomain(),
                groups = dto.groups.map {
                    GroupCompliance(it.groupId, it.groupName, it.status == "COMPLETED", it.onTime, it.contributions)
                },
            )
        }

    override suspend fun getGroupCompliance(groupId: String): Result<List<MemberCompliance>> =
        apiCall { service.getGroupCompliance(groupId) }.map { list ->
            list.map { MemberCompliance(it.membershipId, it.displayName, it.organizer, it.usesApp, it.summary?.toDomain()) }
        }

    override suspend fun shareMyHistory(): Result<ShareLink> =
        apiCall { service.shareMyHistory() }.map { ShareLink(it.token, it.url, Instant.parse(it.expiresAt)) }

    private fun ComplianceSummaryDto.toDomain() =
        ComplianceSummary(level, contributions, onTime, late, covered, rejected, dropouts, cyclesCompleted)
}
