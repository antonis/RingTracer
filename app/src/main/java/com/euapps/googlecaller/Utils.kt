package com.euapps.googlecaller

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import org.jsoup.Jsoup

val String.cleanNumber: String
    get() {
        if (startsWith("+")) { //clean country code
            return substring(3).replace(" ", "")
        }
        return replace(" ", "") //clean spaces
    }

fun String.equalsAlphanumeric(other: String) = replace(Regex("[^A-Za-z0-9 ]"), "").equals(other.replace(Regex("[^A-Za-z0-9 ]"), ""), ignoreCase = true)

fun firstResult(keyword: String): Pair<String?, String?> {
    val doc = Jsoup.connect("https://google.com/search?q=$keyword").userAgent("Mozilla/5.0").get()
    val title = doc.selectFirst("h3.r a")?.text()
    val description = doc.selectFirst("span.st")?.text()
    return Pair(title, description)
}

fun contactExists(context: Context, lookupNumber: String): Boolean {
    val resolver: ContentResolver = context.contentResolver;
    val cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null,
            null)
    if (cursor.count > 0) {
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            val phoneNumber = (cursor.getString(
                    cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))).toInt()
            if (phoneNumber > 0) {
                val cursorPhone = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", arrayOf(id), null)
                if (cursorPhone.count > 0) {
                    while (cursorPhone.moveToNext()) {
                        val phoneNumValue = cursorPhone.getString(
                                cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        if (phoneNumValue.cleanNumber == lookupNumber.cleanNumber) return true;
                    }
                }
                cursorPhone.close()
            }
        }
    }
    cursor.close()
    return false
}

fun getVersionInfo(context: Context): String {
    var versionName = ""
    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        versionName = packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return versionName
}