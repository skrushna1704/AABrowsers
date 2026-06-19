package com.kododake.ecommerce.models

data class Category(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val description: String = ""
)

data class Product(
    val id: String,
    var name: String,
    var description: String,
    var price: Double,
    var category: String,
    var brand: String,
    var stock: Int,
    var weightKg: Double,
    var rating: Double = 0.0,
    var reviewCount: Int = 0,
    var reservedStock: Int = 0,
    var totalSold: Int = 0,
    var isActive: Boolean = true
) {
    val availableStock: Int get() = stock - reservedStock

    fun reserve(quantity: Int): Boolean {
        if (quantity > availableStock) return false
        reservedStock += quantity
        return true
    }

    fun releaseReservation(quantity: Int) {
        reservedStock = (reservedStock - quantity).coerceAtLeast(0)
    }

    fun deductStock(quantity: Int) {
        stock -= quantity
        reservedStock = (reservedStock - quantity).coerceAtLeast(0)
        totalSold += quantity
    }

    fun isLowStock(threshold: Int = 5): Boolean = availableStock <= threshold

    override fun toString(): String =
        "$name | ${brand} | ₹$price | Stock: $availableStock | ★$rating ($reviewCount)"
}
