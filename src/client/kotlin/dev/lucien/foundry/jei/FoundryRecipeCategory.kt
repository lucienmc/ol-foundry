package dev.lucien.foundry.jei

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.menu.FoundryMenu
import dev.lucien.foundry.registry.ModBlocks
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.recipe.types.IRecipeType
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

class FoundryRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<FoundryRecipeDisplay> {

    private val icon = guiHelper.createDrawableItemStack(ItemStack(ModBlocks.FOUNDRY))

    // Cropped from the real GUI sheet so the JEI page matches the block's actual layout.
    private val background: IDrawable =
        guiHelper.createDrawable(CONTAINER_TEXTURE, CROP_U, CROP_V, WIDTH, HEIGHT)

    // All valid furnace fuels, computed once on first recipe render.
    private val allFuels: List<ItemStack> by lazy {
        val level = Minecraft.getInstance().level ?: return@lazy emptyList()
        val fuelValues = level.fuelValues()
        BuiltInRegistries.ITEM
            .filter { fuelValues.burnDuration(ItemStack(it)) > 0 }
            .map { ItemStack(it) }
    }

    override fun getRecipeType(): IRecipeType<FoundryRecipeDisplay> = FoundryJeiPlugin.FOUNDRY_RECIPE_TYPE
    override fun getTitle(): Component = Component.literal("Foundry Smelting")
    override fun getWidth() = WIDTH
    override fun getHeight() = HEIGHT
    override fun getIcon(): IDrawable = icon

    override fun draw(
        recipe: FoundryRecipeDisplay,
        recipeSlotsView: IRecipeSlotsView,
        graphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double,
    ) {
        background.draw(graphics)
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, display: FoundryRecipeDisplay, focuses: IFocusGroup) {
        // No slot backgrounds — the cropped texture already renders them.
        builder.addInputSlot(INPUT_X, INPUT_Y).add(display.ingredient)

        val primary = builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
        if (display.isPooled) primary.addItemStacks(display.resultStacks) else primary.add(display.outputTemplate)
        primary.addRichTooltipCallback { _, tooltip ->
            tooltip.add(Component.literal("%.1f XP".format(display.experience)).withStyle(ChatFormatting.GOLD))
            tooltip.add(Component.literal("%.1fs cooking time".format(display.cookingTimeSeconds)).withStyle(ChatFormatting.GRAY))
            if (display.isPooled) {
                display.resultOdds.forEach { (stack, pct) ->
                    tooltip.add(Component.literal("$pct  ${stack.hoverName.string}").withStyle(ChatFormatting.DARK_GRAY))
                }
                if (display.bonusRequiresLava)
                    tooltip.add(Component.literal("Lava: +1 extra nugget").withStyle(ChatFormatting.GOLD))
            }
        }

        if (display.hasByproduct) {
            builder.addOutputSlot(BYPRODUCT_X, BYPRODUCT_Y)
                .add(display.byproductStack)
                .addRichTooltipCallback { _, tooltip ->
                    tooltip.add(Component.literal("Slag byproduct").withStyle(ChatFormatting.GRAY))
                    if (display.byproductGuaranteed >= 1 && display.byproductExtraChance > 0f)
                        tooltip.add(Component.literal("+${display.byproductExtraPercent} chance for one more").withStyle(ChatFormatting.DARK_GRAY))
                    else if (display.byproductGuaranteed < 1)
                        tooltip.add(Component.literal("${display.byproductExtraPercent} chance to drop").withStyle(ChatFormatting.DARK_GRAY))
                }
        }

        if (display.hasBonusResult && !display.isPooled) {
            builder.addOutputSlot(BONUS_X, BONUS_Y)
                .add(display.outputTemplate)
                .addRichTooltipCallback { _, tooltip ->
                    tooltip.add(Component.literal("${display.bonusResultChancePercent} chance for a second").withStyle(ChatFormatting.GRAY))
                    if (display.bonusRequiresLava)
                        tooltip.add(Component.literal("Requires lava in the tank").withStyle(ChatFormatting.AQUA))
                }
        }

        // RENDER_ONLY = drawn and hoverable, ignored by the recipe-transfer handler.
        if (allFuels.isNotEmpty())
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, FUEL_X, FUEL_Y).addItemStacks(allFuels)
    }

    override fun createRecipeExtras(builder: IRecipeExtrasBuilder, display: FoundryRecipeDisplay, focuses: IFocusGroup) {
        builder.addAnimatedRecipeFlame(display.cookingTime).setPosition(FLAME_X, FLAME_Y)
        builder.addAnimatedRecipeArrow(display.cookingTime).setPosition(ARROW_X, ARROW_Y)
    }

    private companion object {
        val CONTAINER_TEXTURE: Identifier = Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "textures/gui/container/foundry.png")

        // Crop covering input → fuel → flame → arrow → outputs/byproduct.
        const val CROP_U = 22
        const val CROP_V = 14
        const val WIDTH = 113
        const val HEIGHT = 58

        // Item positions in crop space: outer slot corner − crop offset + 1.
        // Flame/arrow: groove top-left − crop offset.
        const val INPUT_X = FoundryMenu.INPUT_X - CROP_U + 1
        const val INPUT_Y = FoundryMenu.INPUT_Y - CROP_V + 1
        const val FUEL_X = FoundryMenu.FUEL_X - CROP_U + 1
        const val FUEL_Y = FoundryMenu.FUEL_Y - CROP_V + 1
        const val OUTPUT_X = FoundryMenu.OUTPUT1_X - CROP_U + 1
        const val OUTPUT_Y = FoundryMenu.OUTPUT1_Y - CROP_V + 1
        const val BONUS_X = FoundryMenu.OUTPUT2_X - CROP_U + 1
        const val BONUS_Y = FoundryMenu.OUTPUT2_Y - CROP_V + 1
        const val BYPRODUCT_X = FoundryMenu.BYPRODUCT_X - CROP_U + 1
        const val BYPRODUCT_Y = FoundryMenu.BYPRODUCT_Y - CROP_V + 1
        const val FLAME_X = FoundryMenu.FLAME_X - CROP_U
        const val FLAME_Y = FoundryMenu.FLAME_Y - CROP_V
        const val ARROW_X = FoundryMenu.ARROW_X - CROP_U
        const val ARROW_Y = FoundryMenu.ARROW_Y - CROP_V
    }
}
