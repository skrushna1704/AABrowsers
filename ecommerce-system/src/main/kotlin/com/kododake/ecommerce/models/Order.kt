package com.kododake.ecommerce.models

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val gstRate: Double
) {
    val subtotal: Double get() = unitPrice * quantity
    val gstAmount: Double get() = subtotal * gstRate / 100
    val total: Double get() = subtotal + gstAmount
}

data class Order(
    val id: String,
    val customerId: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val discount: Double,
    val gstTotal: Double,
    val shippingCharge: Double,
    val grandTotal: Double,
    val paymentMethod: PaymentMethod,
    var status: OrderStatus,
    val shippingAddress: Address,
    val couponCode: String? = null,
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    var shipment: Shipment? = null,
    var invoiceId: String? = null
)

data class Shipment(
    val trackingId: String,
    var status: ShipmentStatus,
    val estimatedDelivery: java.time.LocalDate,
    val updates: MutableList<Pair<java.time.LocalDateTime, String>> = mutableListOf()
) {
    fun addUpdate(message: String) {
        updates.add(java.time.LocalDateTime.now() to message)
    }
}
