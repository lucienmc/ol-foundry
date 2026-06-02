package dev.lucien.foundry.recipe

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeInput

/** The recipe input for the Foundry — the single item to be smelted. */
data class FoundryRecipeInput(val inputItem: ItemStack) : RecipeInput {

    override fun getItem(index: Int): ItemStack =
        if (index == 0) inputItem else ItemStack.EMPTY

    override fun size(): Int = 1
}
