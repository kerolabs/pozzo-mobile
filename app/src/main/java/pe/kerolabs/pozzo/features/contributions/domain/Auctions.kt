package pe.kerolabs.pozzo.features.contributions.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class AuctionStatus { OPEN, CLOSED }

/** Who collects when nobody bids: a draw among the members who have not collected, or the organizer's choice. */
enum class AuctionFallback { DRAW, ORGANIZER }

/** How the turn was given: to the highest bid, by draw, or to the member the organizer chose. */
enum class AuctionResolution { BID, DRAW, ORGANIZER }

/** A bid in the auction of a turn, with what the member would receive if it wins. */
data class AuctionBid(
    val membershipId: String,
    val displayName: String,
    val amount: BigDecimal,
    val receives: BigDecimal,
    val placedAt: Instant,
)

/**
 * The auction of the turn of a period: the members who have not collected offer a discount on the pot, and
 * the highest one collects. Every member sees every bid.
 */
data class Auction(
    val periodId: String,
    val turnNumber: Int,
    val status: AuctionStatus,
    val closesOn: LocalDate,
    val minimumBid: BigDecimal,
    val potAmount: BigDecimal,
    val fallback: AuctionFallback,
    val canBid: Boolean,
    val myBid: BigDecimal?,
    val myBidIsHighest: Boolean,
    val bids: List<AuctionBid>,
    val winnerMembershipId: String?,
    val winnerName: String?,
    val discount: BigDecimal?,
    val payoutAmount: BigDecimal?,
    val resolution: AuctionResolution?,
    val closedAt: Instant?,
) {
    val isOpen: Boolean get() = status == AuctionStatus.OPEN
    val highestBid: AuctionBid? get() = bids.firstOrNull()
}

interface AuctionRepository {

    /** The auction of a period, or a failure with status 404 when the turns of the group are not auctioned. */
    suspend fun getAuction(periodId: String): Result<Auction>

    /** Places or replaces the bid of the requester; only the latest bid of each member counts. */
    suspend fun placeBid(periodId: String, amount: BigDecimal): Result<Auction>

    /**
     * Closes the auction: the turn goes to the highest bid. Without bids it goes by draw, or to [winnerMembershipId]
     * when the organizer chooses, which is required when the group decided that the organizer chooses.
     */
    suspend fun closeAuction(periodId: String, winnerMembershipId: String?): Result<Auction>
}
