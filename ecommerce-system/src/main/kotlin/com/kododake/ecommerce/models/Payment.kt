package com.kododake.ecommerce.models

sealed class PaymentResult {
    data class Success(val transactionId: String, val message: String) : PaymentResult()
    data class Failure(val reason: String) : PaymentResult()
}

data class PaymentRequest(
    val orderId: String,
    val amount: Double,
    val method: PaymentMethod,
    val customerId: String,
    val upiId: String? = null,
    val cardNumber: String? = null
)

data class InventoryRecord(
    val productId: String,
    var stock: Int,
    var reserved: Int,
    var reorderLevel: Int = 5,
    val lastUpdated: java.time.LocalDateTime = java.time.LocalDateTime.now()
) {
    val available: Int get() = stock - reserved
}
