package com.expensetracker.app.data.imports

import java.time.LocalDate
import java.time.LocalTime

enum class ImportSource { SMS, STATEMENT }

/**
 * One debit offered for import, whatever it was read from.
 *
 * SMS and statements describe the same spending in different words, so both are
 * normalised to this before anything else looks at them - that is what lets
 * [DuplicateGuard] compare across sources.
 */
data class ImportCandidate(
    /** Stable across a rescan: "sms:123" or "stmt:7". */
    val id: String,
    val amount: Double,
    val merchant: String,
    val date: LocalDate,
    val time: LocalTime,
    val source: ImportSource,
    /** SMS sender, or the statement's file name. */
    val sourceLabel: String,
    /** Full original text, used for category guessing. */
    val body: String,
    /** UPI/IMPS references, printed identically by both sources. */
    val references: Set<String> = emptySet(),
) {
    /**
     * Saved on the entry. The reference is kept deliberately: it is what lets a
     * later import from the other source recognise this exact payment.
     */
    fun noteLabel(): String {
        val origin = if (source == ImportSource.SMS) "SMS" else "statement"
        val from = sourceLabel.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty()
        val ref = references.firstOrNull()?.let { " | Ref $it" }.orEmpty()
        return "Imported from $origin$from$ref"
    }
}

/** Reads back the references [ImportCandidate.noteLabel] stored on a saved entry. */
fun referencesInNote(note: String?): Set<String> =
    NOTE_REFERENCE.findAll(note.orEmpty()).map { it.groupValues[1] }.toSet()

private val NOTE_REFERENCE = Regex("""Ref\s+(\d{6,})""", RegexOption.IGNORE_CASE)
