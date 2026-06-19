package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.NotFoundException
import com.kododake.ecommerce.exceptions.ValidationException
import com.kododake.ecommerce.models.Product
import com.kododake.ecommerce.repository.ProductRepository
import com.kododake.ecommerce.utils.Algorithms
import com.kododake.ecommerce.utils.RandomGenerator
import com.kododake.ecommerce.utils.Validator

class ProductService(
    private val productRepo: ProductRepository = ProductRepository.instance
) {
    fun addProduct(
        name: String, description: String, price: Double, category: String,
        brand: String, stock: Int, weightKg: Double
    ): Product {
        val product = Product(
            id = RandomGenerator.productId(),
            name = Validator.requireNonBlank(name, "Name"),
            description = description,
            price = Validator.validatePositive(price, "Price"),
            category = Validator.requireNonBlank(category, "Category"),
            brand = Validator.requireNonBlank(brand, "Brand"),
            stock = stock.coerceAtLeast(0),
            weightKg = Validator.validatePositive(weightKg, "Weight")
        )
        productRepo.save(product)
        return product
    }

    fun updateProduct(id: String, updates: Map<String, Any>): Product {
        val product = productRepo.findById(id) ?: throw NotFoundException("Product not found: $id")
        updates["name"]?.let { product.name = it as String }
        updates["description"]?.let { product.description = it as String }
        updates["price"]?.let { product.price = Validator.validatePositive(it as Double, "Price") }
        updates["category"]?.let { product.category = it as String }
        updates["brand"]?.let { product.brand = it as String }
        updates["stock"]?.let { product.stock = (it as Int).coerceAtLeast(0) }
        updates["weightKg"]?.let { product.weightKg = Validator.validatePositive(it as Double, "Weight") }
        return product
    }

    fun deleteProduct(id: String): Boolean {
        val product = productRepo.findById(id) ?: throw NotFoundException("Product not found")
        product.isActive = false
        return true
    }

    fun getProduct(id: String): Product =
        productRepo.findById(id) ?: throw NotFoundException("Product not found: $id")

    fun getAllProducts(): List<Product> = productRepo.findAll()

    fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return productRepo.findAll()
        val binaryResult = Algorithms.binarySearchByName(productRepo.findAll(), query)
        val containsResults = productRepo.searchByName(query)
        return if (binaryResult != null) {
            (listOf(binaryResult) + containsResults.filter { it.id != binaryResult.id }).distinctBy { it.id }
        } else containsResults
    }

    fun sortByPrice(ascending: Boolean = true): List<Product> =
        Algorithms.mergeSortByPrice(productRepo.findAll(), ascending)

    fun filterProducts(
        category: String? = null,
        brand: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minRating: Double? = null,
        availableOnly: Boolean = false
    ): List<Product> {
        var results = productRepo.findAll()
        category?.let { cat -> results = results.filter { it.category.equals(cat, ignoreCase = true) } }
        brand?.let { b -> results = results.filter { it.brand.equals(b, ignoreCase = true) } }
        if (minPrice != null && maxPrice != null) {
            results = results.filter { it.price in minPrice..maxPrice }
        }
        minRating?.let { results = results.filter { it.rating >= it } }
        if (availableOnly) results = results.filter { it.availableStock > 0 }
        return results
    }

    fun getCategories(): Set<String> = productRepo.getCategories()

    fun displayCategoryTree(): String {
        val categories = productRepo.getCategories()
        val tree = categories.groupBy { it.split(" ").first() }
        return buildString {
            appendLine("Category Tree (DFS):")
            tree.keys.forEach { root ->
                Algorithms.categoryTreeDfs(
                    tree.mapValues { (_, v) -> v.map { it } },
                    root
                ) { cat, depth ->
                    appendLine("${"  ".repeat(depth)}└─ $cat")
                }
            }
        }
    }
}
