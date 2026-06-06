package dev.lucien.foundry.jei

import dev.lucien.foundry.registry.ModBlocks
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.recipe.types.IRecipeType
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.world.item.ItemStack

/** Heat-source style tab listing each Foundry fuel and its smelting-speed / burn-time efficiency. */
class FoundryFuelCategory(guiHelper: IGuiHelper) : IRecipeCategory<FoundryFuelDisplay> {

    private val icon = guiHelper.createDrawableItemStack(ItemStack(ModBlocks.FOUNDRY))

    override fun getRecipeType(): IRecipeType<FoundryFuelDisplay> = FoundryJeiPlugin.FOUNDRY_FUEL_TYPE
    override fun getTitle(): Component = Component.literal("Foundry Fuels")
    override fun getWidth() = WIDTH
    override fun getHeight() = HEIGHT
    override fun getIcon(): mezz.jei.api.gui.drawable.IDrawable = icon

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        display: FoundryFuelDisplay,
        focuses: IFocusGroup,
    ) {
        builder.addInputSlot(SLOT_X, SLOT_Y).addItemStacks(display.fuels)
    }

    override fun createRecipeExtras(
        builder: IRecipeExtrasBuilder,
        display: FoundryFuelDisplay,
        focuses: IFocusGroup,
    ) {
        val lines = buildList<FormattedText> {
            add(
                Component.literal("${speedText(display.speedMultiplier)}× smelting speed")
                    .withStyle(ChatFormatting.GOLD)
            )
            add(
                (display.burnTimeTicks
                    ?.let { Component.literal("Burns %.1fs".format(it / 20f)) }
                    ?: Component.literal("Vanilla burn time"))
                    .withStyle(ChatFormatting.GRAY)
            )
            display.note?.let { add(Component.literal(it).withStyle(ChatFormatting.AQUA)) }
        }
        builder.addText(lines, WIDTH - TEXT_X - 2, HEIGHT - 6).setPosition(TEXT_X, 4)
    }

    private companion object {
        const val WIDTH = 150
        const val HEIGHT = 34
        const val SLOT_X = 4
        const val SLOT_Y = 8
        const val TEXT_X = 28

        /** Trims a trailing `.0` so whole multipliers read as "2" not "2.0". */
        fun speedText(multiplier: Double): String =
            if (multiplier == multiplier.toLong().toDouble()) multiplier.toLong().toString()
            else multiplier.toString()
    }
}
