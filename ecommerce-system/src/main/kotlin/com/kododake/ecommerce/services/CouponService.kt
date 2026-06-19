package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.CouponException
import com.kododake.ecommerce.models.Coupon
import com.kododake.ecommerce.models.CouponType
import com.kododake.ecommerce.models.Customer
import com.kododake.ecommerce.models.DiscountResult
import com.kododake.ecommerce.utils.Constants
import java.time.LocalDate

class CouponService {
    private val coupons = HashMap<String, Coupon>()
    private val usedCoupons = HashSet<String>()

    init {
        seedCoupons()
    }

    private fun seedCoupons() {
        listOf(
            Coupon("WELCOME100", CouponType.FLAT, 100.0, minPurchase = 500.0,
                maxDiscount = 100.0, expiryDate = LocalDate.now().plusMonths(6),
                oneTimeUse = true, description = "₹100 off on first order"),
            Coupon("SAVE10", CouponType.PERCENTAGE, 10.0, minPurchase = 1000.0,
                maxDiscount = 2000.0, expiryDate = LocalDate.now().plusMonths(3),
                description = "10% off"),
            Coupon("FLAT500", CouponType.FLAT, 500.0, minPurchase = 3000.0,
                maxDiscount = 500.0, expiryDate = LocalDate.now().plusMonths(2),
                description = "Flat ₹500 off"),
            Coupon("B2G1", CouponType.BUY_TWO_GET_ONE, 33.33, minPurchase = 0.0,
                expiryDate = LocalDate.now().plusMonths(1),
                description = "Buy 2 Get 1 Free"),
            Coupon("FESTIVAL25", CouponType.FESTIVAL, 25.0, minPurchase = 2000.0,
                maxDiscount = 5000.0, expiryDate = LocalDate.now().plusDays(15),
                description = "Festival special 25% off")
        ).forEach { coupons[it.code.uppercase()] = it }
    }

    fun getCoupon(code: String): Coupon? = coupons[code.uppercase()]

    fun validateCoupon(code: String, customerId: String, cartTotal: Double): Coupon {
        val coupon = coupons[code.uppercase()] ?: throw CouponException("Invalid coupon code")
        if (!coupon.isActive) throw CouponException("Coupon is inactive")
        if (coupon.isExpired()) throw CouponException("Coupon has expired")
        if (coupon.isUsedBy(customerId)) throw CouponException("Coupon already used")
        if (cartTotal < coupon.minPurchase) {
            throw CouponException("Minimum purchase of ₹${coupon.minPurchase} required")
        }
        return coupon
    }

    fun applyCoupon(coupon: Coupon, cartTotal: Double, itemCount: Int): Double = when (coupon.type) {
        CouponType.PERCENTAGE, CouponType.FESTIVAL -> {
            (cartTotal * coupon.discountValue / 100).coerceAtMost(coupon.maxDiscount)
        }
        CouponType.FLAT -> coupon.discountValue.coerceAtMost(coupon.maxDiscount)
        CouponType.BUY_TWO_GET_ONE -> {
            if (itemCount >= 3) cartTotal * coupon.discountValue / 100 else 0.0
        }
    }

    fun calculateDynamicDiscount(customer: Customer, cartTotal: Double, couponDiscount: Double): DiscountResult {
        val (discount, source) = when {
            cartTotal >= Constants.HIGH_CART_THRESHOLD ->
                cartTotal * Constants.HIGH_CART_DISCOUNT_PERCENT / 100 to "High cart discount (15%)"
            customer.isLoyalCustomer ->
                cartTotal * Constants.LOYAL_DISCOUNT_PERCENT / 100 to "Loyal customer discount (12%)"
            couponDiscount > 0 ->
                couponDiscount to "Coupon discount"
            else ->
                cartTotal * Constants.DEFAULT_DISCOUNT_PERCENT / 100 to "Standard discount (5%)"
        }
        val finalDiscount = maxOf(discount, couponDiscount).coerceAtMost(cartTotal)
        return DiscountResult(cartTotal, finalDiscount, cartTotal - finalDiscount, source)
    }

    fun markCouponUsed(code: String, customerId: String) {
        coupons[code.uppercase()]?.markUsed(customerId)
        usedCoupons.add("$code:$customerId")
    }

    fun listActiveCoupons(): List<Coupon> =
        coupons.values.filter { it.isActive && !it.isExpired() }

    fun addCoupon(coupon: Coupon) {
        coupons[coupon.code.uppercase()] = coupon
    }
}
