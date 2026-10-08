package pe.kerolabs.pozzo.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Spanish: Locale = Locale.forLanguageTag("es-PE")
private val Symbols = DecimalFormatSymbols(Locale.US)

/**
 * Soles as the design shows them: "S/ 2,400" or "S/ 150.50".
 */
fun formatSoles(amount: BigDecimal): String {
    val scaled = amount.setScale(2, RoundingMode.HALF_UP)
    val whole = scaled.stripTrailingZeros().scale() <= 0
    val pattern = if (whole) "#,##0" else "#,##0.00"
    return "S/ " + DecimalFormat(pattern, Symbols).format(scaled)
}

/** "5 de ene". */
fun formatShortDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d 'de' MMM", Spanish).format(date).removeSuffix(".")

/** "5 nov 2026". */
fun formatMediumDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Spanish).format(date).replace(".", "")

/** "5 de enero de 2027". */
fun formatLongDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Spanish).format(date)
