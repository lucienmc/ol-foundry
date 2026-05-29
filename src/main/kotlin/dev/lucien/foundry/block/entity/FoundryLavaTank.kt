package dev.lucien.foundry.block.entity

import dev.lucien.foundry.block.entity.FoundryLavaTank.Companion.DRAIN_PER_TICK
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class FoundryLavaTank(private val onChanged: () -> Unit) {

    val storage: SingleVariantStorage<FluidVariant> =
        object : SingleVariantStorage<FluidVariant>() {
            override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
            override fun getCapacity(variant: FluidVariant): Long = CAPACITY
            override fun canInsert(variant: FluidVariant): Boolean = variant.fluid == Fluids.LAVA
            override fun canExtract(variant: FluidVariant): Boolean = true
            override fun onFinalCommit() = onChanged()
        }

    val hasLava: Boolean get() = storage.amount > 0L
    val percent: Int get() = if (storage.amount > 0L) ((storage.amount * 100L) / CAPACITY).toInt() else 0
    val mb: Int get() = (storage.amount / DROPLETS_PER_MB).toInt()

    /** Fills the tank to [mb] milli-buckets of lava (clamped to capacity). Used when placing a stored block. */
    fun fillFromMb(mb: Int) {
        if (mb <= 0) return
        storage.variant = FluidVariant.of(Fluids.LAVA)
        storage.amount = (mb * DROPLETS_PER_MB).coerceAtMost(CAPACITY)
        onChanged()
    }

    fun tryConsumeBucket(bucketSlot: ItemStack): ItemStack? {
        if (bucketSlot.isEmpty || !bucketSlot.`is`(Items.LAVA_BUCKET)) return null
        if (CAPACITY - storage.amount < FluidConstants.BUCKET) return null
        Transaction.openOuter().use { tx ->
            storage.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx)
            tx.commit()
        }
        return ItemStack(Items.BUCKET)
    }

    fun drainForBoost() {
        Transaction.openOuter().use { tx ->
            storage.extract(FluidVariant.of(Fluids.LAVA), DRAIN_PER_TICK, tx)
            tx.commit()
        }
    }

    fun save(output: ValueOutput) {
        output.putLong("lava_amount", storage.amount)
    }

    fun load(input: ValueInput) {
        val saved = input.getLongOr("lava_amount", 0L)
        if (saved > 0L) {
            storage.variant = FluidVariant.of(Fluids.LAVA)
            storage.amount = saved.coerceAtMost(CAPACITY)
        }
    }

    companion object {
        const val CAPACITY: Long = FluidConstants.BUCKET * 4
        const val DRAIN_PER_TICK: Long = FluidConstants.BUCKET / 1600
        const val DROPLETS_PER_MB: Long = FluidConstants.BUCKET / 1000

        /** Tank capacity expressed in milli-buckets (for display: "x / 4000 mB"). */
        val CAPACITY_MB: Int = (CAPACITY / DROPLETS_PER_MB).toInt()
    }
}
