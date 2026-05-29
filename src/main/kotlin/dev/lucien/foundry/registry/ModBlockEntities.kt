package dev.lucien.foundry.registry

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.block.entity.FoundryBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntityType

object ModBlockEntities {

    val FOUNDRY: BlockEntityType<FoundryBlockEntity> = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "foundry"),
        FabricBlockEntityTypeBuilder.create(::FoundryBlockEntity, ModBlocks.FOUNDRY).build()
    )

    fun init() {
        // Expose the fluid tank to pipes on all sides
        FluidStorage.SIDED.registerForBlockEntity(
            { entity, _ -> entity.lava.storage },
            FOUNDRY
        )
    }
}
