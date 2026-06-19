package com.kododake.ecommerce.services

import com.kododake.ecommerce.models.OrderStatus
import com.kododake.ecommerce.repository.OrderRepository
import com.kododake.ecommerce.repository.ProductRepository
import com.kododake.ecommerce.repository.UserRepository
import com.kododake.ecommerce.utils.Algorithms
import com.kododake.ecommerce.utils.Constants

class ReportService(
    private val orderRepo: OrderRepository = OrderRepository.instance,
    private val productRepo: ProductRepository = ProductRepository.instance,
    private val userRepo: UserRepository = UserRepository.instance
) {
    fun salesReport(): String = buildString {
        appendLine("=== SALES REPORT ===")
        val orders = orderRepo.findAll().filter {
            it.status !in listOf(OrderStatus.CANCELLED, OrderStatus.RETURNED)
        }
        appendLine("Total Orders: ${orders.size}")
        appendLine("Total Revenue: ₹${"%.2f".format(orders.sumOf { it.grandTotal })}")
        appendLine("Total Discount Given: ₹${"%.2f".format(orders.sumOf { it.discount })}")
        appendLine("Average Order Value: ₹${"%.2f".format(if (orders.isNotEmpty()) orders.sumOf { it.grandTotal } / orders.size else 0.0)}")
    }

    fun revenueReport(): String {
        val productRevenue = productRepo.findAll().map { it.name to it.totalSold * it.price }
        val sorted = Algorithms.quickSortByRevenue(productRevenue)
        return buildString {
            appendLine("=== REVENUE REPORT (Quick Sort) ===")
            sorted.forEach { (name, revenue) ->
                appendLine("$name: ₹${"%.2f".format(revenue)}")
            }
            appendLine("-".repeat(30))
            appendLine("Total: ₹${"%.2f".format(sorted.sumOf { it.second })}")
        }
    }

    fun gstReport(): String = buildString {
        appendLine("=== GST REPORT ===")
        val orders = orderRepo.findAll().filter {
            it.status !in listOf(OrderStatus.CANCELLED, OrderStatus.RETURNED)
        }
        val totalGst = orders.sumOf { it.gstTotal }
        appendLine("Total GST Collected: ₹${"%.2f".format(totalGst)}")
        orders.flatMap { it.items }.groupBy { it.gstRate }.forEach { (rate, items) ->
            appendLine("GST @ $rate%: ₹${"%.2f".format(items.sumOf { it.gstAmount })}")
        }
    }

    fun topProductsReport(limit: Int = 5): String = buildString {
        appendLine("=== TOP SELLING PRODUCTS (Priority Queue) ===")
        productRepo.getTopSelling(limit).forEachIndexed { i, p ->
            appendLine("${i + 1}. ${p.name} - Sold: ${p.totalSold} - Revenue: ₹${p.totalSold * p.price}")
        }
    }

    fun worstProductsReport(): String = buildString {
        appendLine("=== WORST SELLING PRODUCTS ===")
        productRepo.findAll().sortedBy { it.totalSold }.take(5).forEach { p ->
            appendLine("${p.name} - Sold: ${p.totalSold}")
        }
    }

    fun customerRanking(): String = buildString {
        appendLine("=== CUSTOMER RANKING ===")
        userRepo.getAllCustomers()
            .sortedByDescending { it.totalSpent }
            .take(10)
            .forEachIndexed { i, c ->
                appendLine("${i + 1}. ${c.name} - Spent: ₹${c.totalSpent} | Points: ${c.loyaltyPoints}")
            }
    }

    fun lowStockReport(): String = buildString {
        appendLine("=== LOW STOCK ALERT ===")
        productRepo.getLowStockProducts().forEach { p ->
            appendLine("⚠ ${p.name}: ${p.availableStock} units remaining")
        }
        if (productRepo.getLowStockProducts().isEmpty()) appendLine("All products well stocked")
    }

    fun monthlySalesReport(): String = buildString {
        appendLine("=== MONTHLY SALES ===")
        orderRepo.getMonthlySales().forEach { (month, revenue) ->
            appendLine("$month: ₹${"%.2f".format(revenue)}")
        }
    }

    fun cancelledOrdersReport(): String = buildString {
        appendLine("=== CANCELLED ORDERS ===")
        orderRepo.getCancelledOrders().forEach { o ->
            appendLine("${o.id} | Customer: ${o.customerId} | Amount: ₹${o.grandTotal}")
        }
        if (orderRepo.getCancelledOrders().isEmpty()) appendLine("No cancelled orders")
    }

    fun adminDashboard(): String = buildString {
        appendLine("╔══════════════════════════════════════╗")
        appendLine("║         ADMIN DASHBOARD              ║")
        appendLine("╠══════════════════════════════════════╣")
        appendLine("║ Total Revenue    : ₹${"%.2f".format(orderRepo.getTotalRevenue())}")
        appendLine("║ Total Products   : ${productRepo.findAll().size}")
        appendLine("║ Total Customers  : ${userRepo.getAllCustomers().size}")
        appendLine("║ Total Orders     : ${orderRepo.findAll().size}")
        appendLine("║ Cancelled Orders : ${orderRepo.getCancelledOrders().size}")
        appendLine("║ Low Stock Items  : ${productRepo.getLowStockProducts().size}")
        appendLine("╚══════════════════════════════════════╝")
    }

    fun profitEstimate(): String {
        val revenue = orderRepo.getTotalRevenue()
        val estimatedCost = revenue * 0.65
        val profit = revenue - estimatedCost
        return buildString {
            appendLine("=== PROFIT ESTIMATE ===")
            appendLine("Revenue: ₹${"%.2f".format(revenue)}")
            appendLine("Est. Cost (65%): ₹${"%.2f".format(estimatedCost)}")
            appendLine("Est. Profit: ₹${"%.2f".format(profit)}")
        }
    }
}
