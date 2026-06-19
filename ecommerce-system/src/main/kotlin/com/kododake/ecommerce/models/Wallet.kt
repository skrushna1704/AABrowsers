package com.kododake.ecommerce.models

import java.time.LocalDateTime

data class Wallet(
    var balance: Double = 0.0,
    val transactions: MutableList<Transaction> = mutableListOf()
) {
    fun credit(amount: Double, type: TransactionType, description: String, orderId: String? = null) {
        balance += amount
        transactions.add(Transaction(
            id = "TXN${System.currentTimeMillis()}",
            type = type,
            amount = amount,
            description = description,
            orderId = orderId
        ))
    }

    fun debit(amount: Double, type: TransactionType, description: String, orderId: String? = null): Boolean {
        if (balance < amount) return false
        balance -= amount
        transactions.add(Transaction(
            id = "TXN${System.currentTimeMillis()}",
            type = type,
            amount = -amount,
            description = description,
            orderId = orderId
        ))
        return true
    }
}

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val orderId: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
