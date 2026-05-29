package dev.lucien.foundry.recipe

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStackTemplate

/** One possible weighted output of a [FoundryRecipe]'s result pool. */
data class WeightedResult(val result: ItemStackTemplate, val weight: Int) {

    companion object {
        val CODEC: Codec<WeightedResult> = RecordCodecBuilder.create { instance ->
            instance.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(WeightedResult::result),
                Codec.intRange(1, Int.MAX_VALUE).fieldOf("weight").forGetter(WeightedResult::weight),
            ).apply(instance, ::WeightedResult)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WeightedResult> =
            object : StreamCodec<RegistryFriendlyByteBuf, WeightedResult> {
                override fun decode(buf: RegistryFriendlyByteBuf) =
                    WeightedResult(ItemStackTemplate.STREAM_CODEC.decode(buf), buf.readInt())

                override fun encode(buf: RegistryFriendlyByteBuf, v: WeightedResult) {
                    ItemStackTemplate.STREAM_CODEC.encode(buf, v.result)
                    buf.writeInt(v.weight)
                }
            }
    }
}
