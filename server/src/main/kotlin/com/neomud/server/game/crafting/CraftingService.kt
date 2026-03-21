package com.neomud.server.game.crafting

import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.world.ItemCatalog
import com.neomud.server.world.RecipeCatalog
import com.neomud.shared.model.*
import org.slf4j.LoggerFactory

class CraftingService(
    private val recipeCatalog: RecipeCatalog,
    private val itemCatalog: ItemCatalog,
    private val inventoryRepository: InventoryRepository,
    private val coinRepository: CoinRepository
) {
    private val logger = LoggerFactory.getLogger(CraftingService::class.java)

    fun buildRecipeInfo(recipe: Recipe, playerName: String, player: Player): RecipeInfo {
        val inventory = inventoryRepository.getInventory(playerName)
        val playerCoins = coinRepository.getCoins(playerName)

        val materialStatus = recipe.materials.map { mat ->
            val available = inventory.filter { it.itemId == mat.itemId && !it.equipped }.sumOf { it.quantity }
            val item = itemCatalog.getItem(mat.itemId)
            MaterialStatus(
                itemId = mat.itemId,
                itemName = item?.name ?: mat.itemId,
                required = mat.quantity,
                available = available
            )
        }

        val hasMaterials = materialStatus.all { it.available >= it.required }
        val hasGold = playerCoins.totalCopper() >= recipe.cost.totalCopper()
        val meetsLevel = player.level >= recipe.levelRequirement
        val meetsClass = recipe.classRestriction?.let { player.characterClass in it } ?: true

        return RecipeInfo(
            recipe = recipe,
            canCraft = hasMaterials && hasGold && meetsLevel && meetsClass,
            materialStatus = materialStatus
        )
    }

    data class CraftOutcome(
        val success: Boolean,
        val itemName: String,
        val message: String
    )

    fun craft(recipe: Recipe, playerName: String, player: Player): CraftOutcome {
        // Check level
        if (player.level < recipe.levelRequirement) {
            return CraftOutcome(false, "", "You need to be level ${recipe.levelRequirement} to craft ${recipe.name}.")
        }

        // Check class
        val restriction = recipe.classRestriction
        if (restriction != null && player.characterClass !in restriction) {
            return CraftOutcome(false, "", "Your class cannot craft ${recipe.name}.")
        }

        // Check materials
        val inventory = inventoryRepository.getInventory(playerName)
        for (mat in recipe.materials) {
            val available = inventory.filter { it.itemId == mat.itemId && !it.equipped }.sumOf { it.quantity }
            if (available < mat.quantity) {
                val itemName = itemCatalog.getItem(mat.itemId)?.name ?: mat.itemId
                return CraftOutcome(false, "", "You need ${mat.quantity} $itemName but only have $available.")
            }
        }

        // Check gold
        val playerCoins = coinRepository.getCoins(playerName)
        if (playerCoins.totalCopper() < recipe.cost.totalCopper()) {
            return CraftOutcome(false, "", "You can't afford the crafting fee of ${recipe.cost.displayString()}.")
        }

        // Deduct coins first
        val coinsDeducted = coinRepository.subtractCoins(playerName, recipe.cost)
        if (!coinsDeducted) {
            return CraftOutcome(false, "", "You can't afford the crafting fee of ${recipe.cost.displayString()}.")
        }

        // Deduct materials — track consumed for rollback on partial failure
        val consumed = mutableListOf<RecipeIngredient>()
        for (mat in recipe.materials) {
            val removed = inventoryRepository.removeItem(playerName, mat.itemId, mat.quantity)
            if (!removed) {
                // Refund previously consumed materials
                for (prev in consumed) {
                    inventoryRepository.addItem(playerName, prev.itemId, prev.quantity)
                }
                // Refund coins
                coinRepository.addCoins(playerName, recipe.cost)
                return CraftOutcome(false, "", "Failed to consume materials for ${recipe.name}.")
            }
            consumed.add(mat)
        }

        // Add output item
        val outputItem = itemCatalog.getItem(recipe.outputItemId)
        val itemName = outputItem?.name ?: recipe.outputItemId
        val added = inventoryRepository.addItem(playerName, recipe.outputItemId, recipe.outputQuantity)
        if (!added) {
            // Refund all consumed materials and coins
            for (prev in consumed) {
                inventoryRepository.addItem(playerName, prev.itemId, prev.quantity)
            }
            coinRepository.addCoins(playerName, recipe.cost)
            return CraftOutcome(false, "", "Failed to add $itemName to your inventory.")
        }

        val qtyStr = if (recipe.outputQuantity > 1) " x${recipe.outputQuantity}" else ""
        logger.info("$playerName crafted ${recipe.name} ($itemName$qtyStr) for ${recipe.cost.displayString()}")
        return CraftOutcome(true, itemName, "You crafted $itemName$qtyStr!")
    }
}
