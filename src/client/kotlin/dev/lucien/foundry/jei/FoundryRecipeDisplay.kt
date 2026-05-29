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

    val isPooled: Boolean get() = recipe.isPooled

    /** Stacks shown in the primary output slot — the pool entries, cycling, or just [outputTemplate]. */
    val resultStacks: List<ItemStack>
        get() = if (isPooled) recipe.resultPool.map { it.result.create() } else listOf(recipe.result.create())

    /** (stack, "x%") for each pool entry, normalised over the total weight. */
    val resultOdds: List<Pair<ItemStack, String>>
        get() {
            val total = recipe.resultPool.sumOf { it.weight }.coerceAtLeast(1)
            return recipe.resultPool.map { it.result.create() to "${it.weight * 100 / total}%" }
        }

    val hasByproduct: Boolean get() = recipe.byproductChance > 0f

    /** Slag produced every craft (floor of the chance). */
    val byproductGuaranteed: Int get() = recipe.byproductChance.toInt()

    /** Chance (0–1) of one extra slag beyond the guaranteed amount. */
    val byproductExtraChance: Float get() = recipe.byproductChance - byproductGuaranteed
    val byproductExtraPercent: String get() = "${(byproductExtraChance * 100).toInt()}%"

    /** Stack shown in the byproduct slot — at least 1 so the icon renders. */
    val byproductStack: ItemStack get() = ItemStack(ModItems.SLAG, byproductGuaranteed.coerceAtLeast(1))

    val hasBonusResult: Boolean get() = recipe.bonusResultChance > 0f
    val bonusResultChance: Float get() = recipe.bonusResultChance
    val bonusResultChancePercent: String get() = "${(bonusResultChance * 100).toInt()}%"
    val bonusRequiresLava: Boolean get() = recipe.bonusRequiresLava

    val cookingTime: Int get() = recipe.cookingTime
    val cookingTimeSeconds: Float get() = cookingTime / 20f
    val experience: Float get() = recipe.experience
}
