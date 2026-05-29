package dev.lucien.foundry.recipe

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.lucien.foundry.registry.ModRecipes
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.Level

/**
 * A Foundry smelting recipe.
 *
 * @param ingredient        The input ingredient.
 * @param result            The main output (always produced).
 * @param byproductChance   Probability [0,1] of producing one Slag as byproduct.
 * @param cookingTime       Base ticks to complete (before heat-source multipliers).
 * @param experience        XP awarded on completion.
 * @param bonusResultChance Probability [0,1] of producing one extra result roll.
 * @param bonusRequiresLava If true, [bonusResultChance] only rolls when the tank has lava.
 * @param resultPool        Optional weighted outputs. When non-empty, each produced item is drawn
 *                          from this pool (see [rollResult]) and [result] is just the representative
 *                          (typical) output shown in recipe viewers.
 */
class FoundryRecipe(
    val ingredient: Ingredient,
    val result: ItemStackTemplate,
    val byproductChance: Float = 0.0f,
    val cookingTime: Int = 200,
    val experience: Float = 0.1f,
    val bonusResultChance: Float = 0.0f,
    val bonusRequiresLava: Boolean = false,
    val resultPool: List<WeightedResult> = emptyList(),
) : Recipe<FoundryRecipeInput> {

    val isPooled: Boolean get() = resultPool.isNotEmpty()

    /** A single output: a weighted pick from [resultPool] when present, otherwise [result]. */
    fun rollResult(random: RandomSource): ItemStack {
        val total = resultPool.sumOf { it.weight }
        if (total <= 0) return result.create()
        var roll = random.nextInt(total)
        for (entry in resultPool) {
            roll -= entry.weight
            if (roll < 0) return entry.result.create()
        }
        return resultPool.last().result.create()
    }

    override fun matches(input: FoundryRecipeInput, level: Level): Boolean =
        ingredient.test(input.inputItem)

    override fun assemble(input: FoundryRecipeInput): ItemStack =
        result.create()

    override fun getSerializer(): RecipeSerializer<out Recipe<FoundryRecipeInput>> =
        ModRecipes.FOUNDRY_RECIPE_SERIALIZER

    override fun getType(): RecipeType<out Recipe<FoundryRecipeInput>> =
        ModRecipes.FOUNDRY_RECIPE_TYPE

    override fun placementInfo(): PlacementInfo = PlacementInfo.NOT_PLACEABLE
    override fun isSpecial(): Boolean = true
    override fun showNotification(): Boolean = false
    override fun group(): String = "foundry"
    override fun recipeBookCategory(): RecipeBookCategory =
        RecipeBookCategories.BLAST_FURNACE_BLOCKS

    companion object {
        val CODEC: MapCodec<FoundryRecipe> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(FoundryRecipe::ingredient),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(FoundryRecipe::result),
                Codec.FLOAT.optionalFieldOf("byproduct_chance", 0.0f)
                    .forGetter(FoundryRecipe::byproductChance),
                Codec.INT.optionalFieldOf("cooking_time", 200)
                    .forGetter(FoundryRecipe::cookingTime),
                Codec.FLOAT.optionalFieldOf("experience", 0.1f)
                    .forGetter(FoundryRecipe::experience),
                Codec.FLOAT.optionalFieldOf("bonus_result_chance", 0.0f)
                    .forGetter(FoundryRecipe::bonusResultChance),
                Codec.BOOL.optionalFieldOf("bonus_requires_lava", false)
                    .forGetter(FoundryRecipe::bonusRequiresLava),
                WeightedResult.CODEC.listOf().optionalFieldOf("result_pool", emptyList())
                    .forGetter(FoundryRecipe::resultPool),
            ).apply(instance, ::FoundryRecipe)
        }

        // StreamCodec.composite only goes up to 6 fields; use a manual codec for 7.
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FoundryRecipe> =
            object : StreamCodec<RegistryFriendlyByteBuf, FoundryRecipe> {
                override fun decode(buf: RegistryFriendlyByteBuf) = FoundryRecipe(
                    ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    result = ItemStackTemplate.STREAM_CODEC.decode(buf),
                    byproductChance = buf.readFloat(),
                    cookingTime = buf.readInt(),
                    experience = buf.readFloat(),
                    bonusResultChance = buf.readFloat(),
                    bonusRequiresLava = buf.readBoolean(),
                    resultPool = (0 until buf.readInt()).map { WeightedResult.STREAM_CODEC.decode(buf) },
                )

                override fun encode(buf: RegistryFriendlyByteBuf, v: FoundryRecipe) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, v.ingredient)
                    ItemStackTemplate.STREAM_CODEC.encode(buf, v.result)
                    buf.writeFloat(v.byproductChance)
                    buf.writeInt(v.cookingTime)
                    buf.writeFloat(v.experience)
                    buf.writeFloat(v.bonusResultChance)
                    buf.writeBoolean(v.bonusRequiresLava)
                    buf.writeInt(v.resultPool.size)
                    v.resultPool.forEach { WeightedResult.STREAM_CODEC.encode(buf, it) }
                }
            }
    }
}
