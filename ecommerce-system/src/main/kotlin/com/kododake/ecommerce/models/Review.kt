package com.kododake.ecommerce.models

import java.time.LocalDateTime

data class Review(
    val id: String,
    val productId: String,
    val customerId: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val message: String,
    var isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
