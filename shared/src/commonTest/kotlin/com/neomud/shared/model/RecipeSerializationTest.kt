package com.neomud.shared.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipeSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testRecipeRoundTrip() {
        val recipe = Recipe(
            id = "recipe:test",
            name = "Test Recipe",
            description = "A test recipe",
            category = "consumable",
            materials = listOf(
                RecipeIngredient("item:spider_silk", 2),
                RecipeIngredient("item:wolf_pelt", 1)
            ),
            cost = Coins(silver = 1, copper = 50),
            outputItemId = "item:antivenom_vial",
            outputQuantity = 3,
            levelRequirement = 2
        )

        val encoded = json.encodeToString(Recipe.serializer(), recipe)
        val decoded = json.decodeFromString(Recipe.serializer(), encoded)

        assertEquals(recipe.id, decoded.id)
        assertEquals(recipe.name, decoded.name)
        assertEquals(recipe.category, decoded.category)
        assertEquals(recipe.materials.size, decoded.materials.size)
        assertEquals(recipe.materials[0].itemId, decoded.materials[0].itemId)
        assertEquals(recipe.materials[0].quantity, decoded.materials[0].quantity)
        assertEquals(recipe.cost.silver, decoded.cost.silver)
        assertEquals(recipe.cost.copper, decoded.cost.copper)
        assertEquals(recipe.outputItemId, decoded.outputItemId)
        assertEquals(recipe.outputQuantity, decoded.outputQuantity)
        assertEquals(recipe.levelRequirement, decoded.levelRequirement)
        assertNull(decoded.classRestriction)
    }

    @Test
    fun testRecipeWithClassRestriction() {
        val recipe = Recipe(
            id = "recipe:restricted",
            name = "Restricted",
            category = "weapon",
            materials = listOf(RecipeIngredient("item:test", 1)),
            cost = Coins(copper = 10),
            outputItemId = "item:test_output",
            classRestriction = listOf("MAGE", "PRIEST")
        )

        val encoded = json.encodeToString(Recipe.serializer(), recipe)
        val decoded = json.decodeFromString(Recipe.serializer(), encoded)

        assertEquals(listOf("MAGE", "PRIEST"), decoded.classRestriction)
    }

    @Test
    fun testRecipeInfoRoundTrip() {
        val info = RecipeInfo(
            recipe = Recipe(
                id = "recipe:test",
                name = "Test",
                category = "consumable",
                materials = listOf(RecipeIngredient("item:silk", 2)),
                cost = Coins(silver = 1),
                outputItemId = "item:output"
            ),
            canCraft = true,
            materialStatus = listOf(
                MaterialStatus("item:silk", "Spider Silk", 2, 5)
            )
        )

        val encoded = json.encodeToString(RecipeInfo.serializer(), info)
        val decoded = json.decodeFromString(RecipeInfo.serializer(), encoded)

        assertEquals(info.canCraft, decoded.canCraft)
        assertEquals(info.materialStatus.size, decoded.materialStatus.size)
        assertEquals(info.materialStatus[0].available, decoded.materialStatus[0].available)
    }
}
