package com.kododake.ecommerce.patterns

import com.kododake.ecommerce.models.PaymentMethod
import com.kododake.ecommerce.models.PaymentRequest
import com.kododake.ecommerce.models.PaymentResult
import com.kododake.ecommerce.utils.RandomGenerator

interface PaymentStrategy {
    val method: PaymentMethod
    fun process(request: PaymentRequest): PaymentResult
}

class UpiPaymentStrategy : PaymentStrategy {
    override val method = PaymentMethod.UPI

    override fun process(request: PaymentRequest): PaymentResult {
        val upiId = request.upiId
        if (upiId.isNullOrBlank() || !upiId.contains("@")) {
            return PaymentResult.Failure("Invalid UPI ID")
        }
        return PaymentResult.Success(
            RandomGenerator.transactionId(),
            "UPI payment of ₹${request.amount} successful via $upiId"
        )
    }
}

class CreditCardPaymentStrategy : PaymentStrategy {
    override val method = PaymentMethod.CREDIT_CARD

    override fun process(request: PaymentRequest): PaymentResult {
        val card = request.cardNumber?.replace(" ", "") ?: ""
        if (card.length !in 13..19 || !card.all { it.isDigit() }) {
            return PaymentResult.Failure("Invalid card number")
        }
        return PaymentResult.Success(
            RandomGenerator.transactionId(),
            "Card payment of ₹${request.amount} authorized (****${card.takeLast(4)})"
        )
    }
}

class WalletPaymentStrategy(
    private val walletDebit: (String, Double, String) -> Boolean
) : PaymentStrategy {
    override val method = PaymentMethod.WALLET

    override fun process(request: PaymentRequest): PaymentResult {
        val success = walletDebit(request.customerId, request.amount, request.orderId)
        return if (success) {
            PaymentResult.Success(
                RandomGenerator.transactionId(),
                "Wallet payment of ₹${request.amount} successful"
            )
        } else {
            PaymentResult.Failure("Insufficient wallet balance")
        }
    }
}

class NetBankingPaymentStrategy : PaymentStrategy {
    override val method = PaymentMethod.NET_BANKING

    override fun process(request: PaymentRequest): PaymentResult {
        return PaymentResult.Success(
            RandomGenerator.transactionId(),
            "Net banking payment of ₹${request.amount} processed"
        )
    }
}

class CashOnDeliveryStrategy : PaymentStrategy {
    override val method = PaymentMethod.CASH_ON_DELIVERY

    override fun process(request: PaymentRequest): PaymentResult {
        return PaymentResult.Success(
            RandomGenerator.transactionId(),
            "COD order confirmed. Pay ₹${request.amount} on delivery"
        )
    }
}

object PaymentFactory {
    fun create(
        method: PaymentMethod,
        walletDebit: (String, Double, String) -> Boolean
    ): PaymentStrategy = when (method) {
        PaymentMethod.UPI -> UpiPaymentStrategy()
        PaymentMethod.CREDIT_CARD -> CreditCardPaymentStrategy()
        PaymentMethod.WALLET -> WalletPaymentStrategy(walletDebit)
        PaymentMethod.NET_BANKING -> NetBankingPaymentStrategy()
        PaymentMethod.CASH_ON_DELIVERY -> CashOnDeliveryStrategy()
    }
}
