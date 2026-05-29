package dev.lucien.foundry.block.entity

import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * Mutable smelting state for the Foundry block entity.
 *
 * Extracted from [FoundryBlockEntity] so that tick logic and serialization
 * can read/write a single cohesive object rather than scattered fields.
 *
 * All serialization lives here — [FoundryBlockEntity] just calls [save]/[load].
 */
class FoundryState {

    var fuelBurnTimeLeft: Int = 0
    var maxFuelBurnTime: Int = 0
    var smeltProgress: Int = 0
    var smeltTotal: Int = DEFAULT_COOK_TIME
    var storedXp: Float = 0f

    val isBurning: Boolean get() = fuelBurnTimeLeft > 0

    // ── Serialization ─────────────────────────────────────────────────────────

    fun save(output: ValueOutput) {
        output.putInt("fuel_burn_time_left", fuelBurnTimeLeft)
        output.putInt("max_fuel_burn_time", maxFuelBurnTime)
        output.putInt("smelt_progress", smeltProgress)
        output.putInt("smelt_total", smeltTotal)
        output.putFloat("stored_xp", storedXp)
    }

    fun load(input: ValueInput) {
        fuelBurnTimeLeft = input.getIntOr("fuel_burn_time_left", 0)
        maxFuelBurnTime = input.getIntOr("max_fuel_burn_time", 0)
        smeltProgress = input.getIntOr("smelt_progress", 0)
        smeltTotal = input.getIntOr("smelt_total", DEFAULT_COOK_TIME)
        storedXp = input.getFloatOr("stored_xp", 0f)
    }

    companion object {
        const val DEFAULT_COOK_TIME = 200
    }
}
