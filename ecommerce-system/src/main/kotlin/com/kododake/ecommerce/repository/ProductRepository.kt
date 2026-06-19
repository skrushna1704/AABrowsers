package com.kododake.ecommerce.repository

import com.kododake.ecommerce.models.Product
import com.kododake.ecommerce.utils.Constants
import java.io.File
import java.util.PriorityQueue

class ProductRepository private constructor() {
    private val products = HashMap<String, Product>()

    companion object {
        val instance: ProductRepository by lazy { ProductRepository() }
    }

    init {
        seedSampleProducts()
    }

    private fun seedSampleProducts() {
        listOf(
            Product("PRD10001", "iPhone 15 Pro", "Latest Apple smartphone", 89999.0, "Electronics", "Apple", 50, 0.2, 4.5, 120),
            Product("PRD10002", "Samsung Galaxy S24", "Flagship Android phone", 74999.0, "Electronics", "Samsung", 40, 0.2, 4.3, 85),
            Product("PRD10003", "MacBook Air M3", "Lightweight laptop", 114999.0, "Electronics", "Apple", 25, 1.2, 4.7, 60),
            Product("PRD10004", "Nike Air Max", "Running shoes", 8999.0, "Fashion", "Nike", 100, 0.8, 4.2, 200),
            Product("PRD10005", "Levi's Jeans", "Classic fit denim", 3499.0, "Fashion", "Levi's", 80, 0.5, 4.0, 150),
            Product("PRD10006", "Organic Honey", "Pure forest honey 500g", 599.0, "Food", "Nature's Best", 200, 0.5, 4.6, 90),
            Product("PRD10007", "Whole Wheat Bread", "Fresh baked bread", 45.0, "Food", "Baker's Own", 3, 0.4, 4.1, 45),
            Product("PRD10008", "Clean Code", "Software craftsmanship book", 499.0, "Books", "Prentice Hall", 30, 0.3, 4.8, 300),
            Product("PRD10009", "Kotlin in Action", "Kotlin programming guide", 799.0, "Books", "Manning", 25, 0.4, 4.7, 180),
            Product("PRD10010", "Paracetamol 500mg", "Pain relief tablets", 35.0, "Medicine", "Cipla", 500, 0.05, 4.0, 50)
        ).forEach { save(it) }
    }

    fun save(product: Product) {
        products[product.id] = product
    }

    fun findById(id: String): Product? = products[id]

    fun findAll(): List<Product> = products.values.filter { it.isActive }

    fun findAllIncludingInactive(): List<Product> = products.values.toList()

    fun delete(id: String): Boolean = products.remove(id) != null

    fun searchByName(query: String): List<Product> {
        val q = query.lowercase()
        return products.values.filter {
            it.isActive && (it.name.lowercase().contains(q) || it.brand.lowercase().contains(q))
        }
    }

    fun filterByCategory(category: String): List<Product> =
        products.values.filter { it.isActive && it.category.equals(category, ignoreCase = true) }

    fun filterByBrand(brand: String): List<Product> =
        products.values.filter { it.isActive && it.brand.equals(brand, ignoreCase = true) }

    fun filterByPriceRange(min: Double, max: Double): List<Product> =
        products.values.filter { it.isActive && it.price in min..max }

    fun filterByRating(minRating: Double): List<Product> =
        products.values.filter { it.isActive && it.rating >= minRating }

    fun getLowStockProducts(threshold: Int = Constants.LOW_STOCK_THRESHOLD): List<Product> =
        products.values.filter { it.isActive && it.isLowStock(threshold) }

    fun getTopSelling(limit: Int): List<Product> {
        val pq = PriorityQueue<Product>(limit + 1) { a, b -> a.totalSold - b.totalSold }
        products.values.filter { it.isActive }.forEach { product ->
            pq.offer(product)
            if (pq.size > limit) pq.poll()
        }
        return pq.sortedByDescending { it.totalSold }
    }

    fun getCategories(): Set<String> =
        products.values.map { it.category }.toSet()

    fun loadFromFile(dataDir: String) {
        val file = File(dataDir, Constants.PRODUCTS_FILE)
        if (!file.exists()) return
        file.readLines().forEach { line ->
            if (line.isBlank() || line.startsWith("#")) return@forEach
            val p = line.split("|")
            if (p.size < 9) return@forEach
            save(Product(
                id = p[0], name = p[1], description = p[2],
                price = p[3].toDoubleOrNull() ?: 0.0, category = p[4],
                brand = p[5], stock = p[6].toIntOrNull() ?: 0,
                weightKg = p[7].toDoubleOrNull() ?: 0.5,
                rating = p[8].toDoubleOrNull() ?: 0.0,
                reviewCount = p.getOrElse(9) { "0" }.toIntOrNull() ?: 0,
                totalSold = p.getOrElse(10) { "0" }.toIntOrNull() ?: 0
            ))
        }
    }

    fun saveToFile(dataDir: String) {
        val dir = File(dataDir)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, Constants.PRODUCTS_FILE)
        file.writeText(buildString {
            appendLine("# id|name|desc|price|category|brand|stock|weight|rating|reviews|sold")
            products.values.forEach { p ->
                appendLine("${p.id}|${p.name}|${p.description}|${p.price}|${p.category}|${p.brand}|${p.stock}|${p.weightKg}|${p.rating}|${p.reviewCount}|${p.totalSold}")
            }
        })
    }
}
