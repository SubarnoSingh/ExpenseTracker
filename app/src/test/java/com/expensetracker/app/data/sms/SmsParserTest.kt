package com.expensetracker.app.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Real message shapes from Kotak, SBI, slice, Razorpay and Unity SFB. */
class SmsParserTest {

    private fun debit(body: String): ParsedDebit =
        requireNotNull(SmsParser.parse(body)) { "expected a debit in: $body" }

    @Test
    fun `upi transfer to a merchant`() {
        val parsed = debit(
            "Sent Rs.1049.30 from Kotak Bank A/c X7159 to UBER INDIA SYSTEMS P on 06-09-26. " +
                "UPI Ref 190820883373. Not done by you? Tap https://kotak.bank.in/KBANKT/Fraud"
        )
        assertEquals(1049.30, parsed.amount, 0.001)
        assertEquals("UBER INDIA SYSTEMS P", parsed.merchant)
    }

    @Test
    fun `card spend ignores the trailing available balance`() {
        val parsed = debit(
            "Rs.421.00 spent via Kotak Debit Card XX0440 at WL *STEAM PURCHASE on 07/09/2026 IST. " +
                "Avl bal Rs.9495.39 Not you?Tap https://kotak.bank.in/KBANKT/Fraud"
        )
        assertEquals(421.00, parsed.amount, 0.001)
        assertEquals("WL *STEAM PURCHASE", parsed.merchant)
    }

    @Test
    fun `card spend ignores the trailing available limit`() {
        val parsed = debit(
            "Rs. 791 spent at ABSOLUTE BARBEQUE PVT L.\nCard: 6789  Time: 04/09/2026, 03:57pm.\n" +
                "Avl Limit: Rs. 2,185.\nNot you? Call 18002099999.\n-Roarbank by Unity SFB"
        )
        assertEquals(791.0, parsed.amount, 0.001)
        assertEquals("ABSOLUTE BARBEQUE PVT L", parsed.merchant)
    }

    @Test
    fun `sbi style debit has no currency prefix`() {
        val parsed = debit(
            "Dear UPI user A/C X5042 debited by 10.00 on date 05Sep26 trf to shaheentamanna11 " +
                "Refno 661430803383 If not u? call-1800111109 for other services-18001234-SBI"
        )
        assertEquals(10.0, parsed.amount, 0.001)
        assertEquals("shaheentamanna11", parsed.merchant)
    }

    @Test
    fun `amount with a thousands separator`() {
        val parsed = debit(
            "Rs. 4,500 sent from a/c xx5878 on 05-Sep-26 to MISS AYUSHI MANDAL " +
                "(UPI Ref: 624888162634). Not you? Call 08048329999 - slice"
        )
        assertEquals(4500.0, parsed.amount, 0.001)
        assertEquals("MISS AYUSHI MANDAL", parsed.merchant)
    }

    @Test
    fun `subscription payment`() {
        val parsed = debit(
            "Payment of Rs.39 towards subscription for SMARTCOIN FINANCIALS PRIVATE L " +
                "successfully made on 28 August 2026 - Razorpay"
        )
        assertEquals(39.0, parsed.amount, 0.001)
        assertEquals("SMARTCOIN FINANCIALS PRIVATE L", parsed.merchant)
    }

    @Test
    fun `money received is not an expense`() {
        assertNull(
            SmsParser.parse(
                "Received Rs.200.00 in your Kotak Bank AC 7159 from Subarno  Singh on 07-09-26." +
                    "UPI Ref:661694366862"
            )
        )
    }

    @Test
    fun `incoming settlement is not an expense`() {
        assertNull(
            SmsParser.parse(
                "Settlement worth ₹ 100.00 for Razorpay MID SQjw5GHifPSnzD has been processed. " +
                    "It will be credited in your bank account before 9 PM today!"
            )
        )
    }

    @Test
    fun `otp and reminders are skipped`() {
        assertNull(SmsParser.parse("123456 is your OTP to pay Rs.500 at AMAZON. Do not share."))
        assertNull(SmsParser.parse("Rs.1200 will be debited from A/c X1234 on 10-09-26 for your SIP."))
        assertNull(SmsParser.parse("Your payment of Rs.300 to SWIGGY failed. Amount will be reversed."))
    }
}

/** Guards the key that decides whether two messages are the same subscription. */
class MerchantKeyTest {

    @Test
    fun `the same service spelled differently collapses to one key`() {
        val keys = listOf("NETFLIX COM", "Netflix India", "NETFLIX", "netflix pvt ltd")
            .map { merchantKey(it) }
            .toSet()
        assertEquals(setOf("netflix"), keys)
    }

    @Test
    fun `corporate suffixes are dropped`() {
        assertEquals("smartcoin financials", merchantKey("SMARTCOIN FINANCIALS PRIVATE L"))
        assertEquals("github", merchantKey("GITHUB INC"))
        assertEquals("uber systems", merchantKey("UBER INDIA SYSTEMS P"))
    }

    @Test
    fun `different services stay different`() {
        assertNotEquals(merchantKey("SPOTIFY INDIA"), merchantKey("NETFLIX COM"))
    }

    @Test
    fun `a name made only of noise still yields something`() {
        assertEquals("pvt ltd", merchantKey("PVT LTD"))
    }
}
