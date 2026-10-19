package com.codingandcurls.spunkyshairflair.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(message: String) {
    requireContext().toast(message)
}

/** Starts an external intent (dialer, email, browser) without crashing if no app can handle it. */
fun Context.openExternal(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: Exception) {
        toast("No app found to open this")
    }
}

fun clearTaskIntent(context: Context, target: Class<*>): Intent =
    Intent(context, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
