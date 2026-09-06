package com.expensetracker.app.data.sms

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** A parsed debit plus where and when it came from. */
data class SmsExpense(
    val smsId: Long,
    val sentAt: Long,
    val amount: Double,
    val merchant: String,
    val sender: String,
) {
    val date: LocalDate get() = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val time: LocalTime get() = instant.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0)
    private val instant: Instant get() = Instant.ofEpochMilli(sentAt)
}

/** Reads the SMS inbox and hands back only what [SmsParser] recognises as a spend. */
@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Newest first. [since] is the epoch-millis watermark of the last import,
     * so a repeat scan doesn't re-offer messages already dealt with.
     */
    suspend fun readDebits(since: Long): List<SmsExpense> = withContext(Dispatchers.IO) {
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
            "${Telephony.Sms.DATE} DESC LIMIT $SCAN_LIMIT",
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
                    add(
                        SmsExpense(
                            smsId = it.getLong(idCol),
                            sentAt = it.getLong(dateCol),
                            amount = debit.amount,
                            merchant = debit.merchant,
                            sender = it.getString(addressCol).orEmpty(),
                        )
                    )
                }
            }
        }
    }

    private companion object {
        // ponytail: newest 500 messages per scan, paginate if anyone hits the ceiling.
        const val SCAN_LIMIT = 500
    }
}
