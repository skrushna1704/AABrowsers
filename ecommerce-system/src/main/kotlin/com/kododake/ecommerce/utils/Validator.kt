package com.kododake.ecommerce.utils

import com.kododake.ecommerce.exceptions.ValidationException

object Validator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val PHONE_REGEX = Regex("^[6-9]\\d{9}$")
    private val PIN_REGEX = Regex("^\\d{4,6}$")

    fun requireNonBlank(value: String?, field: String): String {
        if (value.isNullOrBlank()) throw ValidationException("$field cannot be blank")
        return value.trim()
    }

    fun validateEmail(email: String): String {
        val trimmed = requireNonBlank(email, "Email")
        if (!EMAIL_REGEX.matches(trimmed)) throw ValidationException("Invalid email format")
        return trimmed.lowercase()
    }

    fun validatePhone(phone: String): String {
        val trimmed = requireNonBlank(phone, "Phone")
        if (!PHONE_REGEX.matches(trimmed)) throw ValidationException("Invalid phone number (10 digits, starts with 6-9)")
        return trimmed
    }

    fun validatePassword(password: String): String {
        val trimmed = requireNonBlank(password, "Password")
        if (trimmed.length < Constants.MIN_PASSWORD_LENGTH) {
            throw ValidationException("Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters")
        }
        return trimmed
    }

    fun validatePin(pin: String): String {
        val trimmed = requireNonBlank(pin, "PIN")
        if (!PIN_REGEX.matches(trimmed)) throw ValidationException("PIN must be 4-6 digits")
        return trimmed
    }

    fun validatePositive(value: Double, field: String): Double {
        if (value <= 0) throw ValidationException("$field must be positive")
        return value
    }

    fun validateNonNegative(value: Double, field: String): Double {
        if (value < 0) throw ValidationException("$field cannot be negative")
        return value
    }

    fun validateQuantity(quantity: Int): Int {
        if (quantity <= 0) throw ValidationException("Quantity must be at least 1")
        return quantity
    }

    fun validateRating(rating: Int): Int {
        if (rating !in 1..5) throw ValidationException("Rating must be between 1 and 5")
        return rating
    }
}
