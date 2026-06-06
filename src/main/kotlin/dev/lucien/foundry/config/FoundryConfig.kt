package dev.lucien.foundry.config

/**
 * User-tunable values, persisted as JSON in the config directory.
 *
 * Speed values are multipliers relative to a vanilla furnace (1.0 = furnace speed), applied before
 * the lava bonus. Burn times are in ticks and only matter for fuels that aren't vanilla furnace
 * fuels. All fields are validated by [sanitized] on load and save, so out-of-range or missing
 * values fall back to the defaults below.
 */
data class FoundryConfig(
    val baseFuelSpeedMultiplier: Double = 1.0,
    val coalFuelSpeedMultiplier: Double = 1.5,
    val magmaCreamFuelSpeedMultiplier: Double = 2.0,
    val blazeRodFuelSpeedMultiplier: Double = 3.0,
    val lavaSpeedMultiplier: Double = 2.0,
    val lavaTankCapacityBuckets: Int = 4,
    val slagBurnTime: Int = 800,
    val magmaCreamBurnTime: Int = 1000,
) {
    /** Returns a copy with every field clamped to a valid range; zero/missing values reset to default. */
    fun sanitized(): FoundryConfig {
        val d = DEFAULTS
        fun speed(value: Double, default: Double) =
            (if (value <= 0.0) default else value).coerceIn(MIN_SPEED, MAX_SPEED)

        fun ticks(value: Int, default: Int) =
            (if (value <= 0) default else value).coerceIn(MIN_BURN_TIME, MAX_BURN_TIME)

        return FoundryConfig(
            baseFuelSpeedMultiplier = speed(baseFuelSpeedMultiplier, d.baseFuelSpeedMultiplier),
            coalFuelSpeedMultiplier = speed(coalFuelSpeedMultiplier, d.coalFuelSpeedMultiplier),
            magmaCreamFuelSpeedMultiplier = speed(
                magmaCreamFuelSpeedMultiplier,
                d.magmaCreamFuelSpeedMultiplier
            ),
            blazeRodFuelSpeedMultiplier = speed(
                blazeRodFuelSpeedMultiplier,
                d.blazeRodFuelSpeedMultiplier
            ),
            lavaSpeedMultiplier = speed(lavaSpeedMultiplier, d.lavaSpeedMultiplier),
            lavaTankCapacityBuckets =
                (if (lavaTankCapacityBuckets <= 0) d.lavaTankCapacityBuckets else lavaTankCapacityBuckets)
                    .coerceIn(MIN_BUCKETS, MAX_BUCKETS),
            slagBurnTime = ticks(slagBurnTime, d.slagBurnTime),
            magmaCreamBurnTime = ticks(magmaCreamBurnTime, d.magmaCreamBurnTime),
        )
    }

    companion object {
        const val MIN_SPEED = 0.1
        const val MAX_SPEED = 50.0
        const val MIN_BUCKETS = 1
        const val MAX_BUCKETS = 64
        const val MIN_BURN_TIME = 1
        const val MAX_BURN_TIME = 100_000

        private val DEFAULTS = FoundryConfig()
    }
}
