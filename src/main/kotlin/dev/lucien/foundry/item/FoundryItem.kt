package dev.lucien.foundry.item

import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.registry.ModDataComponents
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block

class FoundryItem(block: Block, properties: Properties) : BlockItem(block, properties) {

    /** When placing the block, restore any lava carried in the [LavaStorageComponent]. */
    override fun place(context: BlockPlaceContext): InteractionResult {
        val result = super.place(context)
        if (!result.consumesAction()) return result

        val component = context.itemInHand[ModDataComponents.LAVA_STORAGE] ?: return result
        val blockEntity = context.level.getBlockEntity(context.clickedPos)
        if (blockEntity is FoundryBlockEntity) blockEntity.lava.fillFromMb(component.mb)

        return result
    }
}
