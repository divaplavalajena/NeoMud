package com.neomud.server.world

import com.neomud.shared.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class RecipeCatalogData(val recipes: List<Recipe>)

class RecipeCatalog(recipes: List<Recipe>) {
    private val recipeMap: Map<String, Recipe> = recipes.associateBy { it.id }

    val recipeCount: Int get() = recipeMap.size

    fun getRecipe(id: String): Recipe? = recipeMap[id]

    fun getAllRecipes(): List<Recipe> = recipeMap.values.toList()

    companion object {
        private val logger = LoggerFactory.getLogger(RecipeCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(source: WorldDataSource): RecipeCatalog {
            val content = source.readText("world/recipes.json")
            if (content == null) {
                logger.info("No recipes.json found — crafting disabled")
                return RecipeCatalog(emptyList())
            }
            val data = json.decodeFromString<RecipeCatalogData>(content)
            logger.info("Loaded ${data.recipes.size} recipes")
            return RecipeCatalog(data.recipes)
        }
    }
}
