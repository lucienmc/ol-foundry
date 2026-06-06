package dev.lucien.foundry.jei

import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModItems
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.crafting.Ingredient

/** JEI view over a [FoundryRecipe] — all values computed once at construction. */
data class FoundryRecipeDisplay(val recipe: FoundryRecipe) {

    val ingredient: Ingredient = recipe.ingredient
    val outputTemplate: ItemStackTemplate = recipe.result
    val isPooled: Boolean = recipe.isPooled
    val cookingTime: Int = recipe.cookingTime
    val cookingTimeSeconds: Float = recipe.cookingTime / 20f
    val experience: Float = recipe.experience

    val hasBonusResult: Boolean = recipe.bonusResultChance > 0f
    val bonusResultChancePercent: String = "${(recipe.bonusResultChance * 100).toInt()}%"
    val bonusRequiresLava: Boolean = recipe.bonusRequiresLava

    val hasByproduct: Boolean = recipe.byproductChance > 0f
    val byproductGuaranteed: Int = recipe.byproductChance.toInt()
    val byproductExtraChance: Float = recipe.byproductChance - byproductGuaranteed
    val byproductExtraPercent: String = "${(byproductExtraChance * 100).toInt()}%"
    val byproductStack: ItemStack = ItemStack(ModItems.SLAG, byproductGuaranteed.coerceAtLeast(1))

    // Cycling stacks shown in the primary output slot.
    val resultStacks: List<ItemStack> =
        if (isPooled) recipe.resultPool.map { it.result.create() } else listOf(recipe.result.create())

    // (stack, "x%") for each pool entry, weight-normalised.
    val resultOdds: List<Pair<ItemStack, String>> = recipe.resultPool.let { pool ->
        val total = pool.sumOf { it.weight }.coerceAtLeast(1)
        pool.map { it.result.create() to "${it.weight * 100 / total}%" }
    }
}
