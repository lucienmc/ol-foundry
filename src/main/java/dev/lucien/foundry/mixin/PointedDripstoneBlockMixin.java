package dev.lucien.foundry.mixin;

import dev.lucien.foundry.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockMixin {

    /**
     * canDripThrough returns false for solid-rendering blocks, which stops the upward
     * fluid search before it reaches the lava above slag bricks. Override for slag bricks
     * so the search can continue through to the lava source.
     *
     * Static handler is required for a private static target method.
     */
    @Inject(method = "canDripThrough", at = @At("RETURN"), cancellable = true)
    private static void letSlagBricksDripThrough(
            BlockGetter world, BlockPos pos, BlockState state,
            CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValue() && state.is(ModBlocks.INSTANCE.getSLAG_BRICKS())) {
            ci.setReturnValue(true);
        }
    }

    /**
     * After the vanilla drip attempt, make two additional maybeTransferFluid calls when
     * slag bricks sits directly above the stalactite tip. Each extra call carries the same
     * ~17.6% probability as the vanilla one, giving ~3x the expected fill rate.
     *
     * Only the downward-pointing tip drips; all other dripstone blocks are skipped cheaply.
     */
    @Inject(method = "randomTick", at = @At("RETURN"))
    private void accelerateSlagBricksDrip(
            BlockState blockState, ServerLevel level, BlockPos pos, RandomSource random,
            CallbackInfo ci) {
        if (blockState.getValue(PointedDripstoneBlock.THICKNESS) != DripstoneThickness.TIP) return;
        if (blockState.getValue(PointedDripstoneBlock.TIP_DIRECTION) != Direction.DOWN) return;
        if (!level.getBlockState(pos.above()).is(ModBlocks.INSTANCE.getSLAG_BRICKS())) return;
        PointedDripstoneBlock.maybeTransferFluid(blockState, level, pos, random.nextFloat());
        PointedDripstoneBlock.maybeTransferFluid(blockState, level, pos, random.nextFloat());
    }
}
