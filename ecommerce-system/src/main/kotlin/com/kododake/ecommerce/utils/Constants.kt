package com.kododake.ecommerce.utils

object Constants {
    const val DATA_DIR = "data"
    const val USERS_FILE = "users.txt"
    const val PRODUCTS_FILE = "products.txt"
    const val ORDERS_FILE = "orders.txt"
    const val COUPONS_FILE = "coupons.txt"

    const val MIN_PASSWORD_LENGTH = 6
    const val OTP_LENGTH = 6
    const val OTP_EXPIRY_MINUTES = 10
    const val LOYALTY_POINTS_PER_100 = 1
    const val LOYALTY_REDEEM_THRESHOLD = 100
    const val LOYALTY_CASHBACK_AMOUNT = 100.0
    const val LOYAL_CUSTOMER_THRESHOLD = 5000.0
    const val HIGH_CART_THRESHOLD = 10000.0
    const val DEFAULT_DISCOUNT_PERCENT = 5.0
    const val LOYAL_DISCOUNT_PERCENT = 12.0
    const val HIGH_CART_DISCOUNT_PERCENT = 15.0
    const val LOW_STOCK_THRESHOLD = 5
    const val CURRENCY_SYMBOL = "₹"

    val GST_RATES = mapOf(
        "Electronics" to 18.0,
        "Fashion" to 12.0,
        "Food" to 5.0,
        "Books" to 0.0,
        "Medicine" to 5.0,
        "General" to 12.0
    )

    fun shippingCharge(weightKg: Double): Double = when {
        weightKg <= 1.0 -> 50.0
        weightKg <= 5.0 -> 100.0
        weightKg <= 20.0 -> 200.0
        else -> 500.0
    }
}
