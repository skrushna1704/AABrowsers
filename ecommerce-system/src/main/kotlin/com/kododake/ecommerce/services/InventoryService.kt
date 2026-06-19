package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.InsufficientStockException
import com.kododake.ecommerce.exceptions.NotFoundException
import com.kododake.ecommerce.models.InventoryRecord
import com.kododake.ecommerce.models.Product
import com.kododake.ecommerce.patterns.ConsoleNotificationObserver
import com.kododake.ecommerce.patterns.NotificationSubject
import com.kododake.ecommerce.models.NotificationType
import com.kododake.ecommerce.repository.ProductRepository
import com.kododake.ecommerce.utils.Constants

class InventoryService(
    private val productRepo: ProductRepository = ProductRepository.instance,
    private val notificationSubject: NotificationSubject = sharedNotifications
) {
    private val inventory = HashMap<String, InventoryRecord>()

    companion object {
        val sharedNotifications = NotificationSubject().apply {
            subscribe(ConsoleNotificationObserver())
        }
    }

    init {
        productRepo.findAllIncludingInactive().forEach { product ->
            inventory[product.id] = InventoryRecord(
                productId = product.id,
                stock = product.stock,
                reserved = product.reservedStock,
                reorderLevel = Constants.LOW_STOCK_THRESHOLD
            )
        }
    }

    fun syncFromProducts() {
        productRepo.findAllIncludingInactive().forEach { product ->
            inventory[product.id] = InventoryRecord(
                productId = product.id,
                stock = product.stock,
                reserved = product.reservedStock
            )
        }
    }

    fun reserveStock(productId: String, quantity: Int): Boolean {
        val product = productRepo.findById(productId) ?: throw NotFoundException("Product not found")
        if (!product.reserve(quantity)) {
            throw InsufficientStockException("Insufficient stock for ${product.name}. Available: ${product.availableStock}")
        }
        inventory[productId]?.reserved = product.reservedStock
        return true
    }

    fun releaseStock(productId: String, quantity: Int) {
        productRepo.findById(productId)?.releaseReservation(quantity)
        inventory[productId]?.let { it.reserved = (it.reserved - quantity).coerceAtLeast(0) }
    }

    fun deductStock(productId: String, quantity: Int) {
        val product = productRepo.findById(productId) ?: throw NotFoundException("Product not found")
        product.deductStock(quantity)
        inventory[productId]?.apply {
            stock = product.stock
            reserved = product.reservedStock
        }
        checkLowStock(product)
    }

    fun addStock(productId: String, quantity: Int) {
        val product = productRepo.findById(productId) ?: throw NotFoundException("Product not found")
        product.stock += quantity
        inventory[productId]?.stock = product.stock
    }

    fun getLowStockAlerts(): List<Product> = productRepo.getLowStockProducts()

    private fun checkLowStock(product: Product) {
        if (product.isLowStock()) {
            notificationSubject.notify(
                "ADMIN",
                NotificationType.LOW_STOCK,
                "Low stock alert: ${product.name} has only ${product.availableStock} units left"
            )
        }
    }

    fun getInventoryReport(): String = buildString {
        appendLine("=== INVENTORY REPORT ===")
        productRepo.findAll().forEach { p ->
            appendLine("${p.name}: Stock=${p.stock}, Reserved=${p.reservedStock}, Available=${p.availableStock}")
        }
    }
}
