package dev.lucien.foundry.jei

import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModRecipes
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles recipe discovery for JEI in a deferred manner.
 * Waits until the recipe manager is populated before registering with JEI.
 */
object FoundryJeiEvents {
    private val hasAttemptedLoad = AtomicBoolean(false)

    fun init() {
        // Listen for client ticks to detect when recipes are available
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!hasAttemptedLoad.get() && client.level != null) {
                attemptLoadRecipes(client)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun attemptLoadRecipes(client: Minecraft) {
        try {
            val level = client.level ?: return

            // Try to access the recipe manager through reflection
            // This works because the client level does have a reference to the recipe manager
            val recipeManagerField = level.javaClass.getDeclaredFields().find {
                it.name.contains("recipe", ignoreCase = true) &&
                it.name.contains("manager", ignoreCase = true)
            } ?: return

            recipeManagerField.isAccessible = true
            val recipeManager = recipeManagerField.get(level) ?: return

            // Try to get all recipes of our type
            val getAllRecipesForMethod = recipeManager.javaClass.methods.find { method ->
                method.name == "getAllRecipesFor" && method.parameterCount == 1
            } ?: return

            @Suppress("UNCHECKED_CAST")
            val recipes = getAllRecipesForMethod.invoke(
                recipeManager,
                ModRecipes.FOUNDRY_RECIPE_TYPE
            ) as? Collection<Any>

            if (recipes != null && recipes.isNotEmpty()) {
                val foundryRecipes = recipes.filterIsInstance<FoundryRecipe>()
                if (foundryRecipes.isNotEmpty()) {
                    FoundryRecipeGatherer.addRecipes(foundryRecipes)
                    hasAttemptedLoad.set(true)
                }
            }
        } catch (e: Exception) {
            // Silently fail - recipes will be unavailable
            hasAttemptedLoad.set(true)
        }
    }
}
