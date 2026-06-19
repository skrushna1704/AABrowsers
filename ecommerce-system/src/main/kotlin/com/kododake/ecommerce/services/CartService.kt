package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.NotFoundException
import com.kododake.ecommerce.exceptions.ValidationException
import com.kododake.ecommerce.models.Cart
import com.kododake.ecommerce.models.CartItem
import com.kododake.ecommerce.models.Customer
import com.kododake.ecommerce.repository.ProductRepository
import com.kododake.ecommerce.utils.Constants
import com.kododake.ecommerce.utils.Validator
import java.util.concurrent.ConcurrentHashMap

class CartService(
    private val productRepo: ProductRepository = ProductRepository.instance,
    private val inventoryService: InventoryService = InventoryService()
) {
    private val carts = ConcurrentHashMap<String, Cart>()

    fun getOrCreateCart(customerId: String): Cart =
        carts.getOrPut(customerId) { Cart(customerId) }

    fun addToCart(customerId: String, productId: String, quantity: Int): Cart {
        val qty = Validator.validateQuantity(quantity)
        val product = productRepo.findById(productId) ?: throw NotFoundException("Product not found")
        if (product.availableStock < qty) {
            throw ValidationException("Only ${product.availableStock} units available")
        }
        val cart = getOrCreateCart(customerId)
        cart.pushUndoState()
        val existing = cart.items.find { it.productId == productId && !it.savedForLater }
        if (existing != null) {
            existing.quantity += qty
        } else {
            cart.items.add(CartItem(productId, qty))
        }
        return cart
    }

    fun removeFromCart(customerId: String, productId: String): Cart {
        val cart = getOrCreateCart(customerId)
        cart.pushUndoState()
        cart.items.removeAll { it.productId == productId && !it.savedForLater }
        return cart
    }

    fun updateQuantity(customerId: String, productId: String, quantity: Int): Cart {
        val qty = Validator.validateQuantity(quantity)
        val product = productRepo.findById(productId) ?: throw NotFoundException("Product not found")
        if (product.availableStock < qty) throw ValidationException("Insufficient stock")
        val cart = getOrCreateCart(customerId)
        cart.pushUndoState()
        cart.items.find { it.productId == productId && !it.savedForLater }?.quantity = qty
        return cart
    }

    fun saveForLater(customerId: String, productId: String): Cart {
        val cart = getOrCreateCart(customerId)
        cart.items.find { it.productId == productId }?.savedForLater = true
        return cart
    }

    fun moveToCart(customerId: String, productId: String): Cart {
        val cart = getOrCreateCart(customerId)
        cart.items.find { it.productId == productId }?.savedForLater = false
        return cart
    }

    fun undo(customerId: String): Boolean = getOrCreateCart(customerId).undo()

    fun clearCart(customerId: String) {
        getOrCreateCart(customerId).clear()
    }

    fun addToWishlist(customer: Customer, productId: String) {
        productRepo.findById(productId) ?: throw NotFoundException("Product not found")
        customer.wishlist.add(productId)
    }

    fun removeFromWishlist(customer: Customer, productId: String) {
        customer.wishlist.remove(productId)
    }

    fun calculateSubtotal(cart: Cart): Double =
        cart.activeItems().sumOf { item ->
            val product = productRepo.findById(item.productId)
            (product?.price ?: 0.0) * item.quantity
        }

    fun calculateTotalWeight(cart: Cart): Double =
        cart.activeItems().sumOf { item ->
            val product = productRepo.findById(item.productId)
            (product?.weightKg ?: 0.0) * item.quantity
        }

    fun calculateShipping(cart: Cart): Double =
        Constants.shippingCharge(calculateTotalWeight(cart))

    fun displayCart(customerId: String): String {
        val cart = getOrCreateCart(customerId)
        return buildString {
            appendLine("=== YOUR CART ===")
            if (cart.activeItems().isEmpty()) {
                appendLine("Cart is empty")
            } else {
                cart.activeItems().forEach { item ->
                    val product = productRepo.findById(item.productId)
                    appendLine("${product?.name ?: item.productId} x${item.quantity} = ₹${(product?.price ?: 0.0) * item.quantity}")
                }
                appendLine("-".repeat(30))
                appendLine("Subtotal: ₹${calculateSubtotal(cart)}")
                appendLine("Shipping: ₹${calculateShipping(cart)}")
            }
            if (cart.items.any { it.savedForLater }) {
                appendLine("\nSaved for Later:")
                cart.items.filter { it.savedForLater }.forEach { item ->
                    val product = productRepo.findById(item.productId)
                    appendLine("  ${product?.name ?: item.productId}")
                }
            }
        }
    }

    fun validateStock(cart: Cart): List<String> {
        val errors = mutableListOf<String>()
        cart.activeItems().forEach { item ->
            val product = productRepo.findById(item.productId)
            if (product == null) errors.add("Product ${item.productId} not found")
            else if (product.availableStock < item.quantity) {
                errors.add("${product.name}: requested ${item.quantity}, available ${product.availableStock}")
            }
        }
        return errors
    }
}
