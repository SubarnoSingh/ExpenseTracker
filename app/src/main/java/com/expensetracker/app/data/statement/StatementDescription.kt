package com.expensetracker.app.data.statement

import com.expensetracker.app.data.sms.cleanMerchant

/**
 * Pulls a readable payee out of a bank's transaction description.
 *
 * Descriptions are structured, not prose - the bank packs the rail, the counterparty,
 * a routing code and a reference into one slash-separated string, and which slot
 * holds the payee depends on the rail:
 *
 *   UPI/Bablu Fast Foo/YESB/150043055478/NO REMARK   -> Bablu Fast Foo
 *   PCD/0440/ZUDIO A UNIT OF TRENT/KOLKATT040926/... -> ZUDIO A UNIT OF TRENT
 *   SentIMPS624703740627Aftab Alam/SBINX5042/IMPS    -> Aftab Alam
 *   NEFT AXISCN1453340033 RAZORPAY PAYMENTS PVT LTD  -> RAZORPAY PAYMENTS PVT LTD
 */
object StatementDescription {

    /** Long digit runs are references, never a payee. */
    private val REFERENCE_LIKE = Regex("""^[0-9]{6,}$""")
    private val IMPS_PREFIX = Regex("""^sent\s*imps\s*\d+""", RegexOption.IGNORE_CASE)
    private val NEFT_PREFIX = Regex("""^(neft|rtgs|imps)\s+[A-Z0-9]{6,}\s+""", RegexOption.IGNORE_CASE)
    private val CARD_RAILS = setOf("pcd", "pos", "ecom", "atw", "atd")
    private val DIGITS = Regex("""\d{9,}""")

    fun merchantOf(description: String): String {
        val text = description.trim()
        if (text.isBlank()) return "Statement entry"

        val parts = text.split('/').map { it.trim() }.filter { it.isNotBlank() }
        val rail = parts.firstOrNull()?.lowercase().orEmpty()

        val payee = when {
            // Card rails put the card's last four in slot 1 and the merchant in slot 2.
            rail in CARD_RAILS && parts.size >= 3 -> parts[2]
            rail == "upi" && parts.size >= 2 -> parts[1]
            IMPS_PREFIX.containsMatchIn(text) ->
                IMPS_PREFIX.replace(text, "").substringBefore('/').trim()
            NEFT_PREFIX.containsMatchIn(text) -> NEFT_PREFIX.replace(text, "")
            parts.size >= 2 -> parts.firstOrNull { !REFERENCE_LIKE.matches(it) } ?: parts[0]
            else -> text
        }

        return cleanMerchant(payee)
            .ifBlank { cleanMerchant(text) }
            .ifBlank { "Statement entry" }
    }

    /**
     * Every long number the row carries. One of them is usually the same reference
     * the bank's SMS quoted, which is what lets the two sources be matched exactly.
     */
    fun referencesOf(description: String, referenceColumn: String): Set<String> =
        (DIGITS.findAll(description) + DIGITS.findAll(referenceColumn))
            .map { it.value }
            .toSet()
}
