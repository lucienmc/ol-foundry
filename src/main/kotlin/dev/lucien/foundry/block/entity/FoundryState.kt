package dev.lucien.foundry.block.entity

import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class FoundryState {

    var fuelBurnTimeLeft: Int = 0
    var maxFuelBurnTime: Int = 0
    var smeltProgress: Int = 0
    var smeltTotal: Int = DEFAULT_COOK_TIME
    var storedXp: Float = 0f

    /** Smelting progress added per tick by the fuel currently burning (before the lava multiplier). */
    var fuelSpeed: Int = 0

    val isBurning: Boolean get() = fuelBurnTimeLeft > 0

    fun save(output: ValueOutput) {
        output.putInt("fuel_burn_time_left", fuelBurnTimeLeft)
        output.putInt("max_fuel_burn_time", maxFuelBurnTime)
        output.putInt("smelt_progress", smeltProgress)
        output.putInt("smelt_total", smeltTotal)
        output.putInt("fuel_speed", fuelSpeed)
        output.putFloat("stored_xp", storedXp)
    }

    fun load(input: ValueInput) {
        fuelBurnTimeLeft = input.getIntOr("fuel_burn_time_left", 0)
        maxFuelBurnTime = input.getIntOr("max_fuel_burn_time", 0)
        smeltProgress = input.getIntOr("smelt_progress", 0)
        smeltTotal = input.getIntOr("smelt_total", DEFAULT_COOK_TIME)
        fuelSpeed = input.getIntOr("fuel_speed", 0)
        storedXp = input.getFloatOr("stored_xp", 0f)
    }

    companion object {
        const val DEFAULT_COOK_TIME = 200
    }
}
