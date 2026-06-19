package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.NotFoundException
import com.kododake.ecommerce.exceptions.OrderException
import com.kododake.ecommerce.exceptions.ValidationException
import com.kododake.ecommerce.models.*
import com.kododake.ecommerce.patterns.NotificationSubject
import com.kododake.ecommerce.repository.OrderRepository
import com.kododake.ecommerce.repository.ProductRepository
import com.kododake.ecommerce.repository.UserRepository
import com.kododake.ecommerce.utils.Constants
import com.kododake.ecommerce.utils.InvoiceBuilder
import com.kododake.ecommerce.utils.InvoiceGenerator
import com.kododake.ecommerce.utils.RandomGenerator
import java.time.LocalDate

class OrderService(
    private val orderRepo: OrderRepository = OrderRepository.instance,
    private val productRepo: ProductRepository = ProductRepository.instance,
    private val userRepo: UserRepository = UserRepository.instance,
    private val cartService: CartService = CartService(),
    private val couponService: CouponService = CouponService(),
    private val paymentService: PaymentService = PaymentService(),
    private val inventoryService: InventoryService = InventoryService(),
    private val notificationSubject: NotificationSubject = InventoryService.sharedNotifications
) {
    fun placeOrder(
        customer: Customer,
        paymentMethod: PaymentMethod,
        couponCode: String? = null,
        upiId: String? = null,
        cardNumber: String? = null,
        dataDir: String = Constants.DATA_DIR
    ): Order {
        val cart = cartService.getOrCreateCart(customer.id)
        val stockErrors = cartService.validateStock(cart)
        if (stockErrors.isNotEmpty()) throw ValidationException(stockErrors.joinToString("; "))

        val address = customer.addresses.find { it.isDefault }
            ?: customer.addresses.firstOrNull()
            ?: throw OrderException("No shipping address found. Please add an address first.")

        val subtotal = cartService.calculateSubtotal(cart)
        if (subtotal <= 0) throw OrderException("Cart is empty")

        var couponDiscount = 0.0
        var appliedCoupon: Coupon? = null
        if (couponCode != null) {
            appliedCoupon = couponService.validateCoupon(couponCode, customer.id, subtotal)
            couponDiscount = couponService.applyCoupon(
                appliedCoupon, subtotal, cart.activeItems().sumOf { it.quantity }
            )
        }

        val discountResult = couponService.calculateDynamicDiscount(customer, subtotal, couponDiscount)
        val discountedSubtotal = discountResult.finalAmount

        val orderItems = cart.activeItems().mapNotNull { item ->
            val product = productRepo.findById(item.productId) ?: return@mapNotNull null
            inventoryService.reserveStock(product.id, item.quantity)
            val gstRate = Constants.GST_RATES[product.category] ?: Constants.GST_RATES["General"]!!
            OrderItem(product.id, product.name, item.quantity, product.price, gstRate)
        }

        val gstTotal = orderItems.sumOf { it.gstAmount }
        val shipping = cartService.calculateShipping(cart)
        val grandTotal = discountedSubtotal + gstTotal + shipping

        paymentService.validatePaymentMethod(paymentMethod, grandTotal, customer.id)

        val orderId = RandomGenerator.orderId()
        val paymentResult = paymentService.processPayment(PaymentRequest(
            orderId = orderId, amount = grandTotal, method = paymentMethod,
            customerId = customer.id, upiId = upiId, cardNumber = cardNumber
        ))

        val paymentMessage = when (paymentResult) {
            is PaymentResult.Failure -> {
                orderItems.forEach { inventoryService.releaseStock(it.productId, it.quantity) }
                throw OrderException("Payment failed: ${paymentResult.reason}")
            }
            is PaymentResult.Success -> {
                orderItems.forEach { inventoryService.deductStock(it.productId, it.quantity) }
                paymentResult.message
            }
        }

        appliedCoupon?.let { couponService.markCouponUsed(it.code, customer.id) }

        val order = Order(
            id = orderId,
            customerId = customer.id,
            items = orderItems,
            subtotal = subtotal,
            discount = discountResult.discountAmount,
            gstTotal = gstTotal,
            shippingCharge = shipping,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            status = OrderStatus.CONFIRMED,
            shippingAddress = address,
            couponCode = couponCode,
            shipment = Shipment(
                trackingId = "TRK${RandomGenerator.orderId()}",
                status = ShipmentStatus.PROCESSING,
                estimatedDelivery = LocalDate.now().plusDays(5)
            ).also { it.addUpdate("Order confirmed and processing") }
        )

        val invoiceNumber = RandomGenerator.invoiceNumber()
        order.invoiceId = invoiceNumber

        val gstBreakdown = orderItems.groupBy { item ->
            productRepo.findById(item.productId)?.category ?: "General"
        }.mapValues { (_, items) -> items.sumOf { it.gstAmount } }

        val invoice = InvoiceBuilder()
            .invoiceNumber(invoiceNumber)
            .orderId(orderId)
            .customerName(customer.name)
            .customerEmail(customer.email)
            .order(order)
            .gstBreakdown(gstBreakdown)
            .build()

        InvoiceGenerator.saveToFile(invoice, "$dataDir/invoices")

        orderRepo.save(order)
        cartService.clearCart(customer.id)

        customer.totalSpent += grandTotal
        customer.addLoyaltyPoints(grandTotal)

        notificationSubject.notify(customer.id, NotificationType.ORDER_PLACED, "Order $orderId placed successfully!")
        notificationSubject.notify(customer.id, NotificationType.PAYMENT_SUCCESS, paymentMessage)

        return order
    }

    fun cancelOrder(orderId: String, customerId: String): Order {
        val order = orderRepo.findById(orderId) ?: throw NotFoundException("Order not found")
        if (order.customerId != customerId) throw OrderException("Unauthorized")
        if (order.status !in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
            throw OrderException("Order cannot be cancelled in ${order.status} status")
        }
        order.status = OrderStatus.CANCELLED
        order.items.forEach { item ->
            productRepo.findById(item.productId)?.let { product ->
                product.stock += item.quantity
                product.totalSold -= item.quantity.coerceAtMost(product.totalSold)
            }
        }
        if (order.paymentMethod != PaymentMethod.CASH_ON_DELIVERY) {
            paymentService.refundToWallet(customerId, order.grandTotal, orderId)
        }
        notificationSubject.notify(customerId, NotificationType.ORDER_PLACED, "Order $orderId cancelled. Refund initiated.")
        return order
    }

    fun returnOrder(orderId: String, customerId: String): Order {
        val order = orderRepo.findById(orderId) ?: throw NotFoundException("Order not found")
        if (order.customerId != customerId) throw OrderException("Unauthorized")
        if (order.status != OrderStatus.DELIVERED) throw OrderException("Only delivered orders can be returned")
        order.status = OrderStatus.RETURNED
        order.items.forEach { item ->
            inventoryService.addStock(item.productId, item.quantity)
        }
        paymentService.refundToWallet(customerId, order.grandTotal * 0.9, orderId)
        return order
    }

    fun trackOrder(orderId: String): String {
        val order = orderRepo.findById(orderId) ?: throw NotFoundException("Order not found")
        return buildString {
            appendLine("Order: ${order.id} | Status: ${order.status}")
            order.shipment?.let { shipment ->
                appendLine("Tracking: ${shipment.trackingId}")
                appendLine("Shipment Status: ${shipment.status}")
                appendLine("Est. Delivery: ${shipment.estimatedDelivery}")
                shipment.updates.forEach { (time, msg) ->
                    appendLine("  [$time] $msg")
                }
            }
        }
    }

    fun getOrderHistory(customerId: String): List<Order> =
        orderRepo.findByCustomer(customerId)

    fun advanceShipment(orderId: String) {
        val order = orderRepo.findById(orderId) ?: return
        order.shipment?.let { shipment ->
            shipment.status = when (shipment.status) {
                ShipmentStatus.PROCESSING -> ShipmentStatus.IN_TRANSIT
                ShipmentStatus.IN_TRANSIT -> ShipmentStatus.OUT_FOR_DELIVERY
                ShipmentStatus.OUT_FOR_DELIVERY -> ShipmentStatus.DELIVERED
                else -> shipment.status
            }
            shipment.addUpdate("Status updated to ${shipment.status}")
            if (shipment.status == ShipmentStatus.DELIVERED) {
                order.status = OrderStatus.DELIVERED
                notificationSubject.notify(order.customerId, NotificationType.ORDER_DELIVERED, "Order ${order.id} delivered!")
            }
        }
    }
}
