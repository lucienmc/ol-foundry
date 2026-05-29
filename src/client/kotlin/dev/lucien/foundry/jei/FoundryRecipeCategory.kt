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
import net.minecraft.network.chat.FormattedText
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

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

        builder.addInputSlot(FUEL_X, FUEL_Y)
            .addItemStacks(listOf(ItemStack(Items.COAL), ItemStack(Items.CHARCOAL)))
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(
                    Component.literal("1.5× smelting speed").withStyle(ChatFormatting.GRAY)
                )
            }

        builder.addInputSlot(FUEL_X + 25, FUEL_Y)
            .add(Items.BLAZE_ROD)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(
                    Component.literal("3× smelting speed").withStyle(ChatFormatting.GOLD)
                )
            }

        builder.addInputSlot(FUEL_X + 50, FUEL_Y)
            .add(Items.LAVA_BUCKET)
            .addRichTooltipCallback { _, tooltip ->
                tooltip.add(
                    Component.literal("Doubles smelting speed").withStyle(ChatFormatting.GRAY)
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
