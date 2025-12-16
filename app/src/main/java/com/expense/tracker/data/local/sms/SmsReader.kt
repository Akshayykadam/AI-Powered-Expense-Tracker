package com.expense.tracker.data.local.sms

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import javax.inject.Inject



/**
 * Reads SMS messages from Android's SMS Content Provider.
 * Only accesses Inbox messages.
 */
class SmsReader @Inject constructor() {
    
    companion object {
        private val SMS_URI: Uri = Telephony.Sms.Inbox.CONTENT_URI
        
        // Known financial senders - expanded list for Indian banks/wallets
        val KNOWN_FINANCIAL_SENDERS = setOf(
            // Major Banks
            "HDFCBK", "HDFC", "HDFCBANK",
            "ICICIB", "ICICI", "ICICIBANK",
            "AXISBK", "AXIS", "AXISBANK",
            "SBIINB", "SBIIN", "SBI",
            "KOTAKB", "KOTAK",
            "PNBSMS", "PNB",
            "BOIIND", "BOI",
            "CANBNK", "CANARA",
            "UNIONB", "UNION",
            "IABORB", "IDBI",
            "YESBK", "YES",
            "INDUSB", "INDUS",
            "FEDBK", "FEDERAL",
            "RBLBNK", "RBL",
            
            // UPI / Wallets
            "SBIUPI", "IKIUPI", "AXISUPI", "HDFCUPI",
            "GPAY", "GOOGLEPAY",
            "PHONEPE", "PHNEPE",
            "PAYTM", "PYTM",
            "AMAZON", "AMAZONPAY",
            "MOBIKW", "MOBIKWIK",
            "FREECHARGE", "FRCHRG",
            "AIRTEL", "AIRTELMONEY",
            "JIOMONEY", "JIO",
            
            // Credit Cards
            "CITI", "CITIBANK",
            "AMEX", "AMERICANEXPRESS",
            "HSBC",
            "SCBANK", "STANDARDCHARTERED",
            "DBIBANK", "DBS"
        )
    }
    
    /**
     * Reads all SMS from financial senders
     */
    fun readFinancialSms(contentResolver: ContentResolver): List<RawSms> {
        val smsList = mutableListOf<RawSms>()
        
        val cursor: Cursor? = contentResolver.query(
            SMS_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            
            while (it.moveToNext()) {
                val address = it.getString(addressIndex) ?: continue
                val body = it.getString(bodyIndex) ?: continue
                
                // Filter for financial senders
                // Filter for financial senders - DISABLED for AI-Only Mode
                // Check if body contains any digits to be worth checking (basic sanity)
                val hasDigits = body.any { it.isDigit() }
                
                if (hasDigits) { // OLD: if (isFinancialSender(address)) {
                    smsList.add(
                        RawSms(
                            id = it.getLong(idIndex),
                            address = address,
                            body = body,
                            date = it.getLong(dateIndex)
                        )
                    )
                }
            }
        }
        
        return smsList
    }
    
    /**
     * Reads SMS messages from a specific time onwards
     */
    fun readFinancialSmsSince(contentResolver: ContentResolver, sinceTimestamp: Long): List<RawSms> {
        val smsList = mutableListOf<RawSms>()
        
        val cursor: Cursor? = contentResolver.query(
            SMS_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(sinceTimestamp.toString()),
            "${Telephony.Sms.DATE} DESC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            
            while (it.moveToNext()) {
                val address = it.getString(addressIndex) ?: continue
                val body = it.getString(bodyIndex) ?: continue
                
                // Filter for financial senders - DISABLED for AI-Only Mode
                val hasDigits = body.any { it.isDigit() }
                
                if (hasDigits) { // OLD: if (isFinancialSender(address)) {
                    smsList.add(
                        RawSms(
                            id = it.getLong(idIndex),
                            address = address,
                            body = body,
                            date = it.getLong(dateIndex)
                        )
                    )
                }
            }
        }
        
        return smsList
    }
    
    /**
     * Check if sender matches known financial institutions
     */
    private fun isFinancialSender(address: String): Boolean {
        val normalized = address.uppercase().replace(Regex("[^A-Z]"), "")
        return KNOWN_FINANCIAL_SENDERS.any { sender ->
            normalized.contains(sender)
        }
    }
}
