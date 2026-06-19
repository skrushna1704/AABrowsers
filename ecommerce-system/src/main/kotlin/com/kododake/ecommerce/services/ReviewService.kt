package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.NotFoundException
import com.kododake.ecommerce.exceptions.ValidationException
import com.kododake.ecommerce.models.Review
import com.kododake.ecommerce.repository.ProductRepository
import com.kododake.ecommerce.utils.RandomGenerator
import com.kododake.ecommerce.utils.Validator

class ReviewService(
    private val productRepo: ProductRepository = ProductRepository.instance
) {
    private val reviews = mutableListOf<Review>()

    fun addReview(productId: String, customerId: String, customerName: String, rating: Int, comment: String): Review {
        val product = productRepo.findById(productId) ?: throw NotFoundException("Product not found")
        val validRating = Validator.validateRating(rating)
        if (comment.isBlank()) throw ValidationException("Review comment cannot be blank")

        val review = Review(
            id = "REV${RandomGenerator.productId()}",
            productId = productId,
            customerId = customerId,
            customerName = customerName,
            rating = validRating,
            comment = comment
        )
        reviews.add(review)

        val productReviews = reviews.filter { it.productId == productId }
        product.rating = productReviews.map { it.rating }.average()
        product.reviewCount = productReviews.size

        return review
    }

    fun getProductReviews(productId: String): List<Review> =
        reviews.filter { it.productId == productId }.sortedByDescending { it.createdAt }

    fun getAverageRating(productId: String): Double =
        getProductReviews(productId).map { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0
}
