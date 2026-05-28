package dev.lucien.foundry.jei

import dev.lucien.foundry.recipe.FoundryRecipe
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

/**
 * JEI recipe display for Foundry smelting recipes.
 * Shows ingredients, results, and smelting parameters.
 */
data class FoundryRecipeDisplay(
    val recipe: FoundryRecipe
) {
    val ingredient: Ingredient
        get() = recipe.ingredient

    val output: ItemStack
        get() = recipe.result.create()

    val byproductChance: Float
        get() = recipe.byproductChance

    val cookingTime: Int
        get() = recipe.cookingTime

    val cookingTimeSeconds: Float
        get() = cookingTime / 20f

    val experience: Float
        get() = recipe.experience

    // Fuel information for display
    companion object {
        val FUEL_SPEEDS = listOf(
            "Coal/Charcoal" to "1.0x speed (base: 2x, with lava: 4x)",
            "Blaze Rod" to "2.0x speed (base: 4x, with lava: 8x)",
            "Lava Bucket" to "3.0x speed (base: 6x, with lava: 12x)"
        )
    }
}
