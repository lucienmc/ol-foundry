package dev.lucien.foundry.item

import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.registry.ModDataComponents
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block

class FoundryItem(block: Block, properties: Properties) : BlockItem(block, properties) {

    /**
     * When placing the block, restore any lava stored in the [LavaStorageComponent].
     */
    override fun place(context: BlockPlaceContext): InteractionResult {
        val result = super.place(context)
        if (!result.consumesAction()) return result

        val component = context.itemInHand[ModDataComponents.LAVA_STORAGE] ?: return result
        if (component.mb <= 0) return result

        val blockEntity = context.level.getBlockEntity(context.clickedPos)
        if (blockEntity is FoundryBlockEntity) {
            blockEntity.lava.storage.amount = component.mb.toLong() * 81L
            blockEntity.setChanged()
        }

        return result
    }
}
