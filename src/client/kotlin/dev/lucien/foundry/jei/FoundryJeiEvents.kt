package dev.lucien.foundry.jei

import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModRecipes
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

/**
 * Event listeners for JEI recipe management.
 * Gathers recipes from the client's recipe manager when a level is loaded.
 */
object FoundryJeiEvents {
    private var hasLoadedRecipes = false

    fun register() {
        // On client tick, try to load recipes when a level is available
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!hasLoadedRecipes && client.level != null && client.player != null) {
                loadRecipesFromLevel(client)
                hasLoadedRecipes = true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadRecipesFromLevel(client: Minecraft) {
        try {
            val level = client.level ?: return

            // Get all recipes of our type from the level's recipe manager
            // In MC 26.1.2, we access recipes through the level's recipe manager
            val recipes = mutableListOf<FoundryRecipe>()

            // Use reflection as a fallback if direct method access fails
            try {
                // Try the direct approach first (may work in some setups)
                val recipeClass = Class.forName("net.minecraft.world.level.Level")
                val getRecipeManagerMethod = recipeClass.getMethod("getRecipeManager")
                val recipeManager = getRecipeManagerMethod.invoke(level)

                val recipeManagerClass = recipeManager.javaClass
                val getAllRecipesForMethod = recipeManagerClass.getMethod(
                    "getAllRecipesFor",
                    Class.forName("net.minecraft.world.item.crafting.RecipeType")
                )

                @Suppress("UNCHECKED_CAST")
                val foundryRecipes = getAllRecipesForMethod.invoke(
                    recipeManager, ModRecipes.FOUNDRY_RECIPE_TYPE
                ) as Collection<FoundryRecipe>

                recipes.addAll(foundryRecipes)
            } catch (e: Exception) {
                // If direct method access fails, log and skip
                // Recipes can still be added manually if needed
            }

            if (recipes.isNotEmpty()) {
                FoundryRecipeGatherer.addRecipes(recipes)
            }
        } catch (e: Exception) {
            // Silently fail - recipes might not be available yet
        }
    }
}
