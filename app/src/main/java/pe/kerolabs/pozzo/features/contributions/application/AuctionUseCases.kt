package pe.kerolabs.pozzo.features.contributions.application

import java.math.BigDecimal
import javax.inject.Inject
import pe.kerolabs.pozzo.features.contributions.domain.Auction
import pe.kerolabs.pozzo.features.contributions.domain.AuctionRepository
import pe.kerolabs.pozzo.features.contributions.domain.ContributionRepository
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period

/** The cycle of a group, its period in progress and the auction of that period. */
data class TurnAuction(val cycle: Cycle, val period: Period, val auction: Auction)

/**
 * US20: the auction of the turn in progress of a group, read with its cycle and period so the screen knows
 * who collects and how much the pot is.
 */
class GetTurnAuctionUseCase @Inject constructor(
    private val contributions: ContributionRepository,
    private val auctions: AuctionRepository,
) {
    suspend operator fun invoke(groupId: String): Result<TurnAuction> =
        contributions.getCycleOfGroup(groupId).mapCatching { cycle ->
            val period = contributions.getCurrentPeriod(cycle.id).getOrThrow()
            TurnAuction(cycle, period, auctions.getAuction(period.id).getOrThrow())
        }
}

/**
 * The auction of a period when its group auctions the turns, or null when it does not (or it cannot be read
 * now), so the pot offers the auction only where there is one.
 */
class FindAuctionUseCase @Inject constructor(private val auctions: AuctionRepository) {
    suspend operator fun invoke(periodId: String): Auction? = auctions.getAuction(periodId).getOrNull()
}

class PlaceBidUseCase @Inject constructor(private val auctions: AuctionRepository) {
    suspend operator fun invoke(periodId: String, amount: BigDecimal) = auctions.placeBid(periodId, amount)
}
