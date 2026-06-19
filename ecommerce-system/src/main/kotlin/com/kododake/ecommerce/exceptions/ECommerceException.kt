package com.kododake.ecommerce.exceptions

sealed class ECommerceException(message: String) : Exception(message)

class ValidationException(message: String) : ECommerceException(message)

class AuthenticationException(message: String) : ECommerceException(message)

class InsufficientBalanceException(message: String) : ECommerceException(message)

class InsufficientStockException(message: String) : ECommerceException(message)

class CouponException(message: String) : ECommerceException(message)

class OrderException(message: String) : ECommerceException(message)

class PaymentException(message: String) : ECommerceException(message)

class NotFoundException(message: String) : ECommerceException(message)

class AccountLockedException(message: String) : ECommerceException(message)
