package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class RecipeIngredient(val itemId: String, val quantity: Int)

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String,
    val materials: List<RecipeIngredient>,
    val cost: Coins,
    val outputItemId: String,
    val outputQuantity: Int = 1,
    val levelRequirement: Int = 1,
    val classRestriction: List<String>? = null
)
