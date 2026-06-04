package dev.lucien.foundry.registry

import dev.lucien.foundry.block.entity.FoundryBlockEntity
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.Items

object ModEvents {

    fun init() {
        registerLavaTap()
    }

    /**
     * Shift + right-click a Foundry with an empty bucket → tap one bucket of lava back out.
     *
     * Not in `FoundryBlock.useItemOn`: sneaking while holding a non-empty item makes the vanilla
     * pipeline skip block use, so the block hook never fires. `UseBlockCallback` runs before that skip.
     */
    private fun registerLavaTap() {
        UseBlockCallback.EVENT.register { player, level, hand, hit ->
            val stack = player.getItemInHand(hand)
            if (!stack.`is`(Items.BUCKET) || !player.isSecondaryUseActive) {
                return@register InteractionResult.PASS
            }
            val foundry = level.getBlockEntity(hit.blockPos) as? FoundryBlockEntity
                ?: return@register InteractionResult.PASS
            if (level.isClientSide) return@register InteractionResult.SUCCESS
            if (foundry.lava.tryRemoveBucket()) {
                player.setItemInHand(
                    hand,
                    ItemUtils.createFilledResult(stack, player, ItemStack(Items.LAVA_BUCKET)),
                )
            }
            InteractionResult.SUCCESS
        }
    }
}
