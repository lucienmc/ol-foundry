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

/**
 * Lava fluid tank for the Foundry (capacity: 4 buckets).
 *
 * Owns all fluid logic: the [SingleVariantStorage], bucket consumption,
 * per-tick drain, serialization, and the derived display values used by
 * [ContainerData][net.minecraft.world.inventory.ContainerData].
 *
 * @param onChanged Called whenever the tank contents change (delegates to
 *   [BlockEntity.setChanged][net.minecraft.world.level.block.entity.BlockEntity.setChanged]).
 */
class FoundryLavaTank(private val onChanged: () -> Unit) {

    val storage: SingleVariantStorage<FluidVariant> =
        object : SingleVariantStorage<FluidVariant>() {
            override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
            override fun getCapacity(variant: FluidVariant): Long = CAPACITY
            override fun canInsert(variant: FluidVariant): Boolean = variant.fluid == Fluids.LAVA
            override fun canExtract(variant: FluidVariant): Boolean = true
            override fun onFinalCommit() = onChanged()
        }

    // ── Derived state (used by ContainerData and the Screen) ──────────────────

    val hasLava: Boolean get() = storage.amount > 0L

    /** 0–100 fill percentage. */
    val percent: Int get() = if (storage.amount > 0L) ((storage.amount * 100L) / CAPACITY).toInt() else 0

    /** Fill level in milli-buckets (0–4000). */
    val mb: Int get() = (storage.amount / 81L).toInt()

    // ── Bucket slot ───────────────────────────────────────────────────────────

    /**
     * If [bucketSlot] holds a lava bucket and the tank has room for a full bucket,
     * inserts the lava and returns the empty bucket replacement. Returns null if
     * nothing was consumed (slot empty, wrong item, or tank full).
     */
    fun tryConsumeBucket(bucketSlot: ItemStack): ItemStack? {
        if (bucketSlot.isEmpty || !bucketSlot.`is`(Items.LAVA_BUCKET)) return null
        if (CAPACITY - storage.amount < FluidConstants.BUCKET) return null
        Transaction.openOuter().use { tx ->
            storage.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx)
            tx.commit()
        }
        return ItemStack(Items.BUCKET)
    }

    // ── Per-tick boost drain ──────────────────────────────────────────────────

    /** Drains [DRAIN_PER_TICK] droplets of lava. Call once per boosted smelting tick. */
    fun drainForBoost() {
        Transaction.openOuter().use { tx ->
            storage.extract(FluidVariant.of(Fluids.LAVA), DRAIN_PER_TICK, tx)
            tx.commit()
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

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

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** Maximum tank capacity: 4 buckets in Fabric droplet units. */
        const val CAPACITY: Long = FluidConstants.BUCKET * 4

        /** Lava drained per boosted smelting tick (~0.025 buckets per recipe at 4× speed). */
        const val DRAIN_PER_TICK: Long = FluidConstants.BUCKET / 1600
    }
}
