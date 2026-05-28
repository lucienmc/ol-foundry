package dev.lucien.foundry.registry

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.menu.FoundryMenu
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.inventory.MenuType

object ModMenuTypes {

    val FOUNDRY: MenuType<FoundryMenu> = Registry.register(
        BuiltInRegistries.MENU,
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "foundry"),
        MenuType(::FoundryMenu, FeatureFlagSet.of())
    )

    fun init() { /* Triggers static initialisation */ }
}
