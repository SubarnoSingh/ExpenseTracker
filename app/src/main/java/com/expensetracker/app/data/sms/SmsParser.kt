package com.expensetracker.app.data.sms

/** A debit recognised in an SMS body. */
data class ParsedDebit(
    val amount: Double,
    val merchant: String,
)

/**
 * Pulls the amount and merchant out of bank / UPI / card SMS.
 *
 * Banks all write their own format, so this is deliberately keyword driven
 * rather than per-bank: find a debit word, take the first amount, then take
 * whatever follows the first "to" / "at" / "for". Anything that doesn't fit
 * is dropped - the import screen is a review list, not an auto-writer.
 */
object SmsParser {

    private val DEBIT = Regex(
        """\b(debited|debit|spent|sent|paid|payment of|withdrawn|purchased?)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val CREDIT = Regex(
        """\b(credited|received|refunds?|refunded|reversed|settlement|cashback)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Alerts, reminders and failures that mention money but aren't a spend. */
    private val IGNORE = Regex(
        """\b(otp|one[- ]time password|will be (debited|deducted)|is due|due on|failed|declined|unsuccessful|requests? money|has requested|e-?mandate|balance is|available balance)\b""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * First alternative covers "Rs.421.00", "Rs. 4,500", "INR 39", "₹ 100.00".
     * Second covers SBI-style "debited by 10.00" with no currency at all.
     */
    private val AMOUNT = Regex(
        """\b(?:rs|inr)\.?\s*([\d,]+(?:\.\d{1,2})?)|₹\s*([\d,]+(?:\.\d{1,2})?)|\bdebited\s+(?:by|for)\s+([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    /** Tried in order - the first one that hits wins. */
    private val MERCHANT = listOf(
        Regex("""\btrf\s+to\s+(.+?)(?=\s+ref\s|\s+refno\b|\s+ref:|[.\n]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+vpa\s+([\w.@\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""\bat\s+(.+?)(?=\s+on\b|\s+using\b|[.,\n]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+(.+?)(?=\s+on\b|\s*\(|[.,\n]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bfor\s+(.+?)(?=\s+successfully\b|\s+on\b|\s+has\b|[.,\n]|$)""", RegexOption.IGNORE_CASE),
    )

    private val TRAILING_JUNK = Regex("""[\s.,:;\-*]+$""")

    fun parse(body: String): ParsedDebit? {
        if (IGNORE.containsMatchIn(body)) return null

        val debitAt = DEBIT.find(body)?.range?.first ?: return null
        // "credited" before the debit word means the money came in, not out.
        val creditAt = CREDIT.find(body)?.range?.first
        if (creditAt != null && creditAt < debitAt) return null

        val amount = AMOUNT.find(body)
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotEmpty() }
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null
        if (amount <= 0) return null

        return ParsedDebit(amount = amount, merchant = merchantIn(body))
    }

    private fun merchantIn(body: String): String {
        val raw = MERCHANT.firstNotNullOfOrNull { it.find(body)?.groupValues?.getOrNull(1) }
            ?: return "SMS expense"
        val cleaned = raw.replace(TRAILING_JUNK, "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .take(40)
        return cleaned.ifBlank { "SMS expense" }
    }
}
