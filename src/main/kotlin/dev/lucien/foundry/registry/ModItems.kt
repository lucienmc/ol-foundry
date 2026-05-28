package dev.lucien.foundry.registry

import dev.lucien.foundry.Foundry
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

object ModItems {

    /** The raw slag byproduct from smelting ores. */
    val SLAG: Item = register("slag", Item(Item.Properties().setId(itemKey("slag"))))

    /** Block items (registered after blocks exist) */
    val FOUNDRY_ITEM: BlockItem =
        register("foundry", BlockItem(ModBlocks.FOUNDRY, Item.Properties().setId(itemKey("foundry"))))
    val SLAG_BRICKS_ITEM: BlockItem =
        register("slag_bricks", BlockItem(ModBlocks.SLAG_BRICKS, Item.Properties().setId(itemKey("slag_bricks"))))

    val CUSTOM_CREATIVE_TAB_KEY: ResourceKey<CreativeModeTab> =
        ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "creative_tab")
        )
    val CUSTOM_CREATIVE_TAB: CreativeModeTab = FabricCreativeModeTab.builder()
        .icon { ItemStack(FOUNDRY_ITEM) }
        .title(Component.translatable("creativeTab.foundry"))
        .displayItems { _: ItemDisplayParameters?, output: CreativeModeTab.Output ->
            output.accept(FOUNDRY_ITEM)
            output.accept(SLAG)
            output.accept(SLAG_BRICKS_ITEM)
        }
        .build()

    private fun itemKey(name: String): ResourceKey<Item> = ResourceKey.create(
        BuiltInRegistries.ITEM.key(),
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, name)
    )

    private fun <T : Item> register(name: String, item: T): T {
        return Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(Foundry.MOD_ID, name),
            item
        )
    }

    fun init() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            CUSTOM_CREATIVE_TAB_KEY,
            CUSTOM_CREATIVE_TAB
        )

        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES[SLAG] = 0.3f
    }
}
