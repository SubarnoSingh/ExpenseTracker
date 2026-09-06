package com.expensetracker.app.data.statement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Runs against a real Kotak export (identifying details redacted, layout intact):
 * 13 preamble lines, one Amount column beside a Dr/Cr marker, a second Dr/Cr for
 * the balance, and a trailer of closing balance and footnotes.
 */
class StatementParserTest {

    private val result by lazy {
        val text = checkNotNull(javaClass.classLoader?.getResourceAsStream("kotak_statement.csv"))
            .bufferedReader().use { it.readText() }
        StatementParser.parse(text, "kotak_statement.csv")
    }

    @Test
    fun `finds the table under the account preamble`() {
        assertNull(result.error)
        assertTrue("expected transactions, got none", result.candidates.isNotEmpty())
    }

    @Test
    fun `credits are not imported as spending`() {
        // 43 transactions: 9 CR rows (incoming UPI and NEFT settlements), 34 debits.
        assertEquals(9, result.credits)
        assertEquals(34, result.candidates.size)
        assertTrue(result.candidates.all { it.amount > 0 })
    }

    @Test
    fun `the balance Dr Cr column is not mistaken for the amount's`() {
        // Every balance in this file reads "CR". Reading that column would drop
        // every debit and import every credit - the exact inversion to guard.
        val incoming = result.candidates.filter { it.merchant.contains("SAGIRA", true) }
        assertTrue("a credit was imported as spending", incoming.isEmpty())
    }

    @Test
    fun `the closing balance and footnotes are not counted as failures`() {
        assertEquals(0, result.skipped)
    }

    @Test
    fun `upi descriptions yield the payee`() {
        val uber = result.candidates.single { it.merchant.contains("UBER", true) }
        assertEquals("UBER INDIA SYS", uber.merchant)
        assertEquals(1049.30, uber.amount, 0.001)
        assertEquals(LocalDate.of(2026, 9, 6), uber.date)
    }

    @Test
    fun `card descriptions yield the merchant, not the card number`() {
        val zudio = result.candidates.single { it.merchant.contains("ZUDIO", true) }
        assertEquals("ZUDIO A UNIT OF TRENT", zudio.merchant)
        assertEquals(942.0, zudio.amount, 0.001)

        val spotify = result.candidates.single { it.merchant.contains("SPOTIFY", true) }
        assertEquals("SPOTIFY SI", spotify.merchant)
    }

    @Test
    fun `transaction times are kept`() {
        val uber = result.candidates.single { it.merchant.contains("UBER", true) }
        assertEquals(19, uber.time.hour)
        assertEquals(8, uber.time.minute)
    }

    @Test
    fun `the upi reference is available for cross-source matching`() {
        val uber = result.candidates.single { it.merchant.contains("UBER", true) }
        // The same number the bank's SMS prints as "UPI Ref 190820883373".
        assertTrue(
            "expected the UPI ref in ${uber.references}",
            uber.references.contains("190820883373"),
        )
    }

    @Test
    fun `a file with no table says so instead of returning nothing`() {
        val result = StatementParser.parse("hello\nthere\n", "notes.csv")
        assertNotNull(result.error)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun `separate withdrawal and deposit columns still work`() {
        val hdfcStyle = """
            Date,Narration,Withdrawal Amt.,Deposit Amt.,Closing Balance
            06/09/26,UPI-SWIGGY-swiggy@icici,451.00,,10586.39
            06/09/26,SALARY CREDIT,,50000.00,60586.39
        """.trimIndent()
        val parsed = StatementParser.parse(hdfcStyle, "hdfc.csv")
        assertNull(parsed.error)
        assertEquals(1, parsed.candidates.size)
        assertEquals(451.0, parsed.candidates.first().amount, 0.001)
        assertEquals(1, parsed.credits)
    }
}
