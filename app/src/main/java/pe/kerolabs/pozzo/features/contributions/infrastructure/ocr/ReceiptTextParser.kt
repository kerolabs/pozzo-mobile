package pe.kerolabs.pozzo.features.contributions.infrastructure.ocr

import java.math.BigDecimal
import java.time.LocalDate
import pe.kerolabs.pozzo.features.contributions.domain.ReadReceipt
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptSource

/**
 * Finds the four fields Pozzo checks in the text of a Yape, Plin or bank receipt: amount, date,
 * recipient and operation number. It is deliberately forgiving: the member confirms and corrects
 * every field before registering.
 */
object ReceiptTextParser {

    private val months = mapOf(
        "ene" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "ago" to 8, "set" to 9, "sep" to 9, "oct" to 10, "nov" to 11, "dic" to 12,
    )

    private val amountPattern = Regex("""S/\s*([0-9]{1,3}(?:[,\s][0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""")
    private val wordDatePattern = Regex("""(\d{1,2})\s+(?:de\s+)?([a-záéíóú]{3})[a-záéíóú]*\.?\s+(?:de\s+)?(\d{4})""", RegexOption.IGNORE_CASE)
    private val numericDatePattern = Regex("""(\d{1,2})/(\d{1,2})/(\d{2,4})""")
    private val operationDigits = Regex("""\d{6,12}""")
    private val payeeLabels = listOf("enviado a", "destino", "para", "destinatario", "beneficiario", "a nombre de")

    /** Words that follow a payee label but name the wallet or the bank, not the person ("Destino: Yape"). */
    private val notPayees = listOf("yape", "plin", "bcp", "interbank", "bbva", "scotiabank", "banco", "celular")

    fun parse(text: String): ReadReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lower = text.lowercase()
        return ReadReceipt(
            amount = amountOf(text),
            paidAt = dateOf(text),
            payeeName = payeeOf(lines),
            operationNumber = operationOf(lines),
            source = when {
                "yape" in lower -> ReceiptSource.YAPE
                "plin" in lower -> ReceiptSource.PLIN
                lines.isNotEmpty() -> ReceiptSource.BANK
                else -> null
            },
        )
    }

    private fun amountOf(text: String): BigDecimal? =
        amountPattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").replace(" ", "").toBigDecimalOrNull() }
            .maxOrNull()

    private fun dateOf(text: String): LocalDate? {
        wordDatePattern.find(text)?.let { match ->
            val month = months[match.groupValues[2].lowercase().take(3)] ?: return@let
            return runCatching { LocalDate.of(match.groupValues[3].toInt(), month, match.groupValues[1].toInt()) }.getOrNull()
        }
        numericDatePattern.find(text)?.let { match ->
            val year = match.groupValues[3].toInt().let { if (it < 100) 2000 + it else it }
            return runCatching { LocalDate.of(year, match.groupValues[2].toInt(), match.groupValues[1].toInt()) }.getOrNull()
        }
        return null
    }

    private fun operationOf(lines: List<String>): String? {
        val index = lines.indexOfFirst { "operaci" in it.lowercase() }
        if (index >= 0) {
            val sameLine = operationDigits.find(lines[index])?.value
            if (sameLine != null) return sameLine
            lines.getOrNull(index + 1)?.let { next -> operationDigits.find(next)?.value?.let { return it } }
        }
        // Without a label, the longest run of digits that is not a phone number.
        return lines.flatMap { line -> operationDigits.findAll(line).map { it.value }.toList() }
            .filterNot { it.length == 9 && it.startsWith("9") }
            .maxByOrNull { it.length }
    }

    private fun payeeOf(lines: List<String>): String? {
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase()
            val label = payeeLabels.firstOrNull { lower.startsWith(it) } ?: return@forEachIndexed
            val rest = line.drop(label.length).trim(' ', ':')
            val candidate = if (rest.length >= 3) rest else lines.getOrNull(index + 1).orEmpty()
            if (looksLikeName(candidate)) return candidate
        }
        // Yape shows the recipient without a label, on the line right after the amount.
        val amountLine = lines.indexOfFirst { amountPattern.containsMatchIn(it) }
        return lines.drop(amountLine + 1).take(2).firstOrNull(::looksLikeName).takeIf { amountLine >= 0 }
    }

    private fun looksLikeName(value: String): Boolean {
        val lower = value.lowercase()
        return value.count(Char::isLetter) >= 3 &&
            value.none(Char::isDigit) &&
            notPayees.none { lower == it || lower.startsWith("$it ") } &&
            payeeLabels.none { lower.startsWith(it) } &&
            "operaci" !in lower
    }
}
