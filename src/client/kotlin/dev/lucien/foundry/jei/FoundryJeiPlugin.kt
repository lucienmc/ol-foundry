package dev.lucien.foundry.jei

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.registry.ModRecipes
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
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

    override fun registerRecipes(registration: IRecipeRegistration) {
        // Recipes are gathered asynchronously by FoundryJeiEvents
        // Add them to JEI as they become available
        val recipes = FoundryRecipeGatherer.getRecipes()
        val displays = recipes.map { FoundryRecipeDisplay(it) }
        if (displays.isNotEmpty()) {
            registration.addRecipes(FOUNDRY_RECIPE_TYPE, displays)
        }

        // Note: If recipes are empty at this point, they haven't been discovered yet.
        // They will be added when FoundryJeiEvents discovers them after the client fully loads.
    }

    companion object {
        val FOUNDRY_RECIPE_TYPE: RecipeType<FoundryRecipeDisplay> =
            RecipeType.create(Foundry.MOD_ID, "foundry", FoundryRecipeDisplay::class.java)
    }
}
