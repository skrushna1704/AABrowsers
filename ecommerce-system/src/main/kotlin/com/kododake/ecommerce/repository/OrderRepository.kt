package com.kododake.ecommerce.repository

import com.kododake.ecommerce.models.Order
import com.kododake.ecommerce.models.OrderStatus
import com.kododake.ecommerce.utils.Constants
import java.io.File
import java.util.LinkedList
import java.util.Queue

class OrderRepository private constructor() {
    private val orders = HashMap<String, Order>()
    private val customerOrders = HashMap<String, MutableList<String>>()
    private val processingQueue: Queue<String> = LinkedList()

    companion object {
        val instance: OrderRepository by lazy { OrderRepository() }
    }

    fun save(order: Order) {
        orders[order.id] = order
        customerOrders.getOrPut(order.customerId) { mutableListOf() }.add(order.id)
        if (order.status == OrderStatus.PENDING) {
            processingQueue.offer(order.id)
        }
    }

    fun findById(id: String): Order? = orders[id]

    fun findByCustomer(customerId: String): List<Order> =
        customerOrders[customerId]?.mapNotNull { orders[it] }?.sortedByDescending { it.createdAt }
            ?: emptyList()

    fun findAll(): List<Order> = orders.values.sortedByDescending { it.createdAt }

    fun findByStatus(status: OrderStatus): List<Order> =
        orders.values.filter { it.status == status }

    fun getCancelledOrders(): List<Order> =
        findByStatus(OrderStatus.CANCELLED)

    fun pollNextPending(): Order? {
        while (processingQueue.isNotEmpty()) {
            val id = processingQueue.poll()
            val order = orders[id]
            if (order != null && order.status == OrderStatus.PENDING) return order
        }
        return null
    }

    fun getTotalRevenue(): Double =
        orders.values
            .filter { it.status !in listOf(OrderStatus.CANCELLED, OrderStatus.RETURNED) }
            .sumOf { it.grandTotal }

    fun getMonthlySales(): Map<String, Double> =
        orders.values
            .filter { it.status !in listOf(OrderStatus.CANCELLED, OrderStatus.RETURNED) }
            .groupBy { "${it.createdAt.year}-${it.createdAt.monthValue.toString().padStart(2, '0')}" }
            .mapValues { (_, orderList) -> orderList.sumOf { it.grandTotal } }

    fun loadFromFile(dataDir: String) {
        val file = File(dataDir, Constants.ORDERS_FILE)
        if (!file.exists()) return
    }

    fun saveToFile(dataDir: String) {
        val dir = File(dataDir)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, Constants.ORDERS_FILE)
        file.writeText(buildString {
            appendLine("# Orders persisted on ${java.time.LocalDateTime.now()}")
            appendLine("# Total orders: ${orders.size}")
            orders.values.forEach { o ->
                appendLine("${o.id}|${o.customerId}|${o.grandTotal}|${o.status}|${o.createdAt}")
            }
        })
    }
}
