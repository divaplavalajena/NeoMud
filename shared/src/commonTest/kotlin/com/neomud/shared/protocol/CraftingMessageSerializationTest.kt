package com.neomud.shared.protocol

import com.neomud.shared.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CraftingMessageSerializationTest {

    @Test
    fun testInteractCrafterRoundTrip() {
        val msg = ClientMessage.InteractCrafter
        val encoded = MessageSerializer.encodeClientMessage(msg)
        val decoded = MessageSerializer.decodeClientMessage(encoded)
        assertIs<ClientMessage.InteractCrafter>(decoded)
    }

    @Test
    fun testCraftItemRoundTrip() {
        val msg = ClientMessage.CraftItem("recipe:antivenom_vial")
        val encoded = MessageSerializer.encodeClientMessage(msg)
        val decoded = MessageSerializer.decodeClientMessage(encoded)
        assertIs<ClientMessage.CraftItem>(decoded)
        assertEquals("recipe:antivenom_vial", decoded.recipeId)
    }

    @Test
    fun testCraftingMenuRoundTrip() {
        val msg = ServerMessage.CraftingMenu(
            crafterName = "Grimjaw",
            recipes = listOf(
                RecipeInfo(
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
            ),
            playerCoins = Coins(gold = 1, silver = 5)
        )

        val encoded = MessageSerializer.encodeServerMessage(msg)
        val decoded = MessageSerializer.decodeServerMessage(encoded)
        assertIs<ServerMessage.CraftingMenu>(decoded)
        assertEquals("Grimjaw", decoded.crafterName)
        assertEquals(1, decoded.recipes.size)
        assertTrue(decoded.recipes[0].canCraft)
        assertEquals(1, decoded.playerCoins.gold)
    }

    @Test
    fun testCraftResultRoundTrip() {
        val msg = ServerMessage.CraftResult(
            success = true,
            itemName = "Antivenom Vial",
            message = "You crafted Antivenom Vial x3!",
            updatedCoins = Coins(silver = 4),
            updatedInventory = listOf(
                InventoryItem(itemId = "item:antivenom_vial", quantity = 3, equipped = false, slot = "")
            ),
            equipment = mapOf("weapon" to "item:iron_sword")
        )

        val encoded = MessageSerializer.encodeServerMessage(msg)
        val decoded = MessageSerializer.decodeServerMessage(encoded)
        assertIs<ServerMessage.CraftResult>(decoded)
        assertTrue(decoded.success)
        assertEquals("Antivenom Vial", decoded.itemName)
        assertEquals(4, decoded.updatedCoins.silver)
    }
}
