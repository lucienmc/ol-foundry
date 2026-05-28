package dev.lucien.foundry.jei

import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModRecipes
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gathers FoundryRecipe recipes for JEI to display.
 * In JEI 29.6.2, recipes are synced from the server automatically.
 * This gatherer caches them for the JEI plugin to access.
 */
object FoundryRecipeGatherer {
    private val cachedRecipes = CopyOnWriteArrayList<FoundryRecipe>()

    fun init() {
        // Initialization is minimal - recipes will be gathered dynamically
        // when JEI requests them or when they become available
    }

    /**
     * Called by the JEI plugin to retrieve registered recipes.
     * Returns all cached FoundryRecipe objects.
     */
    fun getRecipes(): List<FoundryRecipe> = cachedRecipes.toList()

    fun hasRecipes(): Boolean = cachedRecipes.isNotEmpty()

    /**
     * Internal method to add recipes to the cache.
     * Can be called by event listeners or the JEI plugin.
     */
    fun addRecipe(recipe: FoundryRecipe) {
        cachedRecipes.add(recipe)
    }

    fun addRecipes(recipes: Collection<FoundryRecipe>) {
        cachedRecipes.addAll(recipes)
    }

    fun clear() {
        cachedRecipes.clear()
    }
}
