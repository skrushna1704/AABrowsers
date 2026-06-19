package com.kododake.ecommerce.models

import java.time.LocalDateTime

data class Invoice(
    val invoiceNumber: String,
    val orderId: String,
    val customerName: String,
    val customerEmail: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val discount: Double,
    val gstBreakdown: Map<String, Double>,
    val gstTotal: Double,
    val shippingCharge: Double,
    val grandTotal: Double,
    val paymentMethod: PaymentMethod,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)
