package dev.lucien.foundry.block

import com.mojang.serialization.MapCodec
import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.registry.ModBlockEntities
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult

class FoundryBlock(properties: BlockBehaviour.Properties) : BaseEntityBlock(properties) {

    companion object {
        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        builder.add(FACING)
    }

    /** Face the player who placed the block (same convention as vanilla furnace). */
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun codec(): MapCodec<out BaseEntityBlock> = simpleCodec(::FoundryBlock)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        FoundryBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    /** Right-click with lava bucket → fill the tank; anything else → open GUI. */
    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult
    ): InteractionResult {
        if (!stack.`is`(Items.LAVA_BUCKET)) return useWithoutItem(state, level, pos, player, hit)
        if (level.isClientSide) return InteractionResult.SUCCESS
        val entity = level.getBlockEntity(pos) as? FoundryBlockEntity
            ?: return InteractionResult.PASS
        val lavaVariant = FluidVariant.of(Fluids.LAVA)
        val space = FoundryBlockEntity.LAVA_CAPACITY - entity.fluidStorage.amount
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

    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = createTickerHelper(
        type, ModBlockEntities.FOUNDRY
    ) { lvl, pos, st, entity -> FoundryBlockEntity.tick(lvl, pos, st, entity) }

    /**
     * Drop inventory contents on block removal (pistons, explosions, etc.).
     * Also called after player breaks in survival.
     */
    override fun affectNeighborsAfterRemoval(
        state: BlockState, world: ServerLevel, pos: BlockPos, movedByPiston: Boolean
    ) {
        val entity = world.getBlockEntity(pos)
        if (entity is FoundryBlockEntity) {
            Containers.dropContents(world, pos, entity)
        }
        super.affectNeighborsAfterRemoval(state, world, pos, movedByPiston)
    }

    /**
     * In creative mode, Minecraft skips loot tables so the block never drops.
     * We override this to forcibly drop the foundry item when the tank has lava —
     * otherwise the player would lose the fluid with no recourse.
     */
    override fun playerWillDestroy(
        level: Level, pos: BlockPos, state: BlockState, player: Player
    ): BlockState {
        if (!level.isClientSide && player.isCreative) {
            val entity = level.getBlockEntity(pos) as? FoundryBlockEntity
            if (entity != null && entity.fluidStorage.amount > 0L) {
                val stack = ItemStack(asItem())
                // Store the lava amount in custom data component so it shows in the item tooltip
                val mb = (entity.fluidStorage.amount / 81L).toInt()
                val tag = net.minecraft.nbt.CompoundTag().apply { putInt("LavaAmount", mb) }
                stack.set(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag)
                )
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
}
