package com.codingandcurls.spunkyshairflair.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.ApiResult

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

/**
 * Shows an API error. A 401 means the saved login expired (or was revoked), so the user is
 * sent back to the Login screen.
 */
fun Context.handleFailure(failure: ApiResult.Failure) {
    toast(failure.message)
    if (failure.code == 401) {
        startActivity(clearTaskIntent(this, LoginActivity::class.java))
    }
}

/**
 * Sets the screen layout and draws it edge-to-edge (behind the status and navigation bars).
 * Android 15+ forces this for apps that target API 35 or higher, which Google Play now requires,
 * so every screen keeps its content clear of the system bars and the on-screen keyboard.
 *
 * @param hasToolbar true when the layout starts with the dark toolbar (id R.id.toolbar). The toolbar
 *        is made taller so it extends behind the status bar, keeping the old coloured status bar look.
 */
fun AppCompatActivity.setEdgeToEdgeContentView(layoutRes: Int, hasToolbar: Boolean) {
    val transparent = Color.TRANSPARENT
    // Dark toolbar behind the status bar needs light icons; light screens need dark icons.
    val statusBarStyle = if (hasToolbar) SystemBarStyle.dark(transparent)
    else SystemBarStyle.light(transparent, transparent)
    enableEdgeToEdge(
        statusBarStyle = statusBarStyle,
        navigationBarStyle = SystemBarStyle.light(transparent, transparent)
    )
    setContentView(layoutRes)

    val content = findViewById<View>(android.R.id.content)
    val toolbar: View? = if (hasToolbar) findViewById(R.id.toolbar) else null
    val toolbarHeight = toolbar?.layoutParams?.height ?: 0   // resolved from ?attr/actionBarSize

    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        // With a toolbar the top inset goes to the toolbar; otherwise pad the whole screen.
        val topPadding = if (toolbar != null) 0 else bars.top
        view.setPadding(bars.left, topPadding, bars.right, maxOf(bars.bottom, keyboard.bottom))
        if (toolbar != null && toolbarHeight > 0) {
            toolbar.setPadding(toolbar.paddingLeft, bars.top, toolbar.paddingRight, toolbar.paddingBottom)
            val params = toolbar.layoutParams
            params.height = toolbarHeight + bars.top
            toolbar.layoutParams = params
        }
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(content)
}
