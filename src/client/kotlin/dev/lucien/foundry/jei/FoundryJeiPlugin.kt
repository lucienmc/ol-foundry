package dev.lucien.foundry.jei

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.config.FoundryConfigManager
import dev.lucien.foundry.menu.FoundryMenu
import dev.lucien.foundry.registry.ModBlocks
import dev.lucien.foundry.registry.ModItems
import dev.lucien.foundry.registry.ModMenuTypes
import dev.lucien.foundry.registry.ModRecipes
import dev.lucien.foundry.screen.FoundryScreen
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.types.IRecipeType
import mezz.jei.api.registration.*
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

@JeiPlugin
class FoundryJeiPlugin : mezz.jei.api.IModPlugin {

    override fun getPluginUid(): Identifier =
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "jei_plugin")

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        val gui = registration.jeiHelpers.guiHelper
        registration.addRecipeCategories(
            FoundryRecipeCategory(gui),
            FoundryFuelCategory(gui),
        )
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addCraftingStation(FOUNDRY_RECIPE_TYPE, ModBlocks.FOUNDRY)
        registration.addCraftingStation(FOUNDRY_FUEL_TYPE, ModBlocks.FOUNDRY)
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val level = Minecraft.getInstance().level ?: return
        val recipes =
            level.recipeAccess().synchronizedRecipes.getAllOfType(ModRecipes.FOUNDRY_RECIPE_TYPE)
                .map { FoundryRecipeDisplay(it.value()) }
        registration.addRecipes(FOUNDRY_RECIPE_TYPE, recipes)
        registration.addRecipes(FOUNDRY_FUEL_TYPE, fuelDisplays(level))
    }

    override fun registerGuiHandlers(reg: IGuiHandlerRegistration) {
        reg.addRecipeClickArea(
            FoundryScreen::class.java,
            FoundryMenu.ARROW_X,
            FoundryMenu.ARROW_Y,
            FoundryMenu.ARROW_W,
            FoundryMenu.ARROW_H,
            FOUNDRY_RECIPE_TYPE
        )
    }

    override fun registerRecipeTransferHandlers(reg: IRecipeTransferRegistration) {
        reg.addRecipeTransferHandler(
            FoundryMenu::class.java, ModMenuTypes.FOUNDRY,
            FOUNDRY_RECIPE_TYPE,
            FoundryBlockEntity.INPUT_SLOT, 1,
            FoundryMenu.PLAYER_INV_SLOT_START, 36
        )
    }

    private fun fuelDisplays(level: Level): List<FoundryFuelDisplay> {
        val cfg = FoundryConfigManager.config

        // Items shown in their own tiered entries — excluded from the generic "any other fuel" pile.
        val highlighted = setOf(
            Items.COAL, Items.CHARCOAL, Items.BLAZE_ROD, Items.MAGMA_CREAM, ModItems.SLAG, Items.LAVA_BUCKET
        )
        val fuelValues = level.fuelValues()
        val otherFuels = BuiltInRegistries.ITEM
            .filter { it !in highlighted && fuelValues.burnDuration(ItemStack(it)) > 0 }
            .map { ItemStack(it) }

        return listOf(
            FoundryFuelDisplay(
                listOf(ItemStack(Items.COAL), ItemStack(Items.CHARCOAL)),
                cfg.coalFuelSpeedMultiplier, null,
            ),
            FoundryFuelDisplay(
                listOf(ItemStack(Items.MAGMA_CREAM)),
                cfg.magmaCreamFuelSpeedMultiplier, cfg.magmaCreamBurnTime,
            ),
            FoundryFuelDisplay(
                listOf(ItemStack(Items.BLAZE_ROD)),
                cfg.blazeRodFuelSpeedMultiplier, null,
            ),
            FoundryFuelDisplay(
                listOf(ItemStack(ModItems.SLAG)),
                cfg.baseFuelSpeedMultiplier, cfg.slagBurnTime,
            ),
            FoundryFuelDisplay(
                otherFuels,
                cfg.baseFuelSpeedMultiplier, null,
                note = "Any other furnace fuel",
            ),
            FoundryFuelDisplay(
                listOf(ItemStack(Items.LAVA_BUCKET)),
                cfg.lavaSpeedMultiplier, null,
                note = "Boosts the active fuel while the tank holds lava",
            ),
        )
    }

    companion object {
        val FOUNDRY_RECIPE_TYPE: IRecipeType<FoundryRecipeDisplay> =
            IRecipeType.create(Foundry.MOD_ID, "foundry", FoundryRecipeDisplay::class.java)
        val FOUNDRY_FUEL_TYPE: IRecipeType<FoundryFuelDisplay> =
            IRecipeType.create(Foundry.MOD_ID, "foundry_fuel", FoundryFuelDisplay::class.java)
    }
}
