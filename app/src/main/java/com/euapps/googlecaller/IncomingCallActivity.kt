package com.euapps.googlecaller

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.TextUtils
import kotlinx.android.synthetic.main.incoming_call.*
import org.jetbrains.anko.doAsync

const val title_param = "title"
const val snippet_param = "snippet"
const val phone_param = "phone"

class IncomingCallActivity : Activity() {

    private var popUpHeightWithSnippet = 550
    private var popUpHeightNoSnippet = 400

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.incoming_call)
        setFinishOnTouchOutside(true)

        val phone = intent.getStringExtra(phone_param)
        val title = intent.getStringExtra(title_param)
        val snippet = intent.getStringExtra(snippet_param)
        val snippetNeeded = !title.equalsAlphanumeric(snippet)

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val showOnTop = prefs.getBoolean("show_on_top", false)
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        val params = window.attributes
        params.x = 0
        params.height = if (snippetNeeded) popUpHeightWithSnippet else popUpHeightNoSnippet
        params.width = size.x
        if (showOnTop) {
            params.y = -size.y / 2
        } else {
            params.y = size.y - if (snippetNeeded) popUpHeightWithSnippet else popUpHeightNoSnippet
        }
        window.attributes = params

        incoming_title.text = title
        incoming_snippet.text = if (snippetNeeded) snippet else ""

        dismiss.setOnClickListener { finish() }
        settings.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        search.setOnClickListener {
            val url = "https://www.google.com/search?q=$phone"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}

class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        PreferenceManager.setDefaultValues(context, R.xml.prefs, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
        val isWiFi: Boolean = activeNetwork?.type == ConnectivityManager.TYPE_WIFI
        val disableSearch = prefs.getBoolean("disable_search", false)
        val excludeContacts = prefs.getBoolean("exclude_contacts", false)
        val wifiOnly = prefs.getBoolean("wifi_only", false)

        if (disableSearch || !isConnected || (wifiOnly && !isWiFi)) return

        val phoneListener = IncomingCallListener(context, excludeContacts)
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE)
    }
}

class IncomingCallListener(private val context: Context, private val excludeContacts: Boolean) : PhoneStateListener() {

    override fun onCallStateChanged(state: Int, incomingNumber: String) {
        if (state == TelephonyManager.CALL_STATE_RINGING && !TextUtils.isEmpty(incomingNumber)) {
            if (excludeContacts && contactExists(context, incomingNumber)) return
            context.doAsync {
                val searchResult = firstResult(incomingNumber)
                if (searchResult.first.isNullOrEmpty() || searchResult.second.isNullOrEmpty()) return@doAsync
                val i = Intent(context, IncomingCallActivity::class.java)
                i.putExtra(title_param, searchResult.first)
                i.putExtra(snippet_param, searchResult.second)
                i.putExtra(phone_param, incomingNumber)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
        }
    }
}