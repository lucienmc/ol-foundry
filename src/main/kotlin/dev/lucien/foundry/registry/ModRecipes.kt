package dev.lucien.foundry.registry

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.recipe.FoundryRecipe
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType

object ModRecipes {

    val FOUNDRY_RECIPE_TYPE: RecipeType<FoundryRecipe> = Registry.register(
        BuiltInRegistries.RECIPE_TYPE,
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "foundry_smelting"),
        object : RecipeType<FoundryRecipe> {}
    )

    val FOUNDRY_RECIPE_SERIALIZER: RecipeSerializer<FoundryRecipe> = Registry.register(
        BuiltInRegistries.RECIPE_SERIALIZER,
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "foundry_smelting"),
        RecipeSerializer(FoundryRecipe.CODEC, FoundryRecipe.STREAM_CODEC)
    )

    fun init() {
        // Sync recipes to client so recipe viewers (JEI etc.) can display them
        RecipeSynchronization.synchronizeRecipeSerializer(FOUNDRY_RECIPE_SERIALIZER)
    }
}
