package com.expensetracker.app.data.sms

import com.expensetracker.app.data.local.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCategoryGuessTest {

    private fun guess(body: String): String? {
        val debit = requireNotNull(SmsParser.parse(body)) { "expected a debit in: $body" }
        return SmsCategoryGuess.guess(debit.merchant, body)
    }

    @Test
    fun `every rule points at a category that actually exists`() {
        // A typo in the table would silently fall back to "unsorted" at runtime.
        val known = DefaultCategories.all.map { it.name }.toSet()
        val used = listOf(
            "Music & Video", "Software & Cloud", "Domains & Hosting", "Memberships",
            "Other Services", "Food & Drinks", "Groceries", "Transport", "Fuel",
            "Entertainment", "Education", "Health", "Shopping", "Bills",
            "Clothes", "Electronics", "Travel", "Gifts",
        )
        assertTrue("unknown categories: ${used - known}", known.containsAll(used))
    }

    @Test
    fun `subscription wording with an unknown brand still sorts as a subscription`() {
        assertEquals(
            "Other Services",
            guess(
                "Payment of Rs.39 towards subscription for SMARTCOIN FINANCIALS PRIVATE L " +
                    "successfully made on 28 August 2026 - Razorpay"
            ),
        )
    }

    @Test
    fun `known subscription brands land in their own category`() {
        assertEquals(
            "Music & Video",
            guess("Rs.199 spent via Kotak Debit Card XX0440 at NETFLIX COM on 07/09/2026 IST."),
        )
        assertEquals(
            "Software & Cloud",
            guess("Sent Rs.1699.00 from Kotak Bank A/c X7159 to GITHUB INC on 06-09-26."),
        )
    }

    @Test
    fun `everyday merchants sort into regular categories`() {
        assertEquals(
            "Food & Drinks",
            guess("Rs. 791 spent at ABSOLUTE BARBEQUE PVT L.\nCard: 6789  Avl Limit: Rs. 2,185."),
        )
        assertEquals(
            "Transport",
            guess("Sent Rs.1049.30 from Kotak Bank A/c X7159 to UBER INDIA SYSTEMS P on 06-09-26."),
        )
        assertEquals(
            "Entertainment",
            guess("Rs.421.00 spent via Kotak Debit Card XX0440 at WL *STEAM PURCHASE on 07/09/2026 IST."),
        )
    }

    @Test
    fun `occasional merchants sort into occasional categories`() {
        assertEquals(
            "Travel",
            guess("Rs. 8,400 sent from a/c xx5878 on 05-Sep-26 to MAKEMYTRIP INDIA (UPI Ref: 6248)."),
        )
    }

    @Test
    fun `a person-to-person transfer has no guess`() {
        assertNull(
            guess(
                "Dear UPI user A/C X5042 debited by 10.00 on date 05Sep26 trf to shaheentamanna11 " +
                    "Refno 661430803383"
            )
        )
    }

    @Test
    fun `mandate debits are read as spending, not skipped`() {
        val parsed = SmsParser.parse(
            "Rs.499.00 debited from A/c XX1234 on 05-09-26 towards e-mandate for SPOTIFY INDIA. " +
                "UPI Ref 661694366862"
        )
        assertEquals(499.0, requireNotNull(parsed).amount, 0.001)
    }
}
