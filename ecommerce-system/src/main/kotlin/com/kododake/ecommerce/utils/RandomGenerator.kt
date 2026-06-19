package com.kododake.ecommerce.utils

import java.util.UUID
import kotlin.random.Random

object RandomGenerator {
    fun accountId(): String = "USR${System.currentTimeMillis()}${Random.nextInt(1000, 9999)}"

    fun orderId(): String = "ORD${UUID.randomUUID().toString().take(8).uppercase()}"

    fun transactionId(): String = "TXN${System.currentTimeMillis()}"

    fun invoiceNumber(): String = "INV-${System.currentTimeMillis()}"

    fun otp(): String = (1..Constants.OTP_LENGTH)
        .map { Random.nextInt(0, 10) }
        .joinToString("")

    fun productId(): String = "PRD${Random.nextInt(10000, 99999)}"

    fun couponId(): String = "CPN${Random.nextInt(1000, 9999)}"
}
