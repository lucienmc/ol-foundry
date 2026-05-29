package dev.lucien.foundry.jei

import dev.lucien.foundry.registry.ModBlocks
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.types.IRecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class FoundryRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<FoundryRecipeDisplay> {

    private val background = guiHelper.createBlankDrawable(WIDTH, HEIGHT)

    override fun getRecipeType(): IRecipeType<FoundryRecipeDisplay> =
        FoundryJeiPlugin.FOUNDRY_RECIPE_TYPE

    override fun getTitle(): Component = Component.literal("Foundry Smelting")

    override fun getWidth() = WIDTH
    override fun getHeight() = HEIGHT

    override fun getIcon(): mezz.jei.api.gui.drawable.IDrawable? = null

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        display: FoundryRecipeDisplay,
        focuses: IFocusGroup,
    ) {
        builder.addInputSlot(INPUT_X, SLOT_Y)
            .add(display.ingredient)

        builder.addOutputSlot(OUTPUT_X, SLOT_Y)
            .add(display.outputTemplate)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(
                    Component.literal("%.1f XP".format(display.experience))
                        .withStyle(ChatFormatting.GOLD)
                )
                tooltip.add(
                    Component.literal("%.1fs cooking time".format(display.cookingTimeSeconds))
                        .withStyle(ChatFormatting.GRAY)
                )
            }

        var nextOutputY = SLOT_Y + 26
        if (display.byproductChance > 0f) {
            builder.addOutputSlot(OUTPUT_X, nextOutputY)
                .add(display.byproduct)
                .addRichTooltipCallback { _, tooltip ->
                    tooltip.add(
                        Component.literal("${display.byproductChancePercent} chance to drop")
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            nextOutputY += 26
        }

        if (display.hasBonusResult) {
            builder.addOutputSlot(OUTPUT_X, nextOutputY)
                .add(display.outputTemplate)
                .addRichTooltipCallback { _, tooltip ->
                    tooltip.add(
                        Component.literal("${display.bonusResultChancePercent} chance for a second")
                            .withStyle(ChatFormatting.GRAY)
                    )
                    if (display.bonusRequiresLava) {
                        tooltip.add(
                            Component.literal("Requires lava in the tank")
                                .withStyle(ChatFormatting.AQUA)
                        )
                    }
                }
        }

        builder.addInputSlot(FUEL_X, FUEL_Y)
            .addItemStacks(listOf(ItemStack(Items.COAL), ItemStack(Items.CHARCOAL)))

        builder.addInputSlot(FUEL_X + 25, FUEL_Y)
            .add(Items.LAVA_BUCKET)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(
                    Component.literal("Fills the lava tank for 4× speed")
                        .withStyle(ChatFormatting.GRAY)
                )
            }
    }

    override fun createRecipeExtras(
        builder: IRecipeExtrasBuilder,
        display: FoundryRecipeDisplay,
        focuses: IFocusGroup,
    ) {
        builder.addAnimatedRecipeArrow(display.cookingTime)
            .setPosition(ARROW_X, ARROW_Y)

        builder.addText(
            listOf<FormattedText>(
                Component.literal("Fuels:").withStyle(ChatFormatting.DARK_GRAY)
            ),
            /* maxWidth = */ 50,
            /* maxHeight = */ 9,
        ).setPosition(FUEL_X, FUEL_Y - 11)
    }

    override fun draw(
        display: FoundryRecipeDisplay,
        recipeSlotsView: IRecipeSlotsView,
        graphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double,
    ) {
        // All display is handled via slot tooltips and text/arrow widgets
    }

    private companion object {
        const val WIDTH  = 160
        const val HEIGHT = 110

        // Top row: input → arrow → output / byproduct
        const val SLOT_Y   = 5
        const val INPUT_X  = 10
        const val ARROW_X  = 40
        const val ARROW_Y  = 8
        const val OUTPUT_X = 110

        // Bottom row: fuel items
        const val FUEL_X = 5
        const val FUEL_Y = 83
    }
}
