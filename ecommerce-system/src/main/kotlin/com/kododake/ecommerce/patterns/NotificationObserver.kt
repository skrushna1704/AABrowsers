package com.kododake.ecommerce.patterns

import com.kododake.ecommerce.models.Notification
import com.kododake.ecommerce.models.NotificationType

interface NotificationObserver {
    fun onNotify(notification: Notification)
}

class ConsoleNotificationObserver : NotificationObserver {
    override fun onNotify(notification: Notification) {
        println("[NOTIFICATION] ${notification.type}: ${notification.message}")
    }
}

class NotificationSubject {
    private val observers = mutableListOf<NotificationObserver>()
    private val notifications = mutableListOf<Notification>()

    fun subscribe(observer: NotificationObserver) {
        observers.add(observer)
    }

    fun notify(userId: String, type: NotificationType, message: String) {
        val notification = Notification(
            id = "NTF${System.currentTimeMillis()}",
            userId = userId,
            type = type,
            message = message
        )
        notifications.add(notification)
        observers.forEach { it.onNotify(notification) }
    }

    fun getNotifications(userId: String): List<Notification> =
        notifications.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    fun markAsRead(notificationId: String) {
        notifications.find { it.id == notificationId }?.isRead = true
    }
}
