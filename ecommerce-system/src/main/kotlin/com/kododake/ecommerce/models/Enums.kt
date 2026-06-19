package com.kododake.ecommerce.models

enum class UserRole { CUSTOMER, ADMIN }

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, RETURNED, EXCHANGED
}

enum class PaymentMethod {
    UPI, CREDIT_CARD, WALLET, NET_BANKING, CASH_ON_DELIVERY
}

enum class PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }

enum class TransactionType {
    DEPOSIT, WITHDRAW, PAYMENT, REFUND, CASHBACK, RECHARGE
}

enum class CouponType {
    PERCENTAGE, FLAT, BUY_TWO_GET_ONE, FESTIVAL
}

enum class NotificationType {
    ORDER_PLACED, ORDER_SHIPPED, ORDER_DELIVERED,
    LOW_STOCK, PAYMENT_SUCCESS, PAYMENT_FAILED, PROMOTION
}

enum class ShipmentStatus { PROCESSING, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED }
