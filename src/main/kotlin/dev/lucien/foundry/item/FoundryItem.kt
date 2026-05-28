package dev.lucien.foundry.item

import dev.lucien.foundry.block.entity.FoundryBlockEntity
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import java.util.function.Consumer

class FoundryItem(block: Block, properties: Properties) : BlockItem(block, properties) {

    @Suppress("OVERRIDE_DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipDisplay: TooltipDisplay,
        tooltip: Consumer<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag)

        // Check if this stack has lava data stored in the custom data component
        val customData = stack.get(DataComponents.CUSTOM_DATA)
        if (customData != null) {
            val tag = customData.copyTag()
            val mb = tag.getIntOr("LavaAmount", 0)
            if (mb > 0) {
                val lavaLine = Component.literal("Lava: ")
                    .append(Component.literal("$mb").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" / 4000 mB").withStyle(ChatFormatting.GRAY))
                tooltip.accept(lavaLine)
            }
        }
    }

    /**
     * When placing the block, restore the lava amount from the item's custom data component.
     */
    override fun place(context: BlockPlaceContext): InteractionResult {
        val result = super.place(context)
        if (result.consumesAction()) {
            // Block was placed successfully, now restore lava if any
            val customData = context.itemInHand.get(DataComponents.CUSTOM_DATA)
            if (customData != null) {
                val tag = customData.copyTag()
                val mb = tag.getIntOr("LavaAmount", 0)
                if (mb > 0) {
                    val blockEntity = context.level.getBlockEntity(context.clickedPos)
                    if (blockEntity is FoundryBlockEntity) {
                        // Restore the lava amount (convert mB back to droplets)
                        blockEntity.fluidStorage.amount = mb.toLong() * 81L
                        blockEntity.setChanged()
                    }
                }
            }
        }
        return result
    }
}
