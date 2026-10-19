package com.codingandcurls.spunkyshairflair.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.logic.Validators

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailLayout = findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            val emailError = Validators.emailError(email)
            val passwordError = if (password.isEmpty()) "Enter your password" else null
            emailLayout.error = emailError
            passwordLayout.error = passwordError
            if (emailError != null || passwordError != null) return@setOnClickListener

            val user = Repos.users(this).login(email, password)
            if (user == null) {
                passwordLayout.error = "Incorrect email or password"
                return@setOnClickListener
            }
            toast("Welcome back, ${user.name.substringBefore(' ')}!")
            startActivity(clearTaskIntent(this, MainActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.registerLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
