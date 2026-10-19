package com.codingandcurls.spunkyshairflair.logic

/** Input validation. Every function returns an error message, or null when the input is valid. */
object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val NAME = Regex("^[A-Za-z][A-Za-z '\\-]{1,49}$")
    private val PHONE = Regex("^(0|\\+27)[0-9]{9}$")

    fun nameError(value: String): String? {
        val v = value.trim()
        if (v.length < 2) return "Enter your full name"
        if (!NAME.matches(v)) return "Name can only contain letters, spaces, - and '"
        return null
    }

    fun emailError(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return "Enter your email address"
        if (!EMAIL.matches(v)) return "Enter a valid email address"
        return null
    }

    fun phoneError(value: String): String? {
        val v = value.replace(" ", "")
        if (v.isEmpty()) return "Enter your phone number"
        if (!PHONE.matches(v)) return "Enter a valid SA number, e.g. 0812345678"
        return null
    }

    fun passwordError(value: String): String? {
        if (value.length < 8) return "Password must be at least 8 characters"
        if (!value.any { it.isLetter() } || !value.any { it.isDigit() }) {
            return "Password needs at least one letter and one number"
        }
        return null
    }
}
