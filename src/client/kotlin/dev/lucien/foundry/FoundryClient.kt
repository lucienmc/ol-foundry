package dev.lucien.foundry

import dev.lucien.foundry.registry.ModMenuTypes
import dev.lucien.foundry.screen.FoundryScreen
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screens.MenuScreens

class FoundryClient : ClientModInitializer {
    override fun onInitializeClient() {
        MenuScreens.register(ModMenuTypes.FOUNDRY, ::FoundryScreen)
    }
}
