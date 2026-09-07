package com.expensetracker.app.data.imports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DuplicateGuardTest {

    private fun candidate(
        id: String,
        amount: Double,
        day: Int,
        merchant: String,
        refs: Set<String> = emptySet(),
        hour: Int = 12,
        source: ImportSource = ImportSource.STATEMENT,
    ) = ImportCandidate(
        id = id,
        amount = amount,
        merchant = merchant,
        date = LocalDate.of(2026, 9, day),
        time = LocalTime.of(hour, 0),
        source = source,
        sourceLabel = "test",
        body = merchant,
        references = refs,
    )

    private fun saved(
        amount: Double,
        day: Int,
        merchant: String,
        refs: Set<String> = emptySet(),
        hour: Int = 12,
    ) = SavedTransaction(
        amount = amount,
        date = LocalDate.of(2026, 9, day),
        merchant = merchant,
        origin = "SMS",
        time = LocalTime.of(hour, 0),
        references = refs,
    )

    @Test
    fun `a shared upi reference is a match whatever the wording`() {
        val flags = DuplicateGuard.flag(
            listOf(candidate("a", 1049.30, 6, "UBER INDIA SYS", setOf("190820883373"))),
            listOf(saved(1049.30, 6, "UBER INDIA SYSTEMS P", setOf("190820883373"))),
        )
        assertTrue(flags["a"].orEmpty().contains("Same reference"))
    }

    @Test
    fun `a reference matches even when the bank posts on a different day`() {
        val flags = DuplicateGuard.flag(
            listOf(candidate("a", 500.0, 20, "SOMEONE", setOf("624477989786"))),
            listOf(saved(500.0, 1, "SOMEONE ELSE", setOf("624477989786"))),
        )
        assertNotNull(flags["a"])
    }

    @Test
    fun `card payments have no reference and still match on amount and date`() {
        // The card SMS quotes no reference, so this is the only signal available.
        val flags = DuplicateGuard.flag(
            listOf(candidate("a", 505.0, 6, "BAAZAR RETAIL PVT  LTD")),
            listOf(saved(505.0, 6, "BAAZAR RETAIL P HOWRAH")),
        )
        assertTrue(flags["a"].orEmpty().contains("Already recorded"))
    }

    @Test
    fun `a truncated bank name still counts as the same payee`() {
        assertTrue(DuplicateGuard.sharesName("UBER INDIA SYS", "UBER INDIA SYSTEMS P"))
        assertTrue(DuplicateGuard.sharesName("BAAZAR RETAIL PVT LTD", "BAAZAR RETAIL P HOWRAH"))
    }

    @Test
    fun `unrelated payees of the same amount are not treated as the same payee`() {
        assertTrue(!DuplicateGuard.sharesName("ZUDIO A UNIT OF TRENT", "SPOTIFY SI"))
    }

    @Test
    fun `one saved entry cannot account for several payments`() {
        // Three real ₹500 payments, one already saved: exactly one is a duplicate.
        val flags = DuplicateGuard.flag(
            listOf(
                candidate("a", 500.0, 4, "SHOP A", hour = 9),
                candidate("b", 500.0, 4, "SHOP B", hour = 14),
                candidate("c", 500.0, 4, "SHOP C", hour = 20),
            ),
            listOf(saved(500.0, 4, "SHOP B", hour = 14)),
        )
        assertEquals("expected exactly one duplicate, got $flags", 1, flags.size)
        assertNotNull("the matching payee should be the one flagged", flags["b"])
    }

    @Test
    fun `time picks the closest of several identical amounts`() {
        val flags = DuplicateGuard.flag(
            listOf(
                candidate("early", 200.0, 5, "UNKNOWN", hour = 8),
                candidate("late", 200.0, 5, "UNKNOWN", hour = 22),
            ),
            listOf(saved(200.0, 5, "SOMETHING ELSE", hour = 21)),
        )
        assertEquals(1, flags.size)
        assertNotNull(flags["late"])
    }

    @Test
    fun `a genuinely new payment is not flagged`() {
        val flags = DuplicateGuard.flag(
            listOf(candidate("a", 942.0, 4, "ZUDIO A UNIT OF TRENT")),
            listOf(saved(505.0, 4, "BAAZAR RETAIL"), saved(942.0, 20, "ZUDIO")),
        )
        assertNull(flags["a"])
    }

    @Test
    fun `the same charge listed twice in one file is caught`() {
        val flags = DuplicateGuard.flag(
            listOf(
                candidate("a", 69.0, 5, "SPOTIFY SI"),
                candidate("b", 69.0, 5, "SPOTIFY SI"),
            ),
            emptyList(),
        )
        assertNull("the first one is the real charge", flags["a"])
        assertEquals("Listed twice in this import", flags["b"])
    }

    @Test
    fun `references survive a round trip through the saved note`() {
        val note = ImportCandidate(
            id = "sms:1",
            amount = 1049.30,
            merchant = "UBER INDIA SYSTEMS P",
            date = LocalDate.of(2026, 9, 6),
            time = LocalTime.NOON,
            source = ImportSource.SMS,
            sourceLabel = "AX-KOTAKB",
            body = "",
            references = setOf("190820883373"),
        ).noteLabel()
        assertTrue(note.contains("190820883373"))
        assertEquals(setOf("190820883373"), referencesInNote(note))
    }
}
