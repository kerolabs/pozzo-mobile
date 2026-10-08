package pe.kerolabs.pozzo.features.contributions.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** The member's contribution in one period, with the receipt when there is one. */
data class MyPeriodContribution(
    val periodId: String,
    val turnNumber: Int,
    val cutoffDate: LocalDate,
    val amount: BigDecimal,
    val state: ContributionState,
    val contribution: Contribution?,
)

/**
 * G1: the member's contributions in a cycle, the most recent period first. When the backend cannot be
 * reached it is the last copy kept on the phone, and [savedAt] says when it was saved.
 */
data class MyContributions(
    val cycleId: String,
    val groupName: String,
    val contributedAmount: BigDecimal,
    val pendingAmount: BigDecimal,
    val periods: List<MyPeriodContribution>,
    val savedAt: Instant,
    val isOffline: Boolean,
)
