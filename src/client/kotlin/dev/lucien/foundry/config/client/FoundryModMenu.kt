package dev.lucien.foundry.config.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen

/**
 * Registers the config screen with Mod Menu. The screen needs YACL, so when YACL is absent we fall
 * back to Mod Menu's default (no button) rather than crashing on click.
 */
class FoundryModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        if (!FabricLoader.getInstance().isModLoaded(YACL_MOD_ID)) {
            return super.getModConfigScreenFactory()
        }
        return ConfigScreenFactory<Screen> { parent -> FoundryConfigScreen.create(parent) }
    }

    private companion object {
        const val YACL_MOD_ID = "yet_another_config_lib_v3"
    }
}
