package dev.lucien.foundry.jei

import dev.lucien.foundry.registry.ModBlocks
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.types.IRecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

class FoundryRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<FoundryRecipeDisplay> {

    private val icon = guiHelper.createDrawableItemStack(ItemStack(ModBlocks.FOUNDRY))

    override fun getRecipeType(): IRecipeType<FoundryRecipeDisplay> =
        FoundryJeiPlugin.FOUNDRY_RECIPE_TYPE

    override fun getTitle(): Component = Component.literal("Foundry Smelting")

    override fun getWidth() = WIDTH
    override fun getHeight() = HEIGHT

    override fun getIcon(): mezz.jei.api.gui.drawable.IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        display: FoundryRecipeDisplay,
        focuses: IFocusGroup,
    ) {
        builder.addInputSlot(INPUT_X, SLOT_Y)
            .add(display.ingredient)

        val primary = builder.addOutputSlot(OUTPUT_X, SLOT_Y)
        if (display.isPooled) primary.addItemStacks(display.resultStacks) else primary.add(display.outputTemplate)
        primary.addRichTooltipCallback { _, tooltip ->
            tooltip.add(
                Component.literal("%.1f XP".format(display.experience)).withStyle(ChatFormatting.GOLD)
            )
            tooltip.add(
                Component.literal("%.1fs cooking time".format(display.cookingTimeSeconds))
                    .withStyle(ChatFormatting.GRAY)
            )
            if (display.isPooled) {
                display.resultOdds.forEach { (stack, pct) ->
                    tooltip.add(
                        Component.literal("$pct  ${stack.hoverName.string}")
                            .withStyle(ChatFormatting.DARK_GRAY)
                    )
                }
                if (display.bonusRequiresLava) {
                    tooltip.add(
                        Component.literal("Lava: +1 extra nugget").withStyle(ChatFormatting.GOLD)
                    )
                }
            }
        }

        var nextOutputY = SLOT_Y + 26
        if (display.hasByproduct) {
            builder.addOutputSlot(OUTPUT_X, nextOutputY)
                .add(display.byproductStack)
                .addRichTooltipCallback { _, tooltip ->
                    tooltip.add(
                        Component.literal("Slag byproduct").withStyle(ChatFormatting.GRAY)
                    )
                    if (display.byproductGuaranteed >= 1 && display.byproductExtraChance > 0f) {
                        tooltip.add(
                            Component.literal("+${display.byproductExtraPercent} chance for one more")
                                .withStyle(ChatFormatting.DARK_GRAY)
                        )
                    } else if (display.byproductGuaranteed < 1) {
                        tooltip.add(
                            Component.literal("${display.byproductExtraPercent} chance to drop")
                                .withStyle(ChatFormatting.DARK_GRAY)
                        )
                    }
                }
            nextOutputY += 26
        }

        if (display.hasBonusResult && !display.isPooled) {
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
    }

    override fun createRecipeExtras(
        builder: IRecipeExtrasBuilder,
        display: FoundryRecipeDisplay,
        focuses: IFocusGroup,
    ) {
        builder.addAnimatedRecipeArrow(display.cookingTime)
            .setPosition(ARROW_X, ARROW_Y)
    }

    private companion object {
        const val WIDTH = 160
        const val HEIGHT = 80

        // input → arrow → output / byproduct (stacked on the right)
        const val SLOT_Y = 5
        const val INPUT_X = 10
        const val ARROW_X = 40
        const val ARROW_Y = 8
        const val OUTPUT_X = 110
    }
}
