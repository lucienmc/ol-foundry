package dev.lucien.foundry

import dev.lucien.foundry.registry.*
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

class Foundry : ModInitializer {
    companion object {
        const val MOD_ID = "foundry"
        val LOGGER = LoggerFactory.getLogger(MOD_ID)!!
    }

    override fun onInitialize() {
        ModBlocks.init()
        ModItems.init()
        ModBlockEntities.init()
        ModRecipes.init()
        ModMenuTypes.init()
        LOGGER.info("Foundry mod initialized.")
    }
}
