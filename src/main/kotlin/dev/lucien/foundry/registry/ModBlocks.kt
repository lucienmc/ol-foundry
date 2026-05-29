package dev.lucien.foundry.registry

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.block.FoundryBlock
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

object ModBlocks {

    val FOUNDRY: FoundryBlock = register(
        "foundry",
        FoundryBlock(
            BlockBehaviour.Properties.of()
                .setId(blockKey("foundry"))
                .strength(3.5f, 10.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .lightLevel { if (it.getValue(AbstractFurnaceBlock.LIT)) 13 else 0 }
        )
    )

    val SLAG_BRICKS: Block = register(
        "slag_bricks",
        Block(
            BlockBehaviour.Properties.of()
                .setId(blockKey("slag_bricks"))
                .strength(1.5f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
        )
    )

    private fun blockKey(name: String): ResourceKey<Block> = ResourceKey.create(
        BuiltInRegistries.BLOCK.key(),
        Identifier.fromNamespaceAndPath(Foundry.MOD_ID, name)
    )

    private fun <T : Block> register(name: String, block: T): T {
        return Registry.register(
            BuiltInRegistries.BLOCK,
            Identifier.fromNamespaceAndPath(Foundry.MOD_ID, name),
            block
        )
    }

    /** Forces class-loading so the registrations above run. */
    fun init() {}
}
