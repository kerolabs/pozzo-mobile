package pe.kerolabs.pozzo.features.compliancehistory.domain

import java.time.Instant
import kotlin.math.roundToInt

/**
 * How a member has met their contributions, counted by Pozzo from what was registered in the groups.
 */
data class ComplianceSummary(
    val level: String,
    val contributions: Int,
    val onTime: Int,
    val late: Int,
    val covered: Int,
    val rejected: Int,
    val dropouts: Int,
    val cyclesCompleted: Int,
) {
    /** Share of contributions made on time, from 0 to 100, or null while there is none. */
    val punctuality: Int? get() = if (contributions == 0) null else (onTime * 100.0 / contributions).roundToInt()
}

/** The history of the member in one group. */
data class GroupCompliance(
    val groupId: String,
    val groupName: String,
    val completed: Boolean,
    val onTime: Int,
    val contributions: Int,
)

/** G3: the summary across every group and the detail by group. */
data class MyHistory(val summary: ComplianceSummary, val groups: List<GroupCompliance>)

/** K2: how a member of a group of the organizer has met their contributions; null without the application. */
data class MemberCompliance(
    val membershipId: String,
    val displayName: String,
    val isOrganizer: Boolean,
    val usesApp: Boolean,
    val summary: ComplianceSummary?,
)

/** A public link to the summary of the member, valid for some days. */
data class ShareLink(val token: String, val url: String, val expiresAt: Instant)

interface ComplianceRepository {

    suspend fun getMyHistory(): Result<MyHistory>

    /** Only for the organizer of the group. */
    suspend fun getGroupCompliance(groupId: String): Result<List<MemberCompliance>>

    suspend fun shareMyHistory(): Result<ShareLink>
}
