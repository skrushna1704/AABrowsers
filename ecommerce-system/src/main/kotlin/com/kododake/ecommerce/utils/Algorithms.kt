package com.kododake.ecommerce.utils

import com.kododake.ecommerce.models.Product

object Algorithms {
    fun binarySearchByName(products: List<Product>, target: String): Product? {
        if (products.isEmpty()) return null
        val sorted = products.sortedBy { it.name.lowercase() }
        var low = 0
        var high = sorted.lastIndex
        val key = target.lowercase()
        while (low <= high) {
            val mid = (low + high) / 2
            val cmp = sorted[mid].name.lowercase().compareTo(key)
            when {
                cmp == 0 -> return sorted[mid]
                cmp < 0 -> low = mid + 1
                else -> high = mid - 1
            }
        }
        return null
    }

    fun mergeSortByPrice(products: List<Product>, ascending: Boolean = true): List<Product> {
        if (products.size <= 1) return products
        val mid = products.size / 2
        val left = mergeSortByPrice(products.subList(0, mid), ascending)
        val right = mergeSortByPrice(products.subList(mid, products.size), ascending)
        return merge(left, right, ascending)
    }

    private fun merge(left: List<Product>, right: List<Product>, ascending: Boolean): List<Product> {
        val result = mutableListOf<Product>()
        var i = 0
        var j = 0
        while (i < left.size && j < right.size) {
            val takeLeft = if (ascending) left[i].price <= right[j].price else left[i].price >= right[j].price
            if (takeLeft) result.add(left[i++]) else result.add(right[j++])
        }
        result.addAll(left.subList(i, left.size))
        result.addAll(right.subList(j, right.size))
        return result
    }

    fun quickSortByRevenue(items: List<Pair<String, Double>>): List<Pair<String, Double>> {
        if (items.size <= 1) return items
        val pivot = items[items.size / 2].second
        val less = items.filter { it.second < pivot }
        val equal = items.filter { it.second == pivot }
        val greater = items.filter { it.second > pivot }
        return quickSortByRevenue(greater) + equal + quickSortByRevenue(less)
    }

    fun categoryTreeDfs(
        categories: Map<String, List<String>>,
        root: String,
        visitor: (String, Int) -> Unit,
        depth: Int = 0
    ) {
        visitor(root, depth)
        categories[root]?.forEach { child ->
            categoryTreeDfs(categories, child, visitor, depth + 1)
        }
    }
}
