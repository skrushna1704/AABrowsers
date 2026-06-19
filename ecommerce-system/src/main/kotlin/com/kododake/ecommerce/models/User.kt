package com.kododake.ecommerce.models

import java.time.LocalDateTime

abstract class User(
    open val id: String,
    open val name: String,
    open val email: String,
    open var passwordHash: String,
    open val role: UserRole,
    open val createdAt: LocalDateTime = LocalDateTime.now()
) {
    abstract fun displayProfile(): String
}

data class Customer(
    override val id: String,
    override val name: String,
    override val email: String,
    override var passwordHash: String,
    var phone: String = "",
    var wallet: Wallet = Wallet(),
    var loyaltyPoints: Int = 0,
    var totalSpent: Double = 0.0,
    val addresses: MutableList<Address> = mutableListOf(),
    val wishlist: MutableSet<String> = mutableSetOf(),
    var isVerified: Boolean = false,
    var failedLoginAttempts: Int = 0,
    var isLocked: Boolean = false
) : User(id, name, email, passwordHash, UserRole.CUSTOMER) {

    val isLoyalCustomer: Boolean get() = totalSpent >= 5000.0

    override fun displayProfile(): String = buildString {
        appendLine("Customer: $name ($email)")
        appendLine("Phone: $phone")
        appendLine("Wallet: ${wallet.balance}")
        appendLine("Loyalty Points: $loyaltyPoints")
        appendLine("Total Spent: $totalSpent")
        appendLine("Loyal Customer: $isLoyalCustomer")
    }

    fun addLoyaltyPoints(amount: Double) {
        loyaltyPoints += (amount / 100).toInt() * 1
    }
}

data class Admin(
    override val id: String,
    override val name: String,
    override val email: String,
    override var passwordHash: String,
    val department: String = "Operations"
) : User(id, name, email, passwordHash, UserRole.ADMIN) {

    override fun displayProfile(): String =
        "Admin: $name ($email) - $department"
}
