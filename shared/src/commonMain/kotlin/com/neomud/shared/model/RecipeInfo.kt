package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class MaterialStatus(
    val itemId: String,
    val itemName: String,
    val required: Int,
    val available: Int
)

@Serializable
data class RecipeInfo(
    val recipe: Recipe,
    val canCraft: Boolean,
    val materialStatus: List<MaterialStatus>
)
