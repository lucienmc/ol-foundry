package dev.lucien.foundry.jei

import net.minecraft.world.item.ItemStack

/** One entry in the Foundry Fuels JEI tab. */
data class FoundryFuelDisplay(
    val fuels: List<ItemStack>,
    val speedMultiplier: Double,
    /** Null for entries where burn time doesn't apply (lava booster, other-fuels catch-all). */
    val burnTimeTicks: Int?,
    val note: String? = null,
)
