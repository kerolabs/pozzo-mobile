package pe.kerolabs.pozzo.features.contributions.presentation.auction

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import pe.kerolabs.pozzo.features.contributions.application.CloseAuctionUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetCycleOfGroupUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetTurnAuctionUseCase
import pe.kerolabs.pozzo.features.contributions.application.PlaceBidUseCase
import pe.kerolabs.pozzo.features.contributions.domain.AuctionStatus
import pe.kerolabs.pozzo.features.contributions.testing.FakeAuctionRepository
import pe.kerolabs.pozzo.features.contributions.testing.FakeContributionRepository
import pe.kerolabs.pozzo.features.contributions.testing.GROUP_ID
import pe.kerolabs.pozzo.features.contributions.testing.MainDispatcherRule
import pe.kerolabs.pozzo.features.contributions.testing.PERIOD_ID
import pe.kerolabs.pozzo.features.contributions.testing.auction
import pe.kerolabs.pozzo.features.contributions.testing.noConnection
import pe.kerolabs.pozzo.features.contributions.testing.ruleBroken
import pe.kerolabs.pozzo.features.contributions.testing.soles
import pe.kerolabs.pozzo.features.savingsgroups.application.GetTurnsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroupRepository

/** The auction of the turn: bidding as a member and closing it as the organizer. */
class AuctionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val contributions = FakeContributionRepository()
    private val auctions = FakeAuctionRepository()

    private fun bidding() = AuctionViewModel(
        SavedStateHandle(mapOf("groupId" to GROUP_ID)),
        GetTurnAuctionUseCase(contributions, auctions),
        PlaceBidUseCase(auctions),
    ).apply { load() }

    private fun closing() = CloseAuctionViewModel(
        GetCycleOfGroupUseCase(contributions),
        GetTurnsUseCase(mock(SavingsGroupRepository::class.java)),
        CloseAuctionUseCase(auctions),
    )

    @Test
    fun `shows the auction of the turn in progress`() {
        val vm = bidding()

        assertEquals(PERIOD_ID, vm.state.value.turnAuction!!.auction.periodId)
    }

    @Test
    fun `a bid written with a comma is sent as an amount and tells when it is the highest`() {
        auctions.bidResult = Result.success(auction(myBid = soles("12.50"), myBidIsHighest = true))
        val vm = bidding()

        vm.onBidChange("12,50")
        vm.bid()

        assertEquals(0, soles("12.50").compareTo(auctions.bids.single()))
        assertEquals("Tu oferta es la mayor por ahora.", vm.state.value.message)
        assertEquals("", vm.state.value.bid)
    }

    @Test
    fun `a bid that is not the highest is still recorded`() {
        auctions.bidResult = Result.success(auction(myBid = soles("15"), myBidIsHighest = false))
        val vm = bidding()

        vm.onBidChange("15")
        vm.bid()

        assertEquals("Registramos tu oferta.", vm.state.value.message)
    }

    @Test
    fun `a bid below the minimum shows the reason and keeps what was written`() {
        auctions.bidResult = Result.failure(ruleBroken("BID_BELOW_MINIMUM", "La oferta mínima es S/ 10."))
        val vm = bidding()

        vm.onBidChange("5")
        vm.bid()

        assertEquals("La oferta mínima es S/ 10.", vm.state.value.errorMessage)
        assertEquals("5", vm.state.value.bid)
    }

    @Test
    fun `an empty bid is not sent`() {
        val vm = bidding()

        vm.onBidChange("0")
        vm.bid()

        assertTrue(auctions.bids.isEmpty())
    }

    @Test
    fun `closing hands back the auction with its winner`() {
        val vm = closing()

        vm.close(PERIOD_ID, null)

        assertEquals(listOf<String?>(null), auctions.closes)
        assertEquals(AuctionStatus.CLOSED, vm.state.value.closed!!.status)
        assertEquals("m-sofia", vm.state.value.closed!!.winnerMembershipId)
    }

    @Test
    fun `when nobody bid the organizer's choice is sent`() {
        val vm = closing()

        vm.onWinnerSelected("m-luis")
        vm.close(PERIOD_ID, "m-luis")

        assertEquals(listOf<String?>("m-luis"), auctions.closes)
    }

    @Test
    fun `a close that fails leaves the auction open and says why`() {
        auctions.closeResult = Result.failure(noConnection())
        val vm = closing()

        vm.close(PERIOD_ID, null)

        assertNull(vm.state.value.closed)
        assertEquals("No pudimos conectarnos. Revisa tu conexión e inténtalo de nuevo.", vm.state.value.errorMessage)
    }
}
