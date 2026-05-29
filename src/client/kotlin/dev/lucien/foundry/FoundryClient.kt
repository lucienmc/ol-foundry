package dev.lucien.foundry

import dev.lucien.foundry.block.entity.FoundryLavaTank
import dev.lucien.foundry.registry.ModDataComponents
import dev.lucien.foundry.registry.ModMenuTypes
import dev.lucien.foundry.screen.FoundryScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.network.chat.Component

class FoundryClient : ClientModInitializer {
    override fun onInitializeClient() {
        MenuScreens.register(ModMenuTypes.FOUNDRY, ::FoundryScreen)

        ItemTooltipCallback.EVENT.register { stack, _, _, tooltip ->
            val mb = stack[ModDataComponents.LAVA_STORAGE]?.mb ?: return@register
            if (mb > 0) {
                tooltip.add(
                    Component.literal("Lava: ")
                        .append(Component.literal("$mb").withStyle(ChatFormatting.GOLD))
                        .append(
                            Component.literal(" / ${FoundryLavaTank.CAPACITY_MB} mB")
                                .withStyle(ChatFormatting.GRAY)
                        )
                )
            }
        }
    }
}
