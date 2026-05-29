package dev.lucien.foundry.jei

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.registry.ModBlocks
import dev.lucien.foundry.registry.ModRecipes
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.types.IRecipeType
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

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
        val recipes = level.recipeAccess()
            .getSynchronizedRecipes()
            .getAllOfType(ModRecipes.FOUNDRY_RECIPE_TYPE)
            .map { FoundryRecipeDisplay(it.value()) }
        registration.addRecipes(FOUNDRY_RECIPE_TYPE, recipes)
    }

    companion object {
        val FOUNDRY_RECIPE_TYPE: IRecipeType<FoundryRecipeDisplay> =
            IRecipeType.create(Foundry.MOD_ID, "foundry", FoundryRecipeDisplay::class.java)
    }
}
