package dev.lucien.foundry.jei

import net.minecraft.world.item.ItemStack

/** A single entry in the Foundry Fuels JEI tab: a fuel (or set of equivalents) and its smelting stats. */
data class FoundryFuelDisplay(
    val fuels: List<ItemStack>,
    val speedMultiplier: Double,
    /** Burn duration in ticks, or null when the fuel uses its vanilla furnace burn time. */
    val burnTimeTicks: Int?,
    /** Optional extra note, e.g. lava's boost behaviour. */
    val note: String? = null,
)
