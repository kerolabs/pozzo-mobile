package pe.kerolabs.pozzo.features.savingsgroups.domain

import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

enum class GroupStatus { DRAFT, READY, STARTED, CLOSED }

enum class GroupRole { ORGANIZER, PARTICIPANT }

enum class Periodicity { WEEKLY, BIWEEKLY, MONTHLY }

enum class TurnMethod { DRAW, AGREED, AUCTION }

/** Who collects when nobody bids in the auction of a turn: a draw, or the member the organizer chooses. */
enum class AuctionFallback { DRAW, ORGANIZER }

/**
 * How the turns of a group are auctioned: how many days before each cutoff the auction closes, the smallest
 * discount accepted and what happens when nobody bids.
 */
data class AuctionRules(
    val closesDaysBeforeCutoff: Int,
    val minimumBid: BigDecimal,
    val fallback: AuctionFallback,
)

/**
 * Conditions to start a group.
 */
data class Readiness(
    val groupFull: Boolean,
    val turnsAssigned: Boolean,
    val destinationDefined: Boolean,
    val canStart: Boolean,
)

/**
 * A savings group the member belongs to.
 */
data class SavingsGroup(
    val id: String,
    val name: String,
    val status: GroupStatus,
    val role: GroupRole,
    val organizerName: String,
    val contributionAmount: BigDecimal,
    val currency: String,
    val periodicity: Periodicity,
    val seats: Int,
    val potAmount: BigDecimal,
    val firstContributionDate: LocalDate,
    val destination: Destination?,
    val turnMethod: TurnMethod?,
    val myTurnNumber: Int?,
    val myTurnDate: LocalDate?,
    val readiness: Readiness,
) {
    val isOrganizer: Boolean get() = role == GroupRole.ORGANIZER
    val isStarted: Boolean get() = status == GroupStatus.STARTED || status == GroupStatus.CLOSED
}

enum class MembershipKind { APP, MANUAL }

/**
 * A member of a group: someone with the application, or someone the organizer registered by hand.
 */
data class Member(
    val membershipId: String,
    val displayName: String,
    val kind: MembershipKind,
    val isOrganizer: Boolean,
    val isMe: Boolean,
    val phoneNumber: String?,
    val turnNumber: Int?,
    val photoUrl: String? = null,
    /** The account of a member who uses the application, to read their punctuality. */
    val accountId: String? = null,
)

/**
 * One turn of the collection order: who collects and the cutoff date of that period.
 */
data class Turn(
    val turnNumber: Int,
    val membershipId: String,
    val displayName: String,
    val cutoffDate: LocalDate,
    val isMe: Boolean,
    val photoUrl: String? = null,
)

/**
 * The collection order of a group and how it was decided.
 */
data class TurnCalendar(
    val method: TurnMethod?,
    val drawSeed: String?,
    val assignedAt: java.time.Instant?,
    val potAmount: BigDecimal,
    val turns: List<Turn>,
)

interface SavingsGroupRepository {

    /** The groups kept on the phone, updated every time [refreshMyGroups] succeeds. */
    fun observeMyGroups(): Flow<List<SavingsGroup>>

    /** Downloads the groups of the member and replaces the local copy. */
    suspend fun refreshMyGroups(): Result<Unit>

    /** Removes the local copy, e.g. when the member signs out. */
    suspend fun clearLocalGroups()

    /** Creates a group with the requester as organizer. */
    suspend fun createGroup(group: NewGroup): Result<SavingsGroup>

    /** Generates a new invitation for a group of the organizer; the previous one stops working. */
    suspend fun generateInvitation(groupId: String): Result<Invitation>

    /** The invitation in force, or a new one when there is none. */
    suspend fun getOrGenerateInvitation(groupId: String): Result<Invitation>

    /** The group behind an invitation code, before joining it. */
    suspend fun previewInvitation(code: String): Result<GroupPreview>

    /** Joins the group of an invitation code. */
    suspend fun joinGroup(code: String): Result<SavingsGroup>

    suspend fun getGroup(groupId: String): Result<SavingsGroup>

    suspend fun getMembers(groupId: String): Result<List<Member>>

    suspend fun getTurns(groupId: String): Result<TurnCalendar>

    /** Adds a member who will not use the application; the organizer registers their contributions. */
    suspend fun addManualMember(groupId: String, displayName: String, phoneNumber: String?): Result<List<Member>>

    suspend fun removeMember(groupId: String, membershipId: String): Result<Unit>

    /** Records that a member left a group in progress; their turn waits for a replacement. */
    suspend fun registerDropout(groupId: String, membershipId: String): Result<List<Member>>

    /**
     * Gives the turn of a member who dropped out to a replacement: with the number of a Pozzo account they join
     * with it, otherwise as a member without the application.
     */
    suspend fun replaceMember(
        groupId: String,
        droppedMembershipId: String,
        displayName: String,
        phoneNumber: String?,
    ): Result<List<Member>>

    /** Draws the collection order; drawing again replaces the previous result until the group starts. */
    suspend fun drawTurns(groupId: String): Result<TurnCalendar>

    /** Sets the collection order agreed by the group: the first membership collects first. */
    suspend fun assignAgreedTurns(groupId: String, order: List<String>): Result<TurnCalendar>

    /**
     * Auctions the turns: each period opens an auction among the members who have not collected. The order it
     * returns is provisional until each auction closes.
     */
    suspend fun assignTurnsByAuction(groupId: String, rules: AuctionRules): Result<TurnCalendar>

    /** Starts the group: its rules freeze and the first period opens. */
    suspend fun startGroup(groupId: String): Result<SavingsGroup>

    /**
     * Changes the name, the rules and the destination of a group that has not started. Changing the number
     * of members discards the turns already assigned.
     */
    suspend fun updateGroup(groupId: String, group: NewGroup): Result<SavingsGroup>

    /** Deletes a group that has not started, with its members and invitation. */
    suspend fun deleteGroup(groupId: String): Result<Unit>

    /**
     * Opens the next cycle of a closed group: a new group, still to start, with the same name, rules,
     * destination and members, whose first contribution is on [firstContributionDate].
     */
    suspend fun openNewCycle(groupId: String, firstContributionDate: LocalDate): Result<SavingsGroup>
}

/**
 * Everything the detail of a group shows.
 */
data class GroupDetail(val group: SavingsGroup, val members: List<Member>, val turns: TurnCalendar) {
    val activeMembers: Int get() = members.size
}

/**
 * Cutoff date of a turn: the first contribution date moved one period per turn, as the backend computes it.
 */
fun SavingsGroup.cutoffDateOfTurn(turnNumber: Int): LocalDate {
    val periods = (turnNumber - 1).toLong()
    return when (periodicity) {
        Periodicity.WEEKLY -> firstContributionDate.plusWeeks(periods)
        Periodicity.BIWEEKLY -> firstContributionDate.plusWeeks(2 * periods)
        Periodicity.MONTHLY -> firstContributionDate.plusMonths(periods)
    }
}
