package com.nokia.launcher.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentProviderOperation
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.telephony.TelephonyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.PhoneLookup
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

fun Context.granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

data class Sms(val thread: Long, val address: String, val body: String, val time: Long, val outgoing: Boolean)

/** The newest message of each conversation; [messages] must come newest first. */
fun conversations(messages: List<Sms>): List<Sms> = messages.distinctBy { it.thread }

/** Needs READ_SMS. Newest first. Chat (RCS) and picture messages are not in this table. */
fun loadSms(context: Context, limit: Int = 1_000): List<Sms> = runCatching {
    val columns = arrayOf(
        Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
        Telephony.Sms.DATE, Telephony.Sms.TYPE,
    )
    context.contentResolver.query(
        Telephony.Sms.CONTENT_URI, columns, null, null, "${Telephony.Sms.DATE} DESC",
    )?.use { c ->
        buildList {
            while (size < limit && c.moveToNext()) {
                add(
                    Sms(
                        c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getLong(3),
                        c.getInt(4) != Telephony.Sms.MESSAGE_TYPE_INBOX,
                    ),
                )
            }
        }
    }
}.getOrNull().orEmpty()

/** The contact's name for a number; needs READ_CONTACTS. */
fun contactName(context: Context, number: String): String? = runCatching {
    val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    context.contentResolver.query(uri, arrayOf(PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
}.getOrNull()

data class Contact(val id: Long, val name: String, val numbers: List<String>)

/** Joins one row per phone number into one entry per contact, ignoring repeated numbers. */
fun groupContacts(rows: List<Triple<Long, String, String>>): List<Contact> =
    rows.groupBy { it.first }.map { (id, list) ->
        Contact(id, list.first().second, list.map { it.third }.distinctBy { it.filter(Char::isDigit) })
    }

/** Needs READ_CONTACTS. Only contacts with a phone number, as on a SIM phone book. */
fun loadContacts(context: Context): List<Contact> = runCatching {
    context.contentResolver.query(
        Phone.CONTENT_URI, arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER),
        null, null, "${Phone.DISPLAY_NAME} COLLATE LOCALIZED",
    )?.use { c ->
        groupContacts(
            buildList {
                while (c.moveToNext()) add(Triple(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty()))
            },
        )
    }
}.getOrNull().orEmpty()

data class CallRecord(val number: String, val name: String?, val type: Int, val time: Long, val seconds: Long)

/** Needs READ_CALL_LOG. Newest first. */
fun loadCalls(context: Context, limit: Int = 100): List<CallRecord> = runCatching {
    val columns = arrayOf(
        CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
        CallLog.Calls.DATE, CallLog.Calls.DURATION,
    )
    context.contentResolver.query(CallLog.Calls.CONTENT_URI, columns, null, null, "${CallLog.Calls.DATE} DESC")
        ?.use { c ->
            buildList {
                while (size < limit && c.moveToNext()) {
                    add(CallRecord(c.getString(0).orEmpty(), c.getString(1), c.getInt(2), c.getLong(3), c.getLong(4)))
                }
            }
        }
}.getOrNull().orEmpty()

/** Needs WRITE_CONTACTS. Saves to the phone's local address book; returns false if it fails. */
fun addContact(context: Context, name: String, number: String): Boolean = runCatching {
    fun data(type: String, column: String, value: String) =
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, type)
            .withValue(column, value)
    context.contentResolver.applyBatch(
        ContactsContract.AUTHORITY,
        arrayListOf(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build(),
            data(StructuredName.CONTENT_ITEM_TYPE, StructuredName.DISPLAY_NAME, name).build(),
            data(Phone.CONTENT_ITEM_TYPE, Phone.NUMBER, number)
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build(),
        ),
    )
    true
}.getOrDefault(false)

/**
 * Whether Android must handle this number in its own dialer. Emergency numbers cannot be
 * called by an app, so any doubt goes to the dialer.
 */
fun needsSystemDialer(context: Context, number: String): Boolean = runCatching {
    context.getSystemService(TelephonyManager::class.java).isEmergencyNumber(number)
}.getOrDefault(true)

private const val SENT_ACTION = "com.nokia.launcher.SMS_SENT"

/**
 * Needs SEND_SMS. Android stores the sent message itself because this is not the default SMS app.
 * [onResult] reports whether the radio accepted every part.
 */
fun sendSms(context: Context, number: String, text: String, onResult: (Boolean) -> Unit) {
    val app = context.applicationContext
    try {
        val manager = app.getSystemService(SmsManager::class.java)
        val parts = manager.divideMessage(text)
        var left = parts.size
        var ok = true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (resultCode != android.app.Activity.RESULT_OK) ok = false
                if (--left == 0) {
                    app.unregisterReceiver(this)
                    onResult(ok)
                }
            }
        }
        ContextCompat.registerReceiver(app, receiver, IntentFilter(SENT_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        val sent = ArrayList(
            parts.indices.map {
                PendingIntent.getBroadcast(
                    app, it, Intent(SENT_ACTION).setPackage(app.packageName),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            },
        )
        manager.sendMultipartTextMessage(number, null, parts, sent, null)
    } catch (e: Exception) {
        // No SIM, no default SIM chosen, or no telephony at all.
        onResult(false)
    }
}
