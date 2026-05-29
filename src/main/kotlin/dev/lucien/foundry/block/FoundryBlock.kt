package dev.lucien.foundry.block

import com.mojang.serialization.MapCodec
import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.block.entity.FoundryLavaTank
import dev.lucien.foundry.item.LavaStorageComponent
import dev.lucien.foundry.registry.ModBlockEntities
import dev.lucien.foundry.registry.ModDataComponents
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult

class FoundryBlock(properties: Properties) : AbstractFurnaceBlock(properties) {

    override fun codec(): MapCodec<out AbstractFurnaceBlock> = simpleCodec(::FoundryBlock)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        FoundryBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun openContainer(level: Level, pos: BlockPos, player: Player) {
        val entity = level.getBlockEntity(pos)
        if (entity is FoundryBlockEntity && player is ServerPlayer) {
            player.openMenu(entity)
        }
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!stack.`is`(Items.LAVA_BUCKET)) return useWithoutItem(state, level, pos, player, hit)
        if (level.isClientSide) return InteractionResult.SUCCESS
        val entity =
            level.getBlockEntity(pos) as? FoundryBlockEntity ?: return InteractionResult.PASS
        if (FoundryLavaTank.CAPACITY - entity.lava.storage.amount < FluidConstants.BUCKET) return InteractionResult.SUCCESS
        Transaction.openOuter().use { tx ->
            entity.lava.storage.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx)
            tx.commit()
        }
        if (!player.isCreative) {
            stack.shrink(1)
            player.addItem(ItemStack(Items.BUCKET))
        }
        return InteractionResult.SUCCESS
    }

    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = createTickerHelper(
        type, ModBlockEntities.FOUNDRY
    ) { lvl, _, _, entity -> FoundryBlockEntity.tick(lvl, entity) }

    override fun affectNeighborsAfterRemoval(
        state: BlockState, world: ServerLevel, pos: BlockPos, movedByPiston: Boolean
    ) {
        val entity = world.getBlockEntity(pos)
        if (entity is FoundryBlockEntity) Containers.dropContents(world, pos, entity)
        super.affectNeighborsAfterRemoval(state, world, pos, movedByPiston)
    }

    // In creative mode loot tables are skipped — drop the item manually so lava isn't silently lost.
    override fun playerWillDestroy(
        level: Level, pos: BlockPos, state: BlockState, player: Player
    ): BlockState {
        if (!level.isClientSide && player.isCreative) {
            val entity = level.getBlockEntity(pos) as? FoundryBlockEntity
            if (entity != null && entity.lava.hasLava) {
                val stack = ItemStack(asItem())
                stack.set(ModDataComponents.LAVA_STORAGE, LavaStorageComponent(entity.lava.mb))
                Containers.dropItemStack(
                    level,
                    pos.x.toDouble() + 0.5,
                    pos.y.toDouble() + 0.5,
                    pos.z.toDouble() + 0.5,
                    stack
                )
            }
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        if (!state.getValue(LIT)) return

        val x = pos.x + 0.5
        val y = pos.y.toDouble()
        val z = pos.z + 0.5

        if (random.nextDouble() < 0.1) {
            level.playLocalSound(
                x, y, z, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f, false
            )
        }

        level.addParticle(ParticleTypes.SMOKE, x, y + 1.8, z, 0.0, 0.0, 0.0)

        if (random.nextDouble() < 0.3) {
            val facing = state.getValue(FACING)
            val dx = facing.stepX * 0.52
            val dz = facing.stepZ * 0.52
            val perpX = facing.clockWise.stepX.toDouble()
            val perpZ = facing.clockWise.stepZ.toDouble()
            repeat(2) {
                val offY = y + 0.35 + random.nextDouble() * 0.2
                val side = (random.nextDouble() - 0.5) * 0.5
                level.addParticle(
                    ParticleTypes.FLAME,
                    x + dx + perpX * side,
                    offY,
                    z + dz + perpZ * side,
                    0.0,
                    0.0,
                    0.0
                )
                level.addParticle(
                    ParticleTypes.SMOKE,
                    x + dx + perpX * side,
                    offY,
                    z + dz + perpZ * side,
                    0.0,
                    0.0,
                    0.0
                )
            }
        }
    }
}
