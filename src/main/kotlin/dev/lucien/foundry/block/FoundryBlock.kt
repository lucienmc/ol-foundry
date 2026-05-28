package dev.lucien.foundry.block

import com.mojang.serialization.MapCodec
import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.registry.ModBlockEntities
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult

class FoundryBlock(properties: BlockBehaviour.Properties) : BaseEntityBlock(properties) {

    override fun codec(): MapCodec<out BaseEntityBlock> = simpleCodec(::FoundryBlock)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        FoundryBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    /** Right-click with lava bucket → fill the tank; anything else → open GUI. */
    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult
    ): InteractionResult {
        // In 26.1.2, PASS from useItemOn does NOT fall through to useWithoutItem,
        // so we delegate explicitly for non-bucket items.
        if (!stack.`is`(Items.LAVA_BUCKET)) return useWithoutItem(state, level, pos, player, hit)
        if (level.isClientSide) return InteractionResult.SUCCESS
        val entity = level.getBlockEntity(pos) as? FoundryBlockEntity
            ?: return InteractionResult.PASS
        val lavaVariant = FluidVariant.of(Fluids.LAVA)
        val space = FoundryBlockEntity.LAVA_CAPACITY - entity.fluidStorage.amount
        // Return SUCCESS (not PASS) when full so the bucket isn't used as an item
        if (space < FluidConstants.BUCKET) return InteractionResult.SUCCESS
        Transaction.openOuter().use { tx ->
            entity.fluidStorage.insert(lavaVariant, FluidConstants.BUCKET, tx)
            tx.commit()
        }
        if (!player.isCreative) {
            stack.shrink(1)
            player.addItem(ItemStack(Items.BUCKET))
        }
        return InteractionResult.SUCCESS
    }

    /** Right-click without item → open Foundry GUI */
    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide && player is ServerPlayer) {
            val entity = level.getBlockEntity(pos)
            if (entity is FoundryBlockEntity) {
                player.openMenu(entity)
            }
        }
        return InteractionResult.SUCCESS
    }

    /** Register the per-tick smelting logic */
    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = createTickerHelper(
        type, ModBlockEntities.FOUNDRY
    ) { lvl, pos, st, entity -> FoundryBlockEntity.tick(lvl, pos, st, entity) }

    override fun affectNeighborsAfterRemoval(
        state: BlockState, world: ServerLevel, pos: BlockPos, movedByPiston: Boolean
    ) {
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is FoundryBlockEntity) {
            Containers.dropContents(world, pos, blockEntity)
        }

        super.affectNeighborsAfterRemoval(state, world, pos, movedByPiston)
    }
}
