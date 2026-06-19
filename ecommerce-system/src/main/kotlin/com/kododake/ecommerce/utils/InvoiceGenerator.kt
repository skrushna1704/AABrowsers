package com.kododake.ecommerce.utils

import com.kododake.ecommerce.models.Invoice
import com.kododake.ecommerce.models.Order

class InvoiceBuilder {
    private var invoiceNumber: String = ""
    private var orderId: String = ""
    private var customerName: String = ""
    private var customerEmail: String = ""
    private var order: Order? = null
    private var gstBreakdown: Map<String, Double> = emptyMap()

    fun invoiceNumber(value: String) = apply { invoiceNumber = value }
    fun orderId(value: String) = apply { orderId = value }
    fun customerName(value: String) = apply { customerName = value }
    fun customerEmail(value: String) = apply { customerEmail = value }
    fun order(value: Order) = apply { order = value }
    fun gstBreakdown(value: Map<String, Double>) = apply { gstBreakdown = value }

    fun build(): Invoice {
        val o = order ?: throw IllegalStateException("Order is required")
        return Invoice(
            invoiceNumber = invoiceNumber,
            orderId = orderId,
            customerName = customerName,
            customerEmail = customerEmail,
            items = o.items,
            subtotal = o.subtotal,
            discount = o.discount,
            gstBreakdown = gstBreakdown,
            gstTotal = o.gstTotal,
            shippingCharge = o.shippingCharge,
            grandTotal = o.grandTotal,
            paymentMethod = o.paymentMethod
        )
    }
}

object InvoiceGenerator {
    fun generate(invoice: Invoice): String = buildString {
        appendLine("=".repeat(50))
        appendLine("           E-COMMERCE INVOICE")
        appendLine("=".repeat(50))
        appendLine("Invoice No : ${invoice.invoiceNumber}")
        appendLine("Order ID   : ${invoice.orderId}")
        appendLine("Date       : ${invoice.generatedAt}")
        appendLine("-".repeat(50))
        appendLine("Customer   : ${invoice.customerName}")
        appendLine("Email      : ${invoice.customerEmail}")
        appendLine("-".repeat(50))
        appendLine(String.format("%-25s %5s %10s", "Item", "Qty", "Amount"))
        appendLine("-".repeat(50))
        invoice.items.forEach { item ->
            appendLine(String.format("%-25s %5d %10.2f", item.productName.take(25), item.quantity, item.total))
        }
        appendLine("-".repeat(50))
        appendLine(String.format("%40s %10.2f", "Subtotal:", invoice.subtotal))
        appendLine(String.format("%40s %10.2f", "Discount:", -invoice.discount))
        invoice.gstBreakdown.forEach { (cat, amt) ->
            appendLine(String.format("%40s %10.2f", "GST ($cat):", amt))
        }
        appendLine(String.format("%40s %10.2f", "Shipping:", invoice.shippingCharge))
        appendLine("=".repeat(50))
        appendLine(String.format("%40s %10.2f", "GRAND TOTAL:", invoice.grandTotal))
        appendLine("Payment    : ${invoice.paymentMethod}")
        appendLine("=".repeat(50))
        appendLine("Thank you for shopping with us!")
    }

    fun saveToFile(invoice: Invoice, directory: String): String {
        val dir = java.io.File(directory)
        if (!dir.exists()) dir.mkdirs()
        val file = java.io.File(dir, "${invoice.invoiceNumber}.txt")
        file.writeText(generate(invoice))
        return file.absolutePath
    }
}
