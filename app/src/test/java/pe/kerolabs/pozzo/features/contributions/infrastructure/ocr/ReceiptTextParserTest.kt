package pe.kerolabs.pozzo.features.contributions.infrastructure.ocr

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptSource

class ReceiptTextParserTest {

    @Test
    fun `reads a Yape receipt`() {
        val text = """
            ¡Yapeaste!
            S/ 300
            Anna Weber
            31 dic. 2026 - 07:42 p. m.
            Nro. de celular *** *** 123
            Destino: Yape
            Nro. de operación
            04581273
        """.trimIndent()

        val read = ReceiptTextParser.parse(text)

        assertEquals(0, BigDecimal("300").compareTo(read.amount))
        assertEquals(LocalDate.of(2026, 12, 31), read.paidAt)
        assertEquals("Anna Weber", read.payeeName)
        assertEquals("04581273", read.operationNumber)
        assertEquals(ReceiptSource.YAPE, read.source)
    }

    @Test
    fun `reads a Plin receipt with the recipient on its own line`() {
        val text = """
            Constancia de pago
            S/ 1,250.50
            Enviado a
            Anna Weber R.
            Fecha 05/01/2027
            N.° de operación 05129846
            plin
        """.trimIndent()

        val read = ReceiptTextParser.parse(text)

        assertEquals(0, BigDecimal("1250.50").compareTo(read.amount))
        assertEquals(LocalDate.of(2027, 1, 5), read.paidAt)
        assertEquals("Anna Weber R.", read.payeeName)
        assertEquals("05129846", read.operationNumber)
        assertEquals(ReceiptSource.PLIN, read.source)
    }

    @Test
    fun `leaves unknown fields empty`() {
        val read = ReceiptTextParser.parse("Gracias por tu compra")

        assertNull(read.amount)
        assertNull(read.paidAt)
        assertNull(read.operationNumber)
    }
}
