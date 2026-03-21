package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.RestUtils
import com.neomud.server.game.StealthUtils
import com.neomud.server.game.crafting.CraftingService
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.RecipeCatalog
import com.neomud.shared.protocol.ServerMessage
import org.slf4j.LoggerFactory

class CraftCommand(
    private val npcManager: NpcManager,
    private val craftingService: CraftingService,
    private val recipeCatalog: RecipeCatalog,
    private val inventoryCommand: InventoryCommand,
    private val sessionManager: SessionManager,
    private val inventoryRepository: InventoryRepository,
    private val coinRepository: CoinRepository
) {
    private val logger = LoggerFactory.getLogger(CraftCommand::class.java)

    suspend fun handleInteract(session: PlayerSession) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        MeditationUtils.breakMeditation(session, "You stop meditating.")
        RestUtils.breakRest(session, "You stop resting.")
        StealthUtils.breakStealth(session, sessionManager, "Interacting with a crafter reveals your presence!")

        val crafter = npcManager.getCrafterInRoom(roomId)
        if (crafter == null) {
            session.send(ServerMessage.SystemMessage("There is no crafter here."))
            return
        }

        val recipeInfos = crafter.crafterRecipes.mapNotNull { recipeId ->
            val recipe = recipeCatalog.getRecipe(recipeId) ?: return@mapNotNull null
            craftingService.buildRecipeInfo(recipe, playerName, player)
        }

        val playerCoins = coinRepository.getCoins(playerName)

        session.send(ServerMessage.CraftingMenu(
            crafterName = crafter.name,
            recipes = recipeInfos,
            playerCoins = playerCoins,
            interactSound = crafter.interactSound,
            exitSound = crafter.exitSound
        ))
    }

    suspend fun handleCraft(session: PlayerSession, recipeId: String) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val crafter = npcManager.getCrafterInRoom(roomId)
        if (crafter == null) {
            session.send(ServerMessage.SystemMessage("There is no crafter here."))
            return
        }

        if (recipeId !in crafter.crafterRecipes) {
            session.send(ServerMessage.Error("This crafter doesn't know that recipe."))
            return
        }

        val recipe = recipeCatalog.getRecipe(recipeId)
        if (recipe == null) {
            session.send(ServerMessage.Error("Unknown recipe."))
            return
        }

        val outcome = craftingService.craft(recipe, playerName, player)

        val updatedCoins = coinRepository.getCoins(playerName)
        val updatedInventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)

        session.send(ServerMessage.CraftResult(
            success = outcome.success,
            itemName = outcome.itemName,
            message = outcome.message,
            updatedCoins = updatedCoins,
            updatedInventory = updatedInventory,
            equipment = equipment
        ))

        if (outcome.success) {
            inventoryCommand.sendInventoryUpdate(session)
        }
    }
}
