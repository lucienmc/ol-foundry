package dev.lucien.foundry.jei

import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModRecipes
import mezz.jei.api.registration.IRecipeRegistration
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

/**
 * Event listeners for JEI recipe management.
 * Provides recipes to JEI when they become available.
 */
object FoundryJeiEvents {
    private var hasRegisteredWithJei = false

    fun register() {
        // On client tick, try to load and register recipes
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!hasRegisteredWithJei && client.level != null && client.player != null) {
                loadAndRegisterRecipes(client)
                hasRegisteredWithJei = true
            }
        }
    }

    fun registerJeiRuntime(registration: IRecipeRegistration) {
        // This is called by JEI during plugin initialization
        // Try to gather recipes immediately if possible
        val client = Minecraft.getInstance()
        val recipes = gatherRecipesFromLevel(client)

        if (recipes.isNotEmpty()) {
            val displays = recipes.map { FoundryRecipeDisplay(it) }
            registration.addRecipes(FoundryJeiPlugin.FOUNDRY_RECIPE_TYPE, displays)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadAndRegisterRecipes(client: Minecraft) {
        try {
            val recipes = gatherRecipesFromLevel(client)
            if (recipes.isNotEmpty()) {
                FoundryRecipeGatherer.addRecipes(recipes)
            }
        } catch (e: Exception) {
            // Silently fail - recipes might not be available yet
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun gatherRecipesFromLevel(client: Minecraft): List<FoundryRecipe> {
        val recipes = mutableListOf<FoundryRecipe>()

        try {
            val level = client.level ?: return recipes

            // Try to access the recipe manager through reflection
            val recipeManagerMethod = level.javaClass.getMethod("getRecipeManager")
            val recipeManager = recipeManagerMethod.invoke(level)

            val getAllRecipesForMethod = recipeManager.javaClass.getMethod(
                "getAllRecipesFor",
                Class.forName("net.minecraft.world.item.crafting.RecipeType")
            )

            @Suppress("UNCHECKED_CAST")
            val foundryRecipes = getAllRecipesForMethod.invoke(
                recipeManager, ModRecipes.FOUNDRY_RECIPE_TYPE
            ) as? Collection<*>

            if (foundryRecipes != null) {
                for (recipe in foundryRecipes) {
                    if (recipe is FoundryRecipe) {
                        recipes.add(recipe)
                    }
                }
            }
        } catch (e: Exception) {
            // If direct method access fails, recipes will be unavailable
            // This is expected during early initialization
        }

        return recipes
    }
}
