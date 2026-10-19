package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val loggedIn = Repos.users(this).getCurrentUser() != null
            val target = if (loggedIn) MainActivity::class.java else LoginActivity::class.java
            startActivity(clearTaskIntent(this, target))
            finish()
        }, 1500)
    }
}
