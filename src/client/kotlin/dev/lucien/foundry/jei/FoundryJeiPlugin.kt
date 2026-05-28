package dev.lucien.foundry.jei

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.recipe.FoundryRecipe
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient

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
        // Mirror what's defined in data/foundry/recipe/foundry/*.json
        val recipes = listOf(
            recipe(Items.RAW_IRON,          Items.IRON_INGOT,   byproduct = 0.35f, time = 160,  xp = 0.7f),
            recipe(Items.RAW_GOLD,          Items.GOLD_INGOT,   byproduct = 0.35f, time = 160,  xp = 1.0f),
            recipe(Items.RAW_COPPER,        Items.COPPER_INGOT, byproduct = 0.30f, time = 150,  xp = 0.7f),
            recipe(Items.RAW_IRON_BLOCK,    Items.IRON_BLOCK,   byproduct = 3.15f, time = 1200, xp = 6.3f),
            recipe(Items.RAW_GOLD_BLOCK,    Items.GOLD_BLOCK,   byproduct = 3.15f, time = 1200, xp = 9.0f),
            recipe(Items.RAW_COPPER_BLOCK,  Items.COPPER_BLOCK, byproduct = 2.70f, time = 1200, xp = 6.3f),
            recipe(Items.COBBLESTONE,       Items.STONE,        byproduct = 0.0f,  time = 100,  xp = 0.1f),
            recipe(Items.GRAVEL,            Items.FLINT,        byproduct = 0.0f,  time = 120,  xp = 0.1f),
            recipe(Items.SAND,              Items.GLASS,        byproduct = 0.0f,  time = 200,  xp = 0.1f),
            recipe(Items.RED_SAND,          Items.GLASS,        byproduct = 0.0f,  time = 200,  xp = 0.1f),
        )
        registration.addRecipes(FOUNDRY_RECIPE_TYPE, recipes.map(::FoundryRecipeDisplay))
    }

    private fun recipe(
        input: net.minecraft.world.item.Item,
        output: net.minecraft.world.item.Item,
        byproduct: Float,
        time: Int,
        xp: Float,
    ) = FoundryRecipe(
        ingredient      = Ingredient.of(input),
        result          = ItemStackTemplate(output),
        byproductChance = byproduct,
        cookingTime     = time,
        experience      = xp,
    )

    companion object {
        val FOUNDRY_RECIPE_TYPE: RecipeType<FoundryRecipeDisplay> =
            RecipeType.create(Foundry.MOD_ID, "foundry", FoundryRecipeDisplay::class.java)
    }
}
