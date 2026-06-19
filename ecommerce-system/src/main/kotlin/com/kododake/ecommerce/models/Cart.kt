package com.kododake.ecommerce.models

data class CartItem(
    val productId: String,
    var quantity: Int,
    var savedForLater: Boolean = false
)

data class Cart(
    val customerId: String,
    val items: MutableList<CartItem> = mutableListOf(),
    val savedItems: MutableList<CartItem> = mutableListOf(),
    private val undoStack: ArrayDeque<List<CartItem>> = ArrayDeque()
) {
    fun pushUndoState() {
        undoStack.addLast(items.map { it.copy() })
        if (undoStack.size > 10) undoStack.removeFirst()
    }

    fun undo(): Boolean {
        val previous = undoStack.removeLastOrNull() ?: return false
        items.clear()
        items.addAll(previous.map { it.copy() })
        return true
    }

    fun activeItems(): List<CartItem> = items.filter { !it.savedForLater }

    fun totalItems(): Int = activeItems().sumOf { it.quantity }

    fun clear() {
        pushUndoState()
        items.clear()
    }
}
