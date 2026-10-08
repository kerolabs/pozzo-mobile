package pe.kerolabs.pozzo.features.savingsgroups.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class PaymentMethod { YAPE, PLIN }

/**
 * Where the members send their contributions: a Yape or Plin wallet and its phone number.
 */
data class Destination(val method: PaymentMethod, val phoneNumber: String)

/**
 * The rules the organizer chooses for a new group.
 */
data class NewGroup(
    val name: String,
    val contributionAmount: BigDecimal,
    val periodicity: Periodicity,
    val seats: Int,
    val firstContributionDate: LocalDate,
    val destination: Destination?,
) {
    val potAmount: BigDecimal get() = contributionAmount.multiply(BigDecimal(seats))

    companion object {
        const val MIN_SEATS = 2
        const val MAX_SEATS = 50
        const val NAME_MAX_LENGTH = 80
    }
}

/**
 * The code and link a group shares to invite its members, e.g. "JB-7K4M".
 */
data class Invitation(val code: String, val link: String, val expiresAt: Instant)

/**
 * What someone sees before joining: the group and its rules, without the members or the destination.
 */
data class GroupPreview(
    val groupId: String,
    val name: String,
    val status: GroupStatus,
    val organizerName: String,
    val contributionAmount: BigDecimal,
    val periodicity: Periodicity,
    val seats: Int,
    val potAmount: BigDecimal,
    val firstContributionDate: LocalDate,
    val cutoffDay: Int,
    val membersCount: Int,
    val freeSeats: Int,
)

/**
 * An invitation code as people type it: two letters, a dash and four characters. Letters are made
 * uppercase and the dash is optional.
 */
object InvitationCodes {
    const val LENGTH = 6

    fun normalize(input: String): String = input.filter(Char::isLetterOrDigit).uppercase().take(LENGTH)

    fun display(normalized: String): String =
        if (normalized.length > 2) normalized.take(2) + "-" + normalized.drop(2) else normalized

    fun isComplete(normalized: String): Boolean = normalized.length == LENGTH
}
