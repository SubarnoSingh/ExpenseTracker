package com.expensetracker.app.data.statement

import com.expensetracker.app.data.imports.ImportCandidate
import com.expensetracker.app.data.imports.ImportSource
import com.expensetracker.app.data.sms.cleanMerchant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What a parse run produced, including what it chose not to import. */
data class StatementResult(
    val candidates: List<ImportCandidate>,
    val credits: Int,
    val skipped: Int,
    val error: String? = null,
)

/**
 * Reads a delimited bank statement (CSV/TSV) exported from net banking.
 *
 * Banks disagree on everything here, so nothing is bank-specific: the table is
 * found by looking for a header naming a date and an amount, and the money is read
 * from whichever shape that bank uses - a Withdrawal/Deposit pair, a signed Amount,
 * or (as Kotak does) a single Amount beside a Dr/Cr column.
 *
 * A file that can't be read reports why, rather than showing an empty list.
 */
object StatementParser {

    private val DATE_HEADERS = listOf("transaction date", "txn date", "value date", "posting date", "post date", "date")
    private val DESC_HEADERS = listOf("narration", "particulars", "transaction remarks", "description", "remarks", "transaction details", "details")
    private val DEBIT_HEADERS = listOf("withdrawal amt", "withdrawal amount", "withdrawal", "debit amount", "debit")
    private val CREDIT_HEADERS = listOf("deposit amt", "deposit amount", "deposit", "credit amount", "credit")
    private val AMOUNT_HEADERS = listOf("transaction amount", "amount", "amt")
    private val REF_HEADERS = listOf("chq /ref no", "chq/ref no", "ref no", "reference", "cheque no")

    /** Kotak writes "Dr / Cr" next to the amount and again next to the balance. */
    private val TYPE_HEADERS = listOf("dr / cr", "dr/cr", "type", "transaction type")

    private val DATE_PATTERNS = listOf(
        "dd-MM-yyyy", "dd/MM/yyyy", "dd.MM.yyyy",
        "d-M-yyyy", "d/M/yyyy",
        "dd-MM-yy", "dd/MM/yy",
        "dd-MMM-yyyy", "dd MMM yyyy", "dd-MMM-yy", "dd MMM yy",
        "yyyy-MM-dd", "yyyy/MM/dd",
    )
    private val DATE_FORMATS = DATE_PATTERNS.map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }
    private val DATE_TIME_FORMATS = DATE_PATTERNS.map {
        DateTimeFormatter.ofPattern("$it HH:mm:ss", Locale.ENGLISH)
    }

    /** Rows after the transactions: closing balance, footnotes, contact details. */
    private val TRAILER = Regex(
        """^(closing balance|opening balance|important note|total|statement summary|the transaction|csv statement|you may call|write to us)""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String, fileName: String): StatementResult {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return StatementResult(emptyList(), 0, 0, "That file is empty.")

        val delimiter = detectDelimiter(lines)
        val rows = lines.map { splitRow(it, delimiter) }

        val headerIndex = rows.indexOfFirst { isHeader(it) }
        if (headerIndex < 0) {
            return StatementResult(
                emptyList(), 0, 0,
                "Couldn't find the transaction table. The file needs a header row naming " +
                    "a date column and an amount, withdrawal or debit column.",
            )
        }

        val header = rows[headerIndex].map { it.trim().lowercase() }
        val dateCol = header.findColumn(DATE_HEADERS)
        val descCol = header.findColumn(DESC_HEADERS)
        val amountCol = header.findColumn(AMOUNT_HEADERS)
        val debitCol = header.findColumn(DEBIT_HEADERS)
        val creditCol = header.findColumn(CREDIT_HEADERS)
        val refCol = header.findColumn(REF_HEADERS)
        // The Dr/Cr that belongs to the amount is the one just after it - the later
        // duplicate describes the running balance and is always "CR".
        val typeCol = header.findColumnAfter(TYPE_HEADERS, amountCol)

        if (dateCol < 0) return StatementResult(emptyList(), 0, 0, "No date column in that file.")
        if (debitCol < 0 && amountCol < 0) {
            return StatementResult(emptyList(), 0, 0, "No amount or withdrawal column in that file.")
        }

        val candidates = mutableListOf<ImportCandidate>()
        var credits = 0
        var skipped = 0

        for ((offset, row) in rows.drop(headerIndex + 1).withIndex()) {
            if (row.all { it.isBlank() }) continue
            if (TRAILER.containsMatchIn(row.firstOrNull()?.trim().orEmpty())) break

            val stamp = row.getOrNull(dateCol).orEmpty()
            val date = parseDate(stamp)
            if (date == null) {
                skipped++
                continue
            }

            val spend = spendIn(row, amountCol, debitCol, creditCol, typeCol)
            if (spend == null) {
                credits++
                continue
            }
            if (spend <= 0.0) {
                skipped++
                continue
            }

            val description = descCol.takeIf { it >= 0 }?.let { row.getOrNull(it) }.orEmpty().trim()
            val reference = refCol.takeIf { it >= 0 }?.let { row.getOrNull(it) }.orEmpty().trim()
            candidates += ImportCandidate(
                id = "stmt:${headerIndex + 1 + offset}",
                amount = spend,
                merchant = StatementDescription.merchantOf(description),
                date = date,
                time = parseTime(stamp) ?: LocalTime.NOON,
                source = ImportSource.STATEMENT,
                sourceLabel = fileName,
                body = description,
                references = StatementDescription.referencesOf(description, reference),
            )
        }

        return StatementResult(candidates, credits, skipped)
    }

    /** @return the debit amount, or null when the row is money coming in. */
    private fun spendIn(
        row: List<String>,
        amountCol: Int,
        debitCol: Int,
        creditCol: Int,
        typeCol: Int,
    ): Double? {
        // Shape 1: separate withdrawal and deposit columns.
        if (debitCol >= 0) {
            val debit = row.getOrNull(debitCol)?.let(::parseAmount)
            if (debit != null && debit > 0) return debit
            val credit = creditCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.let(::parseAmount) }
            return if (credit != null && credit > 0) null else 0.0
        }

        val amount = amountCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.let(::parseAmount) }
            ?: return 0.0

        // Shape 2: one amount column with a Dr/Cr marker beside it.
        if (typeCol >= 0) {
            val type = row.getOrNull(typeCol)?.trim()?.lowercase().orEmpty()
            return when {
                type.startsWith("cr") -> null
                type.startsWith("dr") -> amount
                else -> 0.0
            }
        }

        // Shape 3: a single signed column - negative, "(1,234)" or "Dr" means money out.
        return if (amount < 0) -amount else null
    }

    private fun List<String>.findColumn(names: List<String>): Int {
        names.forEach { name -> indexOfFirst { it == name }.let { if (it >= 0) return it } }
        names.forEach { name -> indexOfFirst { it.contains(name) }.let { if (it >= 0) return it } }
        return -1
    }

    /** The first matching column strictly after [after], for headers that repeat. */
    private fun List<String>.findColumnAfter(names: List<String>, after: Int): Int {
        val start = (after + 1).coerceAtLeast(0)
        for (i in start until size) {
            if (names.any { this[i] == it || this[i].contains(it) }) return i
        }
        return findColumn(names)
    }

    private fun isHeader(row: List<String>): Boolean {
        val cells = row.map { it.trim().lowercase() }
        val hasDate = cells.any { cell -> DATE_HEADERS.any { cell.contains(it) } }
        val hasMoney = cells.any { cell ->
            (DEBIT_HEADERS + AMOUNT_HEADERS + CREDIT_HEADERS).any { cell.contains(it) }
        }
        return hasDate && hasMoney
    }

    private fun detectDelimiter(lines: List<String>): Char =
        listOf(',', '\t', ';', '|')
            .associateWith { d -> lines.take(20).sumOf { line -> line.count { it == d } } }
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }
            ?.key
            ?: ','

    /** Minimal CSV reader: honours quoted fields and doubled quotes inside them. */
    internal fun splitRow(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    cell.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> { cells += cell.toString(); cell.clear() }
                else -> cell.append(c)
            }
            i++
        }
        cells += cell.toString()
        return cells.map { it.trim() }
    }

    internal fun parseDate(raw: String): LocalDate? {
        val value = raw.trim().ifBlank { return null }
        val datePart = value.substringBefore(' ')
        DATE_FORMATS.forEach { format ->
            runCatching { return LocalDate.parse(datePart, format) }
        }
        return null
    }

    /** Kotak stamps "01-09-2026 10:39:52", so an entry can keep its real time. */
    internal fun parseTime(raw: String): LocalTime? {
        val value = raw.trim()
        if (!value.contains(' ')) return null
        DATE_TIME_FORMATS.forEach { format ->
            runCatching { return LocalDateTime.parse(value, format).toLocalTime().withSecond(0).withNano(0) }
        }
        return null
    }

    /** "1,234.56", "Rs. 1,234.56", "1234.56 Dr", "(1,234.56)" -> 1234.56 / -1234.56 */
    internal fun parseAmount(raw: String): Double? {
        val text = raw.trim()
        if (text.isBlank()) return null
        val bracketed = text.contains('(') && text.contains(')')
        val cleaned = text
            .replace(Regex("""(?i)\b(rs\.?|inr|dr|cr)\b"""), "")
            .replace(Regex("""[^0-9.\-]"""), "")
        if (cleaned.isBlank()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return if (bracketed && value > 0) -value else value
    }
}
