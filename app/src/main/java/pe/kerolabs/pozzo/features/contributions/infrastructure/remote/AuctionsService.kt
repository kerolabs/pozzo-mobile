package pe.kerolabs.pozzo.features.contributions.infrastructure.remote

import java.math.BigDecimal
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit client for the auction of the turn of each period, in groups whose turns are auctioned.
 */
interface AuctionsService {

    @GET("periods/{periodId}/auction")
    suspend fun getAuction(@Path("periodId") periodId: String): Response<AuctionDto>

    @POST("periods/{periodId}/auction/bids")
    suspend fun placeBid(@Path("periodId") periodId: String, @Body request: PlaceBidRequestDto): Response<AuctionDto>
}

data class AuctionDto(
    val periodId: String,
    val turnNumber: Int,
    val status: String,
    val closesOn: String,
    val minimumBid: BigDecimal,
    val potAmount: BigDecimal,
    val fallback: String,
    val canBid: Boolean,
    val myBid: BigDecimal?,
    val myBidIsHighest: Boolean,
    val bids: List<AuctionBidDto>,
    val winnerMembershipId: String?,
    val winnerName: String?,
    val discount: BigDecimal?,
    val payoutAmount: BigDecimal?,
    val resolution: String?,
    val closedAt: String?,
)

data class AuctionBidDto(
    val membershipId: String,
    val displayName: String,
    val amount: BigDecimal,
    val receives: BigDecimal,
    val placedAt: String,
)

data class PlaceBidRequestDto(val amount: BigDecimal)
