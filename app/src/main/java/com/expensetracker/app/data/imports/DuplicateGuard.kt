package com.expensetracker.app.data.imports

import com.expensetracker.app.data.sms.merchantKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Something already saved, flattened to the parts worth comparing. */
data class SavedTransaction(
    val amount: Double,
    val date: LocalDate,
    val merchant: String,
    /** Where it came from, for the message shown on a flagged row. */
    val origin: String,
    val time: LocalTime = LocalTime.NOON,
    /** References recovered from the saved note, when it has any. */
    val references: Set<String> = emptySet(),
)

/**
 * Decides whether a candidate is already recorded, in either direction.
 *
 * The two sources describe one payment differently. The bank posts on its own date,
 * often a day or two after a card is swiped, and writes its own merchant string, so
 * exact matching finds nothing. Matching runs in two passes, strongest evidence
 * first, and **consumes** what it matches:
 *
 *  1. **Reference.** UPI and IMPS print the same reference in the SMS and in the
 *     statement, which identifies a payment exactly. Card payments carry no
 *     reference in either place, so this pass settles some rows and not others.
 *  2. **Amount, then date and time.** Both sources always agree on the amount to the
 *     paisa, so an equal amount inside a short window is the workhorse, and the only
 *     signal available for card spending. Merchant overlap and closeness in time
 *     pick the best of several equal amounts.
 *
 * Consuming matters: a saved entry can only account for one candidate. Without it,
 * three ₹500 payments on one day would all match the single ₹500 already saved and
 * two real payments would be written off as duplicates.
 *
 * Everything here is a heuristic, so nothing is hidden or deleted. Callers flag the
 * row, say why, and leave the decision to the user.
 */
object DuplicateGuard {

    /** A card swipe posts to the account up to a few days later. */
    private const val DATE_WINDOW_DAYS = 3L

    private val REASON_DATE = DateTimeFormatter.ofPattern("d MMM")

    /** Words too common to count as evidence that two payees are the same. */
    private val WEAK_TOKENS = setOf(
        "payment", "payments", "retail", "store", "stores", "shop", "india", "bank", "self", "name",
    )

    /** Compared in paisa so floating point can't make equal amounts differ. */
    private fun paisa(amount: Double): Long = Math.round(amount * 100)

    /**
     * @return each candidate id that looks already recorded, with the reason to show.
     */
    fun flag(
        candidates: List<ImportCandidate>,
        saved: List<SavedTransaction>,
    ): Map<String, String> {
        val pool = saved.toMutableList()
        val flags = mutableMapOf<String, String>()

        // Pass 1 - references settle a payment outright, whatever the dates say.
        for (candidate in candidates) {
            if (candidate.references.isEmpty()) continue
            val hit = pool.indexOfFirst { it.references.any(candidate.references::contains) }
            if (hit >= 0) {
                val match = pool.removeAt(hit)
                flags[candidate.id] = "Same reference as an entry from ${match.origin} " +
                    "on ${match.date.format(REASON_DATE)}"
            }
        }

        // Pass 2 - whatever is left, including every card payment, falls back to money.
        // Every workable pairing is scored and the best assigned first: taking
        // candidates in order would let an early row claim the match that belonged
        // to a later one that fits far better.
        val pairs = mutableListOf<Triple<Int, Int, Long>>()
        candidates.forEachIndexed { ci, candidate ->
            if (candidate.id in flags) return@forEachIndexed
            pool.forEachIndexed { pi, saved ->
                score(saved, candidate)?.let { pairs += Triple(ci, pi, it) }
            }
        }
        pairs.sortBy { it.third }

        val takenCandidates = mutableSetOf<Int>()
        val takenSaved = mutableSetOf<Int>()
        for ((ci, pi, _) in pairs) {
            if (ci in takenCandidates || pi in takenSaved) continue
            takenCandidates += ci
            takenSaved += pi
            flags[candidates[ci].id] = describe(pool[pi], candidates[ci])
        }

        // Pass 3 - the batch against itself, so one file listing a charge twice is
        // caught. This one insists the payee agrees too: several equal amounts on one
        // day are ordinary spending, whereas a real double-listing repeats the name.
        val batch = mutableListOf<SavedTransaction>()
        for (candidate in candidates.sortedBy { it.date }) {
            if (candidate.id !in flags) {
                val byRef = batch.indexOfFirst {
                    candidate.references.isNotEmpty() && it.references.any(candidate.references::contains)
                }
                val hit = if (byRef >= 0) byRef else bestMatch(batch, candidate, requireName = true)
                if (hit >= 0) {
                    flags[candidate.id] = "Listed twice in this import"
                    batch.removeAt(hit)
                }
            }
            batch += candidate.asSaved()
        }

        return flags
    }

    /**
     * How well a saved entry accounts for a candidate - lower is better, null when
     * they cannot be the same payment. A shared word outranks any closeness in time.
     */
    private fun score(
        saved: SavedTransaction,
        candidate: ImportCandidate,
        requireName: Boolean = false,
    ): Long? {
        if (paisa(saved.amount) != paisa(candidate.amount)) return null
        if (abs(ChronoUnit.DAYS.between(saved.date, candidate.date)) > DATE_WINDOW_DAYS) return null
        val named = sharesName(saved.merchant, candidate.merchant)
        if (requireName && !named) return null
        val nameRank = if (named) 0L else 1L
        val minutes = abs(
            ChronoUnit.MINUTES.between(
                LocalDateTime.of(saved.date, saved.time),
                LocalDateTime.of(candidate.date, candidate.time),
            )
        )
        return nameRank * 1_000_000 + minutes
    }

    /** Index of the best remaining match, or -1. */
    private fun bestMatch(
        pool: List<SavedTransaction>,
        candidate: ImportCandidate,
        requireName: Boolean = false,
    ): Int {
        var bestIndex = -1
        var bestScore = Long.MAX_VALUE
        pool.forEachIndexed { index, saved ->
            val score = score(saved, candidate, requireName) ?: return@forEachIndexed
            if (score < bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun describe(match: SavedTransaction, candidate: ImportCandidate): String =
        if (sharesName(match.merchant, candidate.merchant)) {
            "Already recorded ${match.date.format(REASON_DATE)} from ${match.origin}"
        } else {
            "Same amount as \"${match.merchant}\" on ${match.date.format(REASON_DATE)} " +
                "from ${match.origin}"
        }

    private fun ImportCandidate.asSaved() = SavedTransaction(
        amount = amount,
        date = date,
        merchant = merchant,
        origin = if (source == ImportSource.SMS) "this scan" else "this statement",
        time = time,
        references = references,
    )

    /** True when two payee strings share a distinctive word. */
    internal fun sharesName(a: String, b: String): Boolean {
        val left = tokens(a)
        val right = tokens(b)
        if (left.isEmpty() || right.isEmpty()) return false
        if (left.any { it in right }) return true
        // Banks truncate: "UBER INDIA SYS" against "UBER INDIA SYSTEMS P".
        return left.any { l -> right.any { r -> l.length >= 4 && (r.startsWith(l) || l.startsWith(r)) } }
    }

    private fun tokens(name: String): Set<String> =
        merchantKey(name).split(' ')
            .filter { it.length > 2 && it !in WEAK_TOKENS }
            .toSet()
}
