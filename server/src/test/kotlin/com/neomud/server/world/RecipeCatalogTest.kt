package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeCatalogTest {

    private fun load() = RecipeCatalog.load(defaultWorldSource())

    @Test
    fun testLoadRecipesFromJson() {
        val catalog = load()
        assertTrue(catalog.recipeCount >= 22, "Should load at least 22 recipes, got ${catalog.recipeCount}")
    }

    @Test
    fun testLookupConsumableRecipe() {
        val catalog = load()
        val recipe = catalog.getRecipe("recipe:antivenom_vial")
        assertNotNull(recipe)
        assertEquals("Antivenom Vial", recipe.name)
        assertEquals("consumable", recipe.category)
        assertEquals("item:antivenom_vial", recipe.outputItemId)
        assertEquals(3, recipe.outputQuantity)
        assertEquals(1, recipe.materials.size)
        assertEquals("item:spider_fang", recipe.materials[0].itemId)
        assertEquals(2, recipe.materials[0].quantity)
    }

    @Test
    fun testLookupWeaponRecipe() {
        val catalog = load()
        val recipe = catalog.getRecipe("recipe:obsidian_edge")
        assertNotNull(recipe)
        assertEquals("weapon", recipe.category)
        assertEquals("item:obsidian_edge", recipe.outputItemId)
        assertEquals(1, recipe.outputQuantity)
        assertEquals(6, recipe.levelRequirement)
        assertEquals(2, recipe.materials.size)
    }

    @Test
    fun testLookupArmorRecipe() {
        val catalog = load()
        val recipe = catalog.getRecipe("recipe:wolf_pelt_cloak")
        assertNotNull(recipe)
        assertEquals("armor", recipe.category)
        assertEquals("item:wolf_pelt_cloak", recipe.outputItemId)
        assertEquals(2, recipe.levelRequirement)
    }

    @Test
    fun testLookupAccessoryRecipe() {
        val catalog = load()
        val recipe = catalog.getRecipe("recipe:wraith_sigil_amulet")
        assertNotNull(recipe)
        assertEquals("accessory", recipe.category)
        assertEquals("item:wraith_sigil_amulet", recipe.outputItemId)
        assertEquals(6, recipe.levelRequirement)
    }

    @Test
    fun testUnknownRecipeReturnsNull() {
        val catalog = load()
        assertNull(catalog.getRecipe("recipe:nonexistent"))
    }

    @Test
    fun testRecipeCostIsSerializedCorrectly() {
        val catalog = load()
        val recipe = catalog.getRecipe("recipe:obsidian_edge")
        assertNotNull(recipe)
        assertTrue(recipe.cost.totalCopper() > 0, "Recipe cost should be > 0")
    }

    @Test
    fun testAllRecipesHaveValidOutputItems() {
        val recipeCatalog = load()
        val itemCatalog = ItemCatalog.load(defaultWorldSource())
        for (recipe in recipeCatalog.getAllRecipes()) {
            assertNotNull(
                itemCatalog.getItem(recipe.outputItemId),
                "Recipe '${recipe.id}' output '${recipe.outputItemId}' not found in item catalog"
            )
        }
    }

    @Test
    fun testAllRecipesHaveValidMaterials() {
        val recipeCatalog = load()
        val itemCatalog = ItemCatalog.load(defaultWorldSource())
        for (recipe in recipeCatalog.getAllRecipes()) {
            for (mat in recipe.materials) {
                assertNotNull(
                    itemCatalog.getItem(mat.itemId),
                    "Recipe '${recipe.id}' material '${mat.itemId}' not found in item catalog"
                )
            }
        }
    }

    @Test
    fun testGetAllRecipes() {
        val catalog = load()
        val all = catalog.getAllRecipes()
        assertTrue(all.size >= 22)
        val ids = all.map { it.id }.toSet()
        assertTrue("recipe:antivenom_vial" in ids)
        assertTrue("recipe:obsidian_edge" in ids)
        assertTrue("recipe:wolf_pelt_cloak" in ids)
    }
}
