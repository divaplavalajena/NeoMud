package com.neomud.server.game.crafting

import com.neomud.server.persistence.DatabaseFactory
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.world.ItemCatalog
import com.neomud.server.world.RecipeCatalog
import com.neomud.shared.model.*
import java.io.File
import kotlin.test.*

class CraftingServiceTest {

    private val testItems = listOf(
        Item(id = "item:spider_fang", name = "Spider Fang", description = "Fang", type = "crafting", value = 8, stackable = true, maxStack = 20),
        Item(id = "item:wolf_pelt", name = "Wolf Pelt", description = "Pelt", type = "crafting", value = 10, stackable = true, maxStack = 20),
        Item(id = "item:marsh_hide", name = "Marsh Hide", description = "Hide", type = "crafting", value = 12, stackable = true, maxStack = 20),
        Item(id = "item:antivenom_vial", name = "Antivenom Vial", description = "Cures poison", type = "consumable", value = 15, stackable = true, maxStack = 10, useEffect = "cure_dot"),
        Item(id = "item:wolf_pelt_cloak", name = "Wolf Pelt Cloak", description = "Cloak", type = "armor", slot = "back", armorValue = 2, value = 35)
    )

    private val testRecipes = listOf(
        Recipe(
            id = "recipe:antivenom_vial",
            name = "Antivenom Vial",
            category = "consumable",
            materials = listOf(RecipeIngredient("item:spider_fang", 2)),
            cost = Coins(silver = 1),
            outputItemId = "item:antivenom_vial",
            outputQuantity = 3,
            levelRequirement = 1
        ),
        Recipe(
            id = "recipe:wolf_pelt_cloak",
            name = "Wolf Pelt Cloak",
            category = "armor",
            materials = listOf(RecipeIngredient("item:wolf_pelt", 3)),
            cost = Coins(silver = 2),
            outputItemId = "item:wolf_pelt_cloak",
            levelRequirement = 2
        ),
        Recipe(
            id = "recipe:class_restricted",
            name = "Class Restricted",
            category = "weapon",
            materials = listOf(RecipeIngredient("item:spider_fang", 1)),
            cost = Coins(copper = 10),
            outputItemId = "item:antivenom_vial",
            levelRequirement = 1,
            classRestriction = listOf("MAGE", "PRIEST")
        )
    )

    private val itemCatalog = ItemCatalog(testItems)
    private val recipeCatalog = RecipeCatalog(testRecipes)
    private val testDbFile = File.createTempFile("crafting-test-", ".db").also { it.deleteOnExit() }

    private fun createTestPlayer(level: Int = 5, characterClass: String = "WARRIOR"): Player = Player(
        name = "CraftTester",
        characterClass = characterClass,
        race = "HUMAN",
        level = level,
        currentHp = 100,
        maxHp = 100,
        currentMp = 50,
        maxMp = 50,
        currentRoomId = "test:room1",
        currentXp = 0,
        xpToNextLevel = 1000,
        stats = Stats(strength = 20, agility = 15, intellect = 10, willpower = 12, health = 18, charm = 10),
        unspentCp = 0,
        totalCpEarned = 0
    )

    @BeforeTest
    fun setup() {
        testDbFile.delete()
        DatabaseFactory.init("jdbc:sqlite:${testDbFile.absolutePath}")
    }

    @Test
    fun testCraftSucceedsWithSufficientMaterials() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        // Give materials and coins
        inventoryRepo.addItem(playerName, "item:spider_fang", 5)
        coinRepo.addCoins(playerName, Coins(silver = 5))

        val result = service.craft(testRecipes[0], playerName, createTestPlayer())
        assertTrue(result.success)
        assertTrue(result.message.contains("Antivenom Vial"))

        // Verify materials consumed: 5 - 2 = 3 remaining
        val inventory = inventoryRepo.getInventory(playerName)
        val silk = inventory.find { it.itemId == "item:spider_fang" }
        assertEquals(3, silk?.quantity)

        // Verify output added: 3 antivenom vials
        val vials = inventory.find { it.itemId == "item:antivenom_vial" }
        assertEquals(3, vials?.quantity)

        // Verify coins deducted
        val coins = coinRepo.getCoins(playerName)
        assertEquals(4, coins.silver) // 5 - 1 = 4
    }

    @Test
    fun testCraftFailsWithInsufficientMaterials() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:spider_fang", 1) // need 2
        coinRepo.addCoins(playerName, Coins(silver = 5))

        val result = service.craft(testRecipes[0], playerName, createTestPlayer())
        assertFalse(result.success)
        assertTrue(result.message.contains("need"))
    }

    @Test
    fun testCraftFailsWithInsufficientGold() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:spider_fang", 5)
        coinRepo.addCoins(playerName, Coins(copper = 10)) // need 1 silver = 100 copper

        val result = service.craft(testRecipes[0], playerName, createTestPlayer())
        assertFalse(result.success)
        assertTrue(result.message.contains("afford"))
    }

    @Test
    fun testCraftFailsWithLevelTooLow() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:wolf_pelt", 5)
        coinRepo.addCoins(playerName, Coins(silver = 5))

        val result = service.craft(testRecipes[1], playerName, createTestPlayer(level = 1)) // need level 2
        assertFalse(result.success)
        assertTrue(result.message.contains("level"))
    }

    @Test
    fun testCraftFailsWithWrongClass() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:spider_fang", 5)
        coinRepo.addCoins(playerName, Coins(silver = 5))

        val result = service.craft(testRecipes[2], playerName, createTestPlayer(characterClass = "WARRIOR"))
        assertFalse(result.success)
        assertTrue(result.message.contains("class"))
    }

    @Test
    fun testCraftSucceedsWithAllowedClass() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:spider_fang", 5)
        coinRepo.addCoins(playerName, Coins(silver = 5))

        val result = service.craft(testRecipes[2], playerName, createTestPlayer(characterClass = "MAGE"))
        assertTrue(result.success)
    }

    @Test
    fun testBuildRecipeInfoReportsCorrectMaterialStatus() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:spider_fang", 1) // need 2

        val info = service.buildRecipeInfo(testRecipes[0], playerName, createTestPlayer())
        assertFalse(info.canCraft) // not enough materials, no gold
        assertEquals(1, info.materialStatus.size)
        assertEquals("item:spider_fang", info.materialStatus[0].itemId)
        assertEquals(2, info.materialStatus[0].required)
        assertEquals(1, info.materialStatus[0].available)
    }

    @Test
    fun testCraftRefundsMaterialsOnPartialFailure() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        // Recipe requiring 2 different materials: wolf_pelt(3) + marsh_hide(1) — but we use a multi-material recipe
        val multiMatRecipe = Recipe(
            id = "recipe:multi_mat",
            name = "Multi Material",
            category = "consumable",
            materials = listOf(
                RecipeIngredient("item:spider_fang", 2),
                RecipeIngredient("item:marsh_hide", 3)  // player won't have enough
            ),
            cost = Coins(copper = 10),
            outputItemId = "item:antivenom_vial",
            levelRequirement = 1
        )

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:spider_fang", 5)
        inventoryRepo.addItem(playerName, "item:marsh_hide", 1) // need 3, only have 1
        coinRepo.addCoins(playerName, Coins(silver = 5))

        // Pre-check passes because we have enough spider fangs, but marsh_hide removal will fail
        // The availability check should catch this, but if it didn't the rollback should protect us
        val result = service.craft(multiMatRecipe, playerName, createTestPlayer())
        assertFalse(result.success)

        // Verify spider fangs are still intact (5 — not reduced)
        val inventory = inventoryRepo.getInventory(playerName)
        val fangs = inventory.find { it.itemId == "item:spider_fang" }
        assertEquals(5, fangs?.quantity, "Spider fangs should be fully refunded after partial failure")

        // Verify coins untouched
        val coins = coinRepo.getCoins(playerName)
        assertEquals(5, coins.silver, "Coins should be fully refunded after partial failure")
    }

    @Test
    fun testBuildRecipeInfoCanCraftWhenEverythingSufficient() {
        val inventoryRepo = InventoryRepository(itemCatalog)
        val coinRepo = CoinRepository()
        val service = CraftingService(recipeCatalog, itemCatalog, inventoryRepo, coinRepo)

        val playerName = "CraftTester"
        inventoryRepo.addItem(playerName, "item:spider_fang", 5)
        coinRepo.addCoins(playerName, Coins(silver = 5))

        val info = service.buildRecipeInfo(testRecipes[0], playerName, createTestPlayer())
        assertTrue(info.canCraft)
    }
}
