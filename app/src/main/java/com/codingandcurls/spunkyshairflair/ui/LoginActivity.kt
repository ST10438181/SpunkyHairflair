package com.codingandcurls.spunkyshairflair.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.ApiResult
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.logic.Validators
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_login, hasToolbar = false)

        val emailLayout = findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            val emailError = Validators.emailError(email)
            val passwordError = if (password.isEmpty()) "Enter your password" else null
            emailLayout.error = emailError
            passwordLayout.error = passwordError
            if (emailError != null || passwordError != null) return@setOnClickListener

            loginButton.isEnabled = false
            lifecycleScope.launch {
                val result = Repos.users(this@LoginActivity).login(email, password)
                loginButton.isEnabled = true
                when (result) {
                    is ApiResult.Success -> {
                        toast("Welcome back, ${result.value.name.substringBefore(' ')}!")
                        startActivity(clearTaskIntent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is ApiResult.Failure -> {
                        if (result.code == 0) toast(result.message) else passwordLayout.error = result.message
                    }
                }
            }
        }

        findViewById<TextView>(R.id.registerLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
