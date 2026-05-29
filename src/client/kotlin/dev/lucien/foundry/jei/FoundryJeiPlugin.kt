package dev.lucien.foundry.jei

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModBlocks
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.types.IRecipeType
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.RecipeManager

@JeiPlugin
class FoundryJeiPlugin : mezz.jei.api.IModPlugin {

    override fun getPluginUid(): Identifier =
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "jei_plugin")

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(
            FoundryRecipeCategory(registration.jeiHelpers.guiHelper)
        )
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addCraftingStation(FOUNDRY_RECIPE_TYPE, ModBlocks.FOUNDRY)
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val level = Minecraft.getInstance().level ?: return
        val manager = level.recipeAccess() as? RecipeManager ?: return
        val recipes =
            manager.recipes.mapNotNull { it.value() as? FoundryRecipe }.map(::FoundryRecipeDisplay)
        registration.addRecipes(FOUNDRY_RECIPE_TYPE, recipes)
    }

    companion object {
        val FOUNDRY_RECIPE_TYPE: IRecipeType<FoundryRecipeDisplay> =
            IRecipeType.create(Foundry.MOD_ID, "foundry", FoundryRecipeDisplay::class.java)
    }
}
