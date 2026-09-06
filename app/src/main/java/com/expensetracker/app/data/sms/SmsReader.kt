package com.expensetracker.app.data.sms

import android.content.Context
import android.provider.Telephony
import com.expensetracker.app.data.imports.ImportCandidate
import com.expensetracker.app.data.imports.ImportSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the SMS inbox and hands back only what [SmsParser] recognises as a spend. */
@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Newest first. [since] is the epoch-millis start of the scan window. */
    suspend fun readDebits(since: Long): List<ImportCandidate> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} > ?",
            arrayOf(since.toString()),
            // No row limit: the date window is the bound. A "newest N messages" cap
            // silently overrode it - on a busy inbox 2000 messages is about three
            // months, so asking for 2022 still only ever returned the last 90 days.
            "${Telephony.Sms.DATE} DESC",
        ) ?: return@withContext emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressCol = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            buildList {
                while (it.moveToNext()) {
                    val body = it.getString(bodyCol) ?: continue
                    val debit = SmsParser.parse(body) ?: continue
                    val stamp = Instant.ofEpochMilli(it.getLong(dateCol))
                        .atZone(ZoneId.systemDefault())
                    add(
                        ImportCandidate(
                            id = "sms:${it.getLong(idCol)}",
                            amount = debit.amount,
                            merchant = debit.merchant,
                            date = stamp.toLocalDate(),
                            time = stamp.toLocalTime().withSecond(0).withNano(0),
                            source = ImportSource.SMS,
                            sourceLabel = it.getString(addressCol).orEmpty(),
                            body = body,
                            references = referencesIn(body),
                        )
                    )
                }
            }
        }
    }
}
