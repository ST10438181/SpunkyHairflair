package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.ApiResult
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.data.UserRepository
import com.codingandcurls.spunkyshairflair.logic.Validators
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_settings, hasToolbar = true)
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

        val saveButton = findViewById<Button>(R.id.saveProfileButton)
        saveButton.setOnClickListener {
            nameLayout.error = Validators.nameError(nameInput.text.toString())
            phoneLayout.error = Validators.phoneError(phoneInput.text.toString())
            if (nameLayout.error != null || phoneLayout.error != null) return@setOnClickListener

            saveButton.isEnabled = false
            lifecycleScope.launch {
                val result = users.updateProfile(nameInput.text.toString(), phoneInput.text.toString())
                saveButton.isEnabled = true
                when (result) {
                    is ApiResult.Success -> toast("Profile updated")
                    is ApiResult.Failure -> {
                        nameLayout.error = result.fields["name"]
                        phoneLayout.error = result.fields["phone"]
                        if (result.fields.isEmpty()) handleFailure(result)
                    }
                }
            }
        }

        val currentLayout = findViewById<TextInputLayout>(R.id.currentLayout)
        val newLayout = findViewById<TextInputLayout>(R.id.newLayout)
        val confirmLayout = findViewById<TextInputLayout>(R.id.confirmLayout)
        val currentInput = findViewById<TextInputEditText>(R.id.currentInput)
        val newInput = findViewById<TextInputEditText>(R.id.newInput)
        val confirmInput = findViewById<TextInputEditText>(R.id.confirmInput)

        val passwordButton = findViewById<Button>(R.id.changePasswordButton)
        passwordButton.setOnClickListener {
            val current = currentInput.text.toString()
            val new = newInput.text.toString()
            currentLayout.error = if (current.isEmpty()) "Enter your current password" else null
            newLayout.error = Validators.passwordError(new)
            confirmLayout.error = if (new == confirmInput.text.toString()) null else "Passwords do not match"
            if (listOf(currentLayout, newLayout, confirmLayout).any { it.error != null }) return@setOnClickListener

            passwordButton.isEnabled = false
            lifecycleScope.launch {
                val result = users.changePassword(current, new)
                passwordButton.isEnabled = true
                when (result) {
                    is ApiResult.Success -> {
                        currentInput.setText("")
                        newInput.setText("")
                        confirmInput.setText("")
                        toast("Password updated")
                    }
                    is ApiResult.Failure -> {
                        currentLayout.error = result.fields["currentPassword"]
                        newLayout.error = result.fields["newPassword"]
                        if (result.fields.isEmpty()) handleFailure(result)
                    }
                }
            }
        }

        findViewById<Button>(R.id.deleteAccountButton).setOnClickListener {
            confirmDeleteAccount(users)
        }

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            lifecycleScope.launch {
                users.logout()
                startActivity(clearTaskIntent(this@SettingsActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    /** Asks for the password, then deletes the account on the server and returns to the Login screen. */
    private fun confirmDeleteAccount(users: UserRepository) {
        val passwordInput = EditText(this)
        passwordInput.hint = "Enter your password to confirm"
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this)
        container.setPadding(padding, padding / 2, padding, 0)
        container.addView(passwordInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete your account?")
            .setMessage("This permanently deletes your account and all your bookings. It cannot be undone.")
            .setView(container)
            .setPositiveButton("Delete") { _, _ ->
                val password = passwordInput.text.toString()
                lifecycleScope.launch {
                    when (val result = users.deleteAccount(password)) {
                        is ApiResult.Success -> {
                            toast("Your account has been deleted")
                            startActivity(clearTaskIntent(this@SettingsActivity, LoginActivity::class.java))
                            finish()
                        }
                        is ApiResult.Failure -> handleFailure(result)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
