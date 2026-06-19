package com.kododake.ecommerce.utils

import com.kododake.ecommerce.models.Product
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Double.toCurrency(): String = "${Constants.CURRENCY_SYMBOL}${"%.2f".format(this)}"

fun List<Product>.filterByCategory(category: String): List<Product> =
    filter { it.category.equals(category, ignoreCase = true) }

fun List<Product>.filterByPriceRange(min: Double, max: Double): List<Product> =
    filter { it.price in min..max }

fun List<Product>.filterAvailable(): List<Product> =
    filter { it.stock > 0 }

fun String.toLocalDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(this, DATE_FORMAT)
}.getOrNull()

fun <T> T?.orThrow(message: String): T = this ?: throw NoSuchElementException(message)

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> T): T =
    if (condition) block() else this
