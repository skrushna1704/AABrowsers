package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.InsufficientBalanceException
import com.kododake.ecommerce.exceptions.PaymentException
import com.kododake.ecommerce.models.PaymentMethod
import com.kododake.ecommerce.models.PaymentRequest
import com.kododake.ecommerce.models.PaymentResult
import com.kododake.ecommerce.models.TransactionType
import com.kododake.ecommerce.patterns.PaymentFactory
import com.kododake.ecommerce.repository.UserRepository

class PaymentService(
    private val userRepo: UserRepository = UserRepository.instance
) {
    fun processPayment(request: PaymentRequest): PaymentResult {
        val strategy = PaymentFactory.create(request.method) { customerId, amount, orderId ->
            debitWallet(customerId, amount, orderId)
        }
        return strategy.process(request)
    }

    fun debitWallet(customerId: String, amount: Double, orderId: String): Boolean {
        val customer = userRepo.findCustomerById(customerId) ?: return false
        return customer.wallet.debit(amount, TransactionType.PAYMENT, "Order payment", orderId)
    }

    fun refundToWallet(customerId: String, amount: Double, orderId: String) {
        val customer = userRepo.findCustomerById(customerId)
            ?: throw PaymentException("Customer not found")
        customer.wallet.credit(amount, TransactionType.REFUND, "Order refund", orderId)
    }

    fun validatePaymentMethod(method: PaymentMethod, amount: Double, customerId: String) {
        if (method == PaymentMethod.WALLET) {
            val customer = userRepo.findCustomerById(customerId)
                ?: throw PaymentException("Customer not found")
            if (customer.wallet.balance < amount) {
                throw InsufficientBalanceException("Wallet balance insufficient. Available: ₹${customer.wallet.balance}")
            }
        }
        if (method == PaymentMethod.CASH_ON_DELIVERY && amount > 50000) {
            throw PaymentException("COD not available for orders above ₹50,000")
        }
    }

    fun getWalletHistory(customerId: String): String {
        val customer = userRepo.findCustomerById(customerId) ?: return "Customer not found"
        return buildString {
            appendLine("=== WALLET HISTORY ===")
            appendLine("Balance: ₹${customer.wallet.balance}")
            customer.wallet.transactions.forEach { txn ->
                appendLine("${txn.timestamp} | ${txn.type} | ₹${txn.amount} | ${txn.description}")
            }
        }
    }
}
