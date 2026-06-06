package dev.lucien.foundry.block.entity

import dev.lucien.foundry.block.entity.FoundryLavaTank.Companion.DRAIN_PER_TICK
import dev.lucien.foundry.config.FoundryConfigManager
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class FoundryLavaTank(private val onChanged: () -> Unit) {

    val storage: SingleVariantStorage<FluidVariant> =
        object : SingleVariantStorage<FluidVariant>() {
            override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
            // NB: must be `capacityDroplets`, not a name that collides with the inherited
            // no-arg getCapacity() — otherwise this recurses into itself (StackOverflowError).
            override fun getCapacity(variant: FluidVariant): Long = capacityDroplets
            override fun canInsert(variant: FluidVariant): Boolean = variant.fluid == Fluids.LAVA
            override fun canExtract(variant: FluidVariant): Boolean = true
            override fun onFinalCommit() = onChanged()
        }

    val hasLava: Boolean get() = storage.amount > 0L
    val percent: Int
        get() = if (storage.amount > 0L) ((storage.amount * 100L) / capacityDroplets).toInt() else 0
    val mb: Int get() = (storage.amount / DROPLETS_PER_MB).toInt()

    /** Fills the tank to [mb] milli-buckets of lava (clamped to capacity). Used when placing a stored block. */
    fun fillFromMb(mb: Int) {
        if (mb <= 0) return
        storage.variant = FluidVariant.of(Fluids.LAVA)
        storage.amount = (mb * DROPLETS_PER_MB).coerceAtMost(capacityDroplets)
        onChanged()
    }

    /** Inserts one bucket of lava if the tank has room; returns whether it fit. */
    fun tryAddBucket(): Boolean {
        if (capacityDroplets - storage.amount < FluidConstants.BUCKET) return false
        Transaction.openOuter().use { tx ->
            storage.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx)
            tx.commit()
        }
        return true
    }

    /** Drains one full bucket of lava out of the tank if it holds at least that much; returns success. */
    fun tryRemoveBucket(): Boolean {
        if (storage.amount < FluidConstants.BUCKET) return false
        Transaction.openOuter().use { tx ->
            val extracted = storage.extract(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx)
            if (extracted == FluidConstants.BUCKET) {
                tx.commit()
                return true
            }
        }
        return false
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
            storage.amount = saved.coerceAtMost(capacityDroplets)
        }
    }

    companion object {
        const val DRAIN_PER_TICK: Long = FluidConstants.BUCKET / 1600
        const val DROPLETS_PER_MB: Long = FluidConstants.BUCKET / 1000

        /** Tank capacity in droplets, derived from the configured bucket count. */
        val capacityDroplets: Long
            get() = FoundryConfigManager.config.lavaTankCapacityBuckets.toLong() * FluidConstants.BUCKET

        /** Tank capacity expressed in milli-buckets (for display: "x / 4000 mB"). */
        val capacityMb: Int get() = (capacityDroplets / DROPLETS_PER_MB).toInt()
    }
}
