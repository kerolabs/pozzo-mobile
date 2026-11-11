package pe.kerolabs.pozzo.features.contributions.infrastructure.repositories

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import pe.kerolabs.pozzo.core.network.apiCall
import pe.kerolabs.pozzo.features.contributions.domain.Auction
import pe.kerolabs.pozzo.features.contributions.domain.AuctionBid
import pe.kerolabs.pozzo.features.contributions.domain.AuctionFallback
import pe.kerolabs.pozzo.features.contributions.domain.AuctionRepository
import pe.kerolabs.pozzo.features.contributions.domain.AuctionResolution
import pe.kerolabs.pozzo.features.contributions.domain.AuctionStatus
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.AuctionDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.AuctionsService
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.CloseAuctionRequestDto
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.PlaceBidRequestDto

/**
 * [AuctionRepository] over the auction endpoints. Auctions are not kept on the phone: they change with every
 * bid, so they are always read from the backend.
 */
class AuctionRepositoryImpl @Inject constructor(private val service: AuctionsService) : AuctionRepository {

    override suspend fun getAuction(periodId: String): Result<Auction> =
        apiCall { service.getAuction(periodId) }.map { it.toDomain() }

    override suspend fun placeBid(periodId: String, amount: BigDecimal): Result<Auction> =
        apiCall { service.placeBid(periodId, PlaceBidRequestDto(amount)) }.map { it.toDomain() }

    override suspend fun closeAuction(periodId: String, winnerMembershipId: String?): Result<Auction> =
        apiCall { service.closeAuction(periodId, CloseAuctionRequestDto(winnerMembershipId)) }.map { it.toDomain() }
}

internal fun AuctionDto.toDomain() = Auction(
    periodId = periodId,
    turnNumber = turnNumber,
    status = AuctionStatus.valueOf(status),
    closesOn = LocalDate.parse(closesOn),
    minimumBid = minimumBid,
    potAmount = potAmount,
    fallback = AuctionFallback.valueOf(fallback),
    canBid = canBid,
    myBid = myBid,
    myBidIsHighest = myBidIsHighest,
    bids = bids.map { AuctionBid(it.membershipId, it.displayName, it.amount, it.receives, Instant.parse(it.placedAt)) },
    winnerMembershipId = winnerMembershipId,
    winnerName = winnerName,
    discount = discount,
    payoutAmount = payoutAmount,
    resolution = resolution?.let(AuctionResolution::valueOf),
    closedAt = closedAt?.let(Instant::parse),
)
