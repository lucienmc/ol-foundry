package dev.lucien.foundry.registry

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.item.LavaStorageComponent
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

object ModDataComponents {

    val LAVA_STORAGE: DataComponentType<LavaStorageComponent> = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "lava_storage"),
        DataComponentType.builder<LavaStorageComponent>()
            .persistent(LavaStorageComponent.CODEC)
            .networkSynchronized(LavaStorageComponent.STREAM_CODEC)
            .build()
    )

    fun init() { /* triggers static initialisation */
    }
}
