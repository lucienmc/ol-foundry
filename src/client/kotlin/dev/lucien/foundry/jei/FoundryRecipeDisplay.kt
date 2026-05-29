package dev.lucien.foundry.jei

import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModItems
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.crafting.Ingredient

/** JEI view over a [FoundryRecipe], exposing the values the recipe category renders. */
data class FoundryRecipeDisplay(val recipe: FoundryRecipe) {

    val ingredient: Ingredient get() = recipe.ingredient
    val outputTemplate: ItemStackTemplate get() = recipe.result
    val byproduct: ItemStack get() = ItemStack(ModItems.SLAG)

    val byproductChance: Float get() = recipe.byproductChance
    val byproductChancePercent: String get() = "${(byproductChance * 100).toInt()}%"

    val hasBonusResult: Boolean get() = recipe.bonusResultChance > 0f
    val bonusResultChance: Float get() = recipe.bonusResultChance
    val bonusResultChancePercent: String get() = "${(bonusResultChance * 100).toInt()}%"
    val bonusRequiresLava: Boolean get() = recipe.bonusRequiresLava

    val cookingTime: Int get() = recipe.cookingTime
    val cookingTimeSeconds: Float get() = cookingTime / 20f
    val experience: Float get() = recipe.experience
}
