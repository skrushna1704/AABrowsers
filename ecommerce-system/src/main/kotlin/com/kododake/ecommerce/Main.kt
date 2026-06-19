package com.kododake.ecommerce

import com.kododake.ecommerce.exceptions.ECommerceException
import com.kododake.ecommerce.models.PaymentMethod
import com.kododake.ecommerce.models.UserRole
import com.kododake.ecommerce.repository.OrderRepository
import com.kododake.ecommerce.repository.ProductRepository
import com.kododake.ecommerce.repository.UserRepository
import com.kododake.ecommerce.services.*
import com.kododake.ecommerce.utils.Constants
import java.io.File

fun main() {
    ECommerceApp().run()
}

class ECommerceApp {
    private val dataDir = File(Constants.DATA_DIR).also { it.mkdirs() }.absolutePath

    private val loginService = LoginService()
    private val productService = ProductService()
    private val cartService = CartService()
    private val orderService = OrderService()
    private val couponService = CouponService()
    private val paymentService = PaymentService()
    private val inventoryService = InventoryService()
    private val reportService = ReportService()
    private val reviewService = ReviewService()

    init {
        UserRepository.instance.loadFromFile(dataDir)
        ProductRepository.instance.loadFromFile(dataDir)
        OrderRepository.instance.loadFromFile(dataDir)
    }

    fun run() {
        printBanner()
        while (true) {
            try {
                if (!loginService.isLoggedIn()) {
                    if (!mainMenu()) break
                } else {
                    when (loginService.getCurrentUser()?.role) {
                        UserRole.CUSTOMER -> if (!customerMenu()) loginService.logout()
                        UserRole.ADMIN -> if (!adminMenu()) loginService.logout()
                        else -> loginService.logout()
                    }
                }
            } catch (e: ECommerceException) {
                println("\n❌ Error: ${e.message}")
            } catch (e: Exception) {
                println("\n❌ Unexpected error: ${e.message}")
            }
            println()
        }
        saveData()
        println("Thank you for using E-Commerce Management System!")
    }

    private fun printBanner() {
        println("""
            ╔══════════════════════════════════════════════════╗
            ║     E-COMMERCE MANAGEMENT SYSTEM v1.0            ║
            ║     Advanced Kotlin Console Application          ║
            ║     Part of AABrowser Repository                 ║
            ╚══════════════════════════════════════════════════╝
        """.trimIndent())
    }

    private fun mainMenu(): Boolean {
        println("""
            ┌─────────────────────────┐
            │      MAIN MENU          │
            ├─────────────────────────┤
            │ 1. Customer Login       │
            │ 2. Customer Register    │
            │ 3. Admin Login          │
            │ 4. Forgot Password      │
            │ 0. Exit                 │
            └─────────────────────────┘
        """.trimIndent())
        return when (readChoice("Choice")) {
            1 -> { customerLogin(); true }
            2 -> { customerRegister(); true }
            3 -> { adminLogin(); true }
            4 -> { forgotPasswordFlow(); true }
            0 -> false
            else -> { println("Invalid choice"); true }
        }
    }

    private fun customerLogin() {
        print("Email: ")
        val email = readLine()?.trim() ?: return
        print("Password: ")
        val password = readLine()?.trim() ?: return
        val user = loginService.login(email, password)
        println("✅ Welcome, ${user.name}!")
    }

    private fun adminLogin() {
        print("Admin Email: ")
        val email = readLine()?.trim() ?: return
        print("Password: ")
        val password = readLine()?.trim() ?: return
        loginService.login(email, password)
        println("✅ Admin logged in. Default: admin@ecommerce.com / admin123")
    }

    private fun customerRegister() {
        print("Name: "); val name = readLine()?.trim() ?: return
        print("Email: "); val email = readLine()?.trim() ?: return
        print("Phone: "); val phone = readLine()?.trim() ?: return
        print("Password: "); val password = readLine()?.trim() ?: return
        val customer = loginService.registerCustomer(name, email, password, phone)
        println("✅ Registered! Your ID: ${customer.id}")
        loginService.login(email, password)
    }

    private fun forgotPasswordFlow() {
        print("Email: "); val email = readLine()?.trim() ?: return
        val otp = loginService.forgotPassword(email)
        println("📧 OTP sent (simulated): $otp")
        print("Enter OTP: "); val enteredOtp = readLine()?.trim() ?: return
        print("New Password: "); val newPass = readLine()?.trim() ?: return
        loginService.resetPassword(email, enteredOtp, newPass)
        println("✅ Password reset successful!")
    }

    private fun customerMenu(): Boolean {
        val customer = loginService.getCurrentCustomer() ?: return false
        println("""
            ┌─────────────────────────┐
            │   CUSTOMER MENU         │
            │   Hello, ${customer.name.take(15)}!
            ├─────────────────────────┤
            │  1. Browse Products     │
            │  2. Search Products     │
            │  3. Filter & Sort       │
            │  4. View Cart           │
            │  5. Add to Cart         │
            │  6. Remove from Cart    │
            │  7. Update Quantity     │
            │  8. Undo Cart           │
            │  9. Wishlist            │
            │ 10. Apply Coupon Preview│
            │ 11. Checkout            │
            │ 12. Order History       │
            │ 13. Track Order         │
            │ 14. Cancel Order        │
            │ 15. Return Order        │
            │ 16. Wallet              │
            │ 17. Loyalty Points      │
            │ 18. Address Book        │
            │ 19. Write Review        │
            │ 20. My Profile          │
            │ 21. Change Password     │
            │ 22. Notifications       │
            │  0. Logout              │
            └─────────────────────────┘
        """.trimIndent())
        when (readChoice("Choice")) {
            1 -> browseProducts()
            2 -> searchProducts()
            3 -> filterSortProducts()
            4 -> println(cartService.displayCart(customer.id))
            5 -> addToCartFlow(customer.id)
            6 -> removeFromCartFlow(customer.id)
            7 -> updateQuantityFlow(customer.id)
            8 -> println(if (cartService.undo(customer.id)) "✅ Cart restored" else "Nothing to undo")
            9 -> wishlistFlow(customer)
            10 -> couponPreview(customer.id)
            11 -> checkoutFlow(customer)
            12 -> orderHistory(customer.id)
            13 -> trackOrderFlow()
            14 -> cancelOrderFlow(customer.id)
            15 -> returnOrderFlow(customer.id)
            16 -> walletMenu(customer.id)
            17 -> loyaltyFlow(customer.id)
            18 -> addressBookFlow(customer.id)
            19 -> reviewFlow(customer)
            20 -> println(customer.displayProfile())
            21 -> changePasswordFlow()
            22 -> notificationsFlow(customer.id)
            0 -> return false
            else -> println("Invalid choice")
        }
        return true
    }

    private fun adminMenu(): Boolean {
        println("""
            ┌─────────────────────────┐
            │     ADMIN MENU          │
            ├─────────────────────────┤
            │  1. Dashboard           │
            │  2. Add Product         │
            │  3. Update Product      │
            │  4. Delete Product      │
            │  5. View All Products   │
            │  6. Inventory Report    │
            │  7. Add Stock           │
            │  8. Sales Report        │
            │  9. Revenue Report      │
            │ 10. GST Report          │
            │ 11. Top Products        │
            │ 12. Customer Ranking    │
            │ 13. Low Stock Alert     │
            │ 14. Monthly Sales       │
            │ 15. Cancelled Orders    │
            │ 16. Profit Estimate     │
            │ 17. Manage Coupons      │
            │ 18. Process Orders      │
            │ 19. View Customers      │
            │  0. Logout              │
            └─────────────────────────┘
        """.trimIndent())
        when (readChoice("Choice")) {
            1 -> println(reportService.adminDashboard())
            2 -> adminAddProduct()
            3 -> adminUpdateProduct()
            4 -> adminDeleteProduct()
            5 -> browseProducts()
            6 -> println(inventoryService.getInventoryReport())
            7 -> adminAddStock()
            8 -> println(reportService.salesReport())
            9 -> println(reportService.revenueReport())
            10 -> println(reportService.gstReport())
            11 -> println(reportService.topProductsReport())
            12 -> println(reportService.customerRanking())
            13 -> println(reportService.lowStockReport())
            14 -> println(reportService.monthlySalesReport())
            15 -> println(reportService.cancelledOrdersReport())
            16 -> println(reportService.profitEstimate())
            17 -> adminCoupons()
            18 -> adminProcessOrders()
            19 -> viewCustomers()
            0 -> return false
            else -> println("Invalid choice")
        }
        return true
    }

    private fun browseProducts() {
        println("=== ALL PRODUCTS ===")
        productService.getAllProducts().forEachIndexed { i, p ->
            println("${i + 1}. [${p.id}] $p")
        }
    }

    private fun searchProducts() {
        print("Search query: ")
        val query = readLine()?.trim() ?: return
        val results = productService.searchProducts(query)
        if (results.isEmpty()) println("No products found")
        else results.forEach { println(it) }
    }

    private fun filterSortProducts() {
        println("1. Sort by Price (Low-High)  2. Sort by Price (High-Low)")
        println("3. Filter by Category  4. Filter Available Only")
        when (readChoice("Choice")) {
            1 -> productService.sortByPrice(true).forEach { println(it) }
            2 -> productService.sortByPrice(false).forEach { println(it) }
            3 -> {
                println("Categories: ${productService.getCategories().joinToString()}")
                print("Category: "); val cat = readLine()?.trim() ?: return
                productService.filterProducts(category = cat).forEach { println(it) }
            }
            4 -> productService.filterProducts(availableOnly = true).forEach { println(it) }
        }
    }

    private fun addToCartFlow(customerId: String) {
        print("Product ID: "); val pid = readLine()?.trim() ?: return
        print("Quantity: "); val qty = readLine()?.trim()?.toIntOrNull() ?: return
        cartService.addToCart(customerId, pid, qty)
        println("✅ Added to cart")
        println(cartService.displayCart(customerId))
    }

    private fun removeFromCartFlow(customerId: String) {
        print("Product ID: "); val pid = readLine()?.trim() ?: return
        cartService.removeFromCart(customerId, pid)
        println("✅ Removed from cart")
    }

    private fun updateQuantityFlow(customerId: String) {
        print("Product ID: "); val pid = readLine()?.trim() ?: return
        print("New Quantity: "); val qty = readLine()?.trim()?.toIntOrNull() ?: return
        cartService.updateQuantity(customerId, pid, qty)
        println("✅ Quantity updated")
    }

    private fun wishlistFlow(customer: com.kododake.ecommerce.models.Customer) {
        println("1. Add  2. Remove  3. View")
        when (readChoice("Choice")) {
            1 -> { print("Product ID: "); val pid = readLine()?.trim() ?: return; cartService.addToWishlist(customer, pid); println("✅ Added") }
            2 -> { print("Product ID: "); val pid = readLine()?.trim() ?: return; cartService.removeFromWishlist(customer, pid); println("✅ Removed") }
            3 -> {
                customer.wishlist.forEach { id ->
                    ProductRepository.instance.findById(id)?.let { println(it) }
                }
                if (customer.wishlist.isEmpty()) println("Wishlist empty")
            }
        }
    }

    private fun couponPreview(customerId: String) {
        println("Active Coupons:")
        couponService.listActiveCoupons().forEach { println("  ${it.code}: ${it.description}") }
        print("Coupon code (or Enter to skip): ")
        val code = readLine()?.trim() ?: return
        if (code.isBlank()) return
        val subtotal = cartService.calculateSubtotal(cartService.getOrCreateCart(customerId))
        try {
            val coupon = couponService.validateCoupon(code, customerId, subtotal)
            val discount = couponService.applyCoupon(coupon, subtotal, cartService.getOrCreateCart(customerId).totalItems())
            println("✅ Coupon valid! Discount: ₹$discount")
        } catch (e: ECommerceException) {
            println("❌ ${e.message}")
        }
    }

    private fun checkoutFlow(customer: com.kododake.ecommerce.models.Customer) {
        println(cartService.displayCart(customer.id))
        if (customer.addresses.isEmpty()) {
            println("⚠ No address found. Adding one now...")
            addressBookFlow(customer.id)
        }
        print("Coupon code (optional): "); val coupon = readLine()?.trim()?.ifBlank { null }
        println("Payment Methods:")
        PaymentMethod.entries.forEachIndexed { i, m -> println("  ${i + 1}. $m") }
        val methodChoice = readChoice("Payment method")
        val method = PaymentMethod.entries.getOrNull(methodChoice - 1) ?: run {
            println("Invalid"); return
        }
        var upiId: String? = null
        var cardNumber: String? = null
        if (method == PaymentMethod.UPI) {
            print("UPI ID: "); upiId = readLine()?.trim()
        }
        if (method == PaymentMethod.CREDIT_CARD) {
            print("Card Number: "); cardNumber = readLine()?.trim()
        }
        val order = orderService.placeOrder(customer, method, coupon, upiId, cardNumber, dataDir)
        println("✅ Order placed! ID: ${order.id}")
        println("   Total: ₹${order.grandTotal}")
        println("   Invoice: ${order.invoiceId}")
        order.shipment?.let { println("   Tracking: ${it.trackingId}") }
    }

    private fun orderHistory(customerId: String) {
        val orders = orderService.getOrderHistory(customerId)
        if (orders.isEmpty()) println("No orders yet")
        else orders.forEach { o ->
            println("${o.id} | ₹${o.grandTotal} | ${o.status} | ${o.createdAt.toLocalDate()}")
        }
    }

    private fun trackOrderFlow() {
        print("Order ID: "); val id = readLine()?.trim() ?: return
        println(orderService.trackOrder(id))
    }

    private fun cancelOrderFlow(customerId: String) {
        print("Order ID: "); val id = readLine()?.trim() ?: return
        val order = orderService.cancelOrder(id, customerId)
        println("✅ Order ${order.id} cancelled")
    }

    private fun returnOrderFlow(customerId: String) {
        print("Order ID: "); val id = readLine()?.trim() ?: return
        val order = orderService.returnOrder(id, customerId)
        println("✅ Return initiated for ${order.id}. 90% refund credited to wallet.")
    }

    private fun walletMenu(customerId: String) {
        println("1. View Balance & History  2. Recharge")
        when (readChoice("Choice")) {
            1 -> println(paymentService.getWalletHistory(customerId))
            2 -> {
                print("Amount: "); val amt = readLine()?.trim()?.toDoubleOrNull() ?: return
                loginService.rechargeWallet(customerId, amt)
                println("✅ Wallet recharged")
            }
        }
    }

    private fun loyaltyFlow(customerId: String) {
        val customer = loginService.getCurrentCustomer() ?: return
        println("Loyalty Points: ${customer.loyaltyPoints}")
        println("100 points = ₹100 cashback")
        if (readChoice("Redeem? (1=Yes, 0=No)") == 1) {
            val cashback = loginService.redeemLoyaltyPoints(customerId)
            println("✅ Redeemed! ₹$cashback added to wallet")
        }
    }

    private fun addressBookFlow(customerId: String) {
        val customer = UserRepository.instance.findCustomerById(customerId) ?: return
        println("1. Add Address  2. View Addresses")
        when (readChoice("Choice")) {
            1 -> {
                print("Label (Home/Work): "); val label = readLine()?.trim() ?: return
                print("Street: "); val street = readLine()?.trim() ?: return
                print("City: "); val city = readLine()?.trim() ?: return
                print("State: "); val state = readLine()?.trim() ?: return
                print("Pincode: "); val pin = readLine()?.trim() ?: return
                val addr = loginService.addAddress(customerId, label, street, city, state, pin, customer.addresses.isEmpty())
                println("✅ Address added: $addr")
            }
            2 -> {
                if (customer.addresses.isEmpty()) println("No addresses")
                else customer.addresses.forEach { println(it) }
            }
        }
    }

    private fun reviewFlow(customer: com.kododake.ecommerce.models.Customer) {
        print("Product ID: "); val pid = readLine()?.trim() ?: return
        print("Rating (1-5): "); val rating = readLine()?.trim()?.toIntOrNull() ?: return
        print("Comment: "); val comment = readLine()?.trim() ?: return
        reviewService.addReview(pid, customer.id, customer.name, rating, comment)
        println("✅ Review submitted!")
    }

    private fun changePasswordFlow() {
        print("Current Password: "); val old = readLine()?.trim() ?: return
        print("New Password: "); val new = readLine()?.trim() ?: return
        loginService.changePassword(old, new)
        println("✅ Password changed")
    }

    private fun notificationsFlow(userId: String) {
        InventoryService.sharedNotifications.getNotifications(userId).forEach { n ->
            val read = if (n.isRead) "✓" else "•"
            println("$read [${n.type}] ${n.message}")
        }
    }

    private fun adminAddProduct() {
        print("Name: "); val name = readLine()?.trim() ?: return
        print("Description: "); val desc = readLine()?.trim() ?: return
        print("Price: "); val price = readLine()?.trim()?.toDoubleOrNull() ?: return
        print("Category: "); val cat = readLine()?.trim() ?: return
        print("Brand: "); val brand = readLine()?.trim() ?: return
        print("Stock: "); val stock = readLine()?.trim()?.toIntOrNull() ?: return
        print("Weight (kg): "); val weight = readLine()?.trim()?.toDoubleOrNull() ?: return
        val p = productService.addProduct(name, desc, price, cat, brand, stock, weight)
        println("✅ Product added: ${p.id}")
    }

    private fun adminUpdateProduct() {
        print("Product ID: "); val id = readLine()?.trim() ?: return
        print("New Price (or Enter to skip): "); val price = readLine()?.trim()?.toDoubleOrNull()
        print("New Stock (or Enter to skip): "); val stock = readLine()?.trim()?.toIntOrNull()
        val updates = mutableMapOf<String, Any>()
        price?.let { updates["price"] = it }
        stock?.let { updates["stock"] = it }
        if (updates.isEmpty()) { println("Nothing to update"); return }
        productService.updateProduct(id, updates)
        println("✅ Product updated")
    }

    private fun adminDeleteProduct() {
        print("Product ID: "); val id = readLine()?.trim() ?: return
        productService.deleteProduct(id)
        println("✅ Product deactivated")
    }

    private fun adminAddStock() {
        print("Product ID: "); val id = readLine()?.trim() ?: return
        print("Quantity to add: "); val qty = readLine()?.trim()?.toIntOrNull() ?: return
        inventoryService.addStock(id, qty)
        println("✅ Stock updated")
    }

    private fun adminCoupons() {
        println("Active Coupons:")
        couponService.listActiveCoupons().forEach { println("  ${it.code}: ${it.description} (expires ${it.expiryDate})") }
    }

    private fun adminProcessOrders() {
        val pending = OrderRepository.instance.findByStatus(com.kododake.ecommerce.models.OrderStatus.CONFIRMED)
        if (pending.isEmpty()) { println("No orders to process"); return }
        pending.take(3).forEach { order ->
            orderService.advanceShipment(order.id)
            println("Advanced shipment for ${order.id}")
        }
    }

    private fun viewCustomers() {
        UserRepository.instance.getAllCustomers().forEach { c ->
            println("${c.id} | ${c.name} | ${c.email} | Spent: ₹${c.totalSpent}")
        }
    }

    private fun saveData() {
        UserRepository.instance.saveToFile(dataDir)
        ProductRepository.instance.saveToFile(dataDir)
        OrderRepository.instance.saveToFile(dataDir)
        println("💾 Data saved to $dataDir")
    }

    private fun readChoice(prompt: String): Int =
        print("$prompt: ").let { readLine()?.trim()?.toIntOrNull() ?: -1 }
}
