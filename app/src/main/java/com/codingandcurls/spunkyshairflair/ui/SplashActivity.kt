package com.codingandcurls.spunkyshairflair.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full-screen brand colour behind both system bars, so use light icons on them.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val loggedIn = Repos.users(this).getCurrentUser() != null
            val target = if (loggedIn) MainActivity::class.java else LoginActivity::class.java
            startActivity(clearTaskIntent(this, target))
            finish()
        }, 1500)
    }
}
