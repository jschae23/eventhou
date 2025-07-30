package com.example.eventhou.data.model

import java.io.Serializable

/**
 * The event category.
 */
class Category : Serializable {
    var categoryName: String? = null
    var subCategoriesName: String? = null
    var subCategories: List<String>? = null

    override fun toString(): String {
        return categoryName + ": " + subCategoriesToString()
    }

    /**
     * Join all subcategories to a readable string and limit its length.
     */
    fun subCategoriesToString(limit: Int? = null): String {
        subCategories?.let {
            val join = it.joinToString()
            if (limit != null && join.length > limit) {
                return join.substring(0, limit) + "..."
            }
            return join
        }

        return "no $subCategoriesName listed"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Category
        if (categoryName == other.categoryName) return true
        return false
    }

    override fun hashCode(): Int {
        return categoryName?.hashCode() ?: 0
    }
}
