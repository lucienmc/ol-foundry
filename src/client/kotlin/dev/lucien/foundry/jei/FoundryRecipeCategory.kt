package dev.lucien.foundry.jei

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.registry.ModBlocks
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class FoundryRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<FoundryRecipeDisplay> {

    private val icon = ItemStack(ModBlocks.FOUNDRY)
    private val background = guiHelper.createBlankDrawable(160, 90)

    override fun getRecipeType(): RecipeType<FoundryRecipeDisplay> =
        FoundryJeiPlugin.FOUNDRY_RECIPE_TYPE

    override fun getTitle(): Component =
        Component.literal("Foundry Smelting")

    override fun getWidth(): Int = 160

    override fun getHeight(): Int = 90

    override fun getIcon(): mezz.jei.api.gui.drawable.IDrawable? =
        null

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        display: FoundryRecipeDisplay,
        focuses: IFocusGroup
    ) {
        // Input slot
        builder.addInputSlot(10, 30)
            .addIngredients(display.ingredient)

        // Primary output slot
        builder.addOutputSlot(140, 30)
            .addItemStack(display.output)

        // Byproduct (slag) slot - shows with chance percentage
        if (display.byproductChance > 0) {
            builder.addOutputSlot(140, 50)
                .addItemStack(display.byproduct)
        }
    }

    override fun draw(
        display: FoundryRecipeDisplay,
        recipeSlotsView: IRecipeSlotsView,
        graphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double
    ) {
        // JEI handles basic layout rendering.
        // Fuel speed information is documented in FoundryRecipeDisplay.FUEL_SPEEDS
        // and can be viewed via tooltips or the JEI UI.
    }
}
