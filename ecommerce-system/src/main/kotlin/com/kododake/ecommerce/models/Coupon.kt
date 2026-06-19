package com.kododake.ecommerce.models

import java.time.LocalDate
import java.time.LocalDateTime

data class Coupon(
    val code: String,
    val type: CouponType,
    val discountValue: Double,
    val minPurchase: Double = 0.0,
    val maxDiscount: Double = Double.MAX_VALUE,
    val expiryDate: LocalDate,
    val oneTimeUse: Boolean = false,
    val usedBy: MutableSet<String> = mutableSetOf(),
    var isActive: Boolean = true,
    val description: String = ""
) {
    fun isExpired(): Boolean = LocalDate.now().isAfter(expiryDate)

    fun isUsedBy(userId: String): Boolean = oneTimeUse && userId in usedBy

    fun markUsed(userId: String) {
        if (oneTimeUse) usedBy.add(userId)
    }
}

data class DiscountResult(
    val originalAmount: Double,
    val discountAmount: Double,
    val finalAmount: Double,
    val discountSource: String
)
