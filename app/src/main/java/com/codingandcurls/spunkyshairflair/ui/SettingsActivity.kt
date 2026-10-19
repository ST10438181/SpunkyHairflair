package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.logic.Validators

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        val users = Repos.users(this)
        val user = users.getCurrentUser()
        if (user == null) {
            finish()
            return
        }

        val nameLayout = findViewById<TextInputLayout>(R.id.nameLayout)
        val phoneLayout = findViewById<TextInputLayout>(R.id.phoneLayout)
        val nameInput = findViewById<TextInputEditText>(R.id.nameInput)
        val phoneInput = findViewById<TextInputEditText>(R.id.phoneInput)
        findViewById<TextInputEditText>(R.id.emailInput).setText(user.email)
        nameInput.setText(user.name)
        phoneInput.setText(user.phone)

        findViewById<Button>(R.id.saveProfileButton).setOnClickListener {
            nameLayout.error = Validators.nameError(nameInput.text.toString())
            phoneLayout.error = Validators.phoneError(phoneInput.text.toString())
            if (nameLayout.error != null || phoneLayout.error != null) return@setOnClickListener
            users.updateProfile(user.id, nameInput.text.toString(), phoneInput.text.toString())
            toast("Profile updated")
        }

        val currentLayout = findViewById<TextInputLayout>(R.id.currentLayout)
        val newLayout = findViewById<TextInputLayout>(R.id.newLayout)
        val confirmLayout = findViewById<TextInputLayout>(R.id.confirmLayout)
        val currentInput = findViewById<TextInputEditText>(R.id.currentInput)
        val newInput = findViewById<TextInputEditText>(R.id.newInput)
        val confirmInput = findViewById<TextInputEditText>(R.id.confirmInput)

        findViewById<Button>(R.id.changePasswordButton).setOnClickListener {
            val current = currentInput.text.toString()
            val new = newInput.text.toString()
            currentLayout.error = if (current.isEmpty()) "Enter your current password" else null
            newLayout.error = Validators.passwordError(new)
            confirmLayout.error = if (new == confirmInput.text.toString()) null else "Passwords do not match"
            if (listOf(currentLayout, newLayout, confirmLayout).any { it.error != null }) return@setOnClickListener

            if (!users.changePassword(user.id, current, new)) {
                currentLayout.error = "Current password is incorrect"
                return@setOnClickListener
            }
            currentInput.setText("")
            newInput.setText("")
            confirmInput.setText("")
            toast("Password updated")
        }

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            users.logout()
            startActivity(clearTaskIntent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
