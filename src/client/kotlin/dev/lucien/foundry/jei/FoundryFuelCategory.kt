package dev.lucien.foundry.jei

import dev.lucien.foundry.registry.ModBlocks
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.placement.HorizontalAlignment
import mezz.jei.api.gui.placement.VerticalAlignment
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.recipe.types.IRecipeType
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.world.item.ItemStack
import java.text.NumberFormat

class FoundryFuelCategory(guiHelper: IGuiHelper) : IRecipeCategory<FoundryFuelDisplay> {

    private val icon = guiHelper.createDrawableItemStack(ItemStack(ModBlocks.FOUNDRY))

    override fun getRecipeType(): IRecipeType<FoundryFuelDisplay> = FoundryJeiPlugin.FOUNDRY_FUEL_TYPE
    override fun getTitle(): Component = Component.literal("Foundry Fuels")
    override fun getWidth() = WIDTH
    override fun getHeight() = HEIGHT
    override fun getIcon() = icon

    override fun setRecipe(builder: IRecipeLayoutBuilder, display: FoundryFuelDisplay, focuses: IFocusGroup) {
        builder.addInputSlot(SLOT_X, SLOT_Y).setStandardSlotBackground().addItemStacks(display.fuels)
    }

    override fun createRecipeExtras(builder: IRecipeExtrasBuilder, display: FoundryFuelDisplay, focuses: IFocusGroup) {
        builder.addAnimatedRecipeFlame(display.burnTimeTicks ?: 200).setPosition(SLOT_X, 0)

        val lines = buildList<FormattedText> {
            when {
                display.burnTimeTicks != null -> add(smeltCountText(display.burnTimeTicks))
                display.note != null -> add(Component.literal(display.note))
            }
            if (display.speedMultiplier != 1.0)
                add(Component.literal("${speedText(display.speedMultiplier)}× smelting speed").withStyle(ChatFormatting.GOLD))
        }

        if (lines.isNotEmpty()) {
            builder.addText(lines, WIDTH - TEXT_X, HEIGHT)
                .setPosition(TEXT_X, 0)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setTextAlignment(VerticalAlignment.CENTER)
                .setColor(TEXT_COLOR)
        }
    }

    private companion object {
        const val WIDTH = 150
        const val HEIGHT = 34
        const val SLOT_X = 1
        // Slot top at HEIGHT/2 so the flame fills the upper half — the slot bottom clips 1px below HEIGHT,
        // intentional in JEI's vanilla fuel categories.
        const val SLOT_Y = HEIGHT / 2
        const val TEXT_X = 20  // 1px margin + 18px slot + 1px gap
        const val TEXT_COLOR = -8355712  // 0x808080, matches JEI's own fuel-category gray

        fun smeltCountText(burnTimeTicks: Int): Component {
            val items = burnTimeTicks / 200.0
            return if (items == 1.0) {
                Component.translatable("gui.jei.category.fuel.smeltCount.single")
            } else {
                val nf = NumberFormat.getNumberInstance().also { it.maximumFractionDigits = 2 }
                Component.translatable("gui.jei.category.fuel.smeltCount", nf.format(items))
            }
        }

        fun speedText(multiplier: Double): String =
            if (multiplier == multiplier.toLong().toDouble()) multiplier.toLong().toString()
            else multiplier.toString()
    }
}
