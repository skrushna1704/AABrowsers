package com.kododake.ecommerce.models

data class Address(
    val id: String,
    var label: String,
    var street: String,
    var city: String,
    var state: String,
    var pincode: String,
    var isDefault: Boolean = false
) {
    override fun toString(): String =
        "$label: $street, $city, $state - $pincode${if (isDefault) " [Default]" else ""}"
}
