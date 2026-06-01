package dev.lucien.foundry.config.client

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.lucien.foundry.config.FoundryConfig
import dev.lucien.foundry.config.FoundryConfigManager
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Builds the YACL options screen. Referenced only by [FoundryModMenu] and only when YACL is
 * actually installed, so this class (and its YACL imports) never loads otherwise.
 */
object FoundryConfigScreen {

    fun create(parent: Screen?): Screen {
        val cfg = FoundryConfigManager.config
        val default = FoundryConfig()

        var base = cfg.baseFuelSpeedMultiplier
        var coal = cfg.coalFuelSpeedMultiplier
        var magma = cfg.magmaCreamFuelSpeedMultiplier
        var blaze = cfg.blazeRodFuelSpeedMultiplier
        var lavaMultiplier = cfg.lavaSpeedMultiplier
        var capacityBuckets = cfg.lavaTankCapacityBuckets
        var slagBurn = cfg.slagBurnTime
        var magmaBurn = cfg.magmaCreamBurnTime

        val speedGroup = OptionGroup.createBuilder()
            .name(Component.literal("Smelting speed"))
            .description(OptionDescription.of(Component.literal("Multipliers relative to a vanilla furnace, applied before the lava bonus.")))
            .option(speedOption("Base fuel", "Any fuel not listed below.", default.baseFuelSpeedMultiplier, { base }, { base = it }))
            .option(speedOption("Coal / charcoal", "Speed when burning items tagged #c:coals.", default.coalFuelSpeedMultiplier, { coal }, { coal = it }))
            .option(speedOption("Magma cream", "Speed when burning magma cream.", default.magmaCreamFuelSpeedMultiplier, { magma }, { magma = it }))
            .option(speedOption("Blaze rod", "Speed when burning a blaze rod.", default.blazeRodFuelSpeedMultiplier, { blaze }, { blaze = it }))
            .option(speedOption("Lava bonus", "Extra multiplier applied while the tank holds lava.", default.lavaSpeedMultiplier, { lavaMultiplier }, { lavaMultiplier = it }))
            .build()

        val tankGroup = OptionGroup.createBuilder()
            .name(Component.literal("Tank & fuel"))
            .option(
                Option.createBuilder<Int>()
                    .name(Component.literal("Lava tank capacity (buckets)"))
                    .description(OptionDescription.of(Component.literal("How much lava the tank holds.")))
                    .binding(default.lavaTankCapacityBuckets, Supplier { capacityBuckets }, Consumer { capacityBuckets = it })
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(FoundryConfig.MIN_BUCKETS, FoundryConfig.MAX_BUCKETS)
                            .step(1)
                    }
                    .build()
            )
            .option(burnTimeOption("Slag burn time (ticks)", "How long slag burns as fuel.", default.slagBurnTime, { slagBurn }, { slagBurn = it }))
            .option(burnTimeOption("Magma cream burn time (ticks)", "How long magma cream burns as fuel.", default.magmaCreamBurnTime, { magmaBurn }, { magmaBurn = it }))
            .build()

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Foundry"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.literal("Foundry"))
                    .group(speedGroup)
                    .group(tankGroup)
                    .build()
            )
            .save {
                FoundryConfigManager.save(
                    FoundryConfig(base, coal, magma, blaze, lavaMultiplier, capacityBuckets, slagBurn, magmaBurn)
                )
            }
            .build()
            .generateScreen(parent)
    }

    private fun speedOption(
        name: String,
        tooltip: String,
        default: Double,
        getter: () -> Double,
        setter: (Double) -> Unit,
    ): Option<Double> =
        Option.createBuilder<Double>()
            .name(Component.literal(name))
            .description(OptionDescription.of(Component.literal(tooltip)))
            .binding(default, Supplier { getter() }, Consumer { setter(it) })
            .controller { opt ->
                DoubleSliderControllerBuilder.create(opt)
                    .range(FoundryConfig.MIN_SPEED, SPEED_SLIDER_MAX)
                    .step(0.1)
            }
            .build()

    private fun burnTimeOption(
        name: String,
        tooltip: String,
        default: Int,
        getter: () -> Int,
        setter: (Int) -> Unit,
    ): Option<Int> =
        Option.createBuilder<Int>()
            .name(Component.literal(name))
            .description(OptionDescription.of(Component.literal(tooltip)))
            .binding(default, Supplier { getter() }, Consumer { setter(it) })
            .controller { opt ->
                IntegerFieldControllerBuilder.create(opt)
                    .range(FoundryConfig.MIN_BURN_TIME, FoundryConfig.MAX_BURN_TIME)
            }
            .build()

    /** Slider ceiling for speed multipliers; the config itself allows higher values via the JSON file. */
    private const val SPEED_SLIDER_MAX = 10.0
}
