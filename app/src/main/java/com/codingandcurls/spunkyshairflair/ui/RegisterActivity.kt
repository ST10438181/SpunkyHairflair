package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.logic.Validators

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameLayout = findViewById<TextInputLayout>(R.id.nameLayout)
        val emailLayout = findViewById<TextInputLayout>(R.id.emailLayout)
        val phoneLayout = findViewById<TextInputLayout>(R.id.phoneLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)
        val confirmLayout = findViewById<TextInputLayout>(R.id.confirmLayout)
        val nameInput = findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val phoneInput = findViewById<TextInputEditText>(R.id.phoneInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)
        val confirmInput = findViewById<TextInputEditText>(R.id.confirmInput)

        findViewById<Button>(R.id.registerButton).setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val phone = phoneInput.text.toString()
            val password = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()

            nameLayout.error = Validators.nameError(name)
            emailLayout.error = Validators.emailError(email)
            phoneLayout.error = Validators.phoneError(phone)
            passwordLayout.error = Validators.passwordError(password)
            confirmLayout.error = if (password == confirm) null else "Passwords do not match"

            val hasError = listOf(nameLayout, emailLayout, phoneLayout, passwordLayout, confirmLayout)
                .any { it.error != null }
            if (hasError) return@setOnClickListener

            val user = Repos.users(this).register(name, email, phone, password)
            if (user == null) {
                emailLayout.error = "An account with this email already exists"
                return@setOnClickListener
            }
            toast("Welcome ${user.name.substringBefore(' ')}! You can now book appointments.")
            startActivity(clearTaskIntent(this, MainActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.loginLink).setOnClickListener { finish() }
    }
}
