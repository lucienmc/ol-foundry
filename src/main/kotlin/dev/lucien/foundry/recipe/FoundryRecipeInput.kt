package dev.lucien.foundry.recipe

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeInput

/**
 * The recipe input for the Foundry. Contains the item to be smelted.
 * The [hasLava] flag is used for recipe matching if we want lava-only recipes in future.
 */
data class FoundryRecipeInput(
    val inputItem: ItemStack,
    val hasLava: Boolean = false
) : RecipeInput {

    override fun getItem(index: Int): ItemStack = when (index) {
        0 -> inputItem
        else -> ItemStack.EMPTY
    }

    override fun size(): Int = 1
}
