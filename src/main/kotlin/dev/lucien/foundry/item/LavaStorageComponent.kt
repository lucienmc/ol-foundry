package dev.lucien.foundry.item

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

/**
 * Stores how much lava (in milli-buckets) a Foundry item is carrying.
 * Attached to item stacks dropped by creative players so lava survives
 * the item form and is restored when the block is placed again.
 *
 * Tooltip rendering is handled client-side via [ItemTooltipCallback][net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback].
 */
@JvmRecord
data class LavaStorageComponent(val mb: Int) {

    companion object {
        val CODEC: Codec<LavaStorageComponent> =
            Codec.INT.xmap(::LavaStorageComponent, LavaStorageComponent::mb)

        val STREAM_CODEC: StreamCodec<ByteBuf, LavaStorageComponent> =
            ByteBufCodecs.INT.map(::LavaStorageComponent, LavaStorageComponent::mb)
    }
}
