package pe.kerolabs.pozzo.features.savingsgroups.presentation.common

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import pe.kerolabs.pozzo.features.savingsgroups.domain.PaymentMethod
import pe.kerolabs.pozzo.features.savingsgroups.domain.Periodicity

private val Spanish: Locale = Locale.forLanguageTag("es-PE")

/** "Mensual". */
fun periodicityTitle(periodicity: Periodicity): String = when (periodicity) {
    Periodicity.WEEKLY -> "Semanal"
    Periodicity.BIWEEKLY -> "Quincenal"
    Periodicity.MONTHLY -> "Mensual"
}

/** "mensual", for sentences such as "Aporte mensual". */
fun periodicityAdjective(periodicity: Periodicity): String = periodicityTitle(periodicity).lowercase(Spanish)

/**
 * When the contributions are due, derived from the first contribution date as the backend does:
 * the same day of the month, or the same day of the week.
 */
fun cutoffLabel(periodicity: Periodicity, firstContributionDate: LocalDate): String {
    val weekday = firstContributionDate.dayOfWeek.getDisplayName(TextStyle.FULL, Spanish)
    return when (periodicity) {
        Periodicity.MONTHLY -> "Día ${firstContributionDate.dayOfMonth} de cada mes"
        Periodicity.WEEKLY -> "Cada $weekday"
        Periodicity.BIWEEKLY -> "Un $weekday de por medio"
    }
}

fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.YAPE -> "Yape"
    PaymentMethod.PLIN -> "Plin"
}
