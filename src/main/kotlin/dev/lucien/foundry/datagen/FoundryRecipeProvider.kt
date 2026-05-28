package dev.lucien.foundry.datagen

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.registry.ModBlocks
import dev.lucien.foundry.registry.ModItems
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import java.util.concurrent.CompletableFuture

/**
 * Registered as a FabricRecipeProvider.Runner via FoundryDatagen.
 * Delegates to the inner RecipeProvider which has access to the items HolderGetter.
 */
class FoundryRecipeProvider(
    output: FabricPackOutput,
    registries: CompletableFuture<HolderLookup.Provider>,
) : FabricRecipeProvider(output, registries) {

    override fun createRecipeProvider(
        registries: HolderLookup.Provider,
        output: RecipeOutput,
    ): RecipeProvider = object : RecipeProvider(registries, output) {

        override fun buildRecipes() {
            val items = registries.lookupOrThrow(Registries.ITEM)

            // ── Foundry block ─────────────────────────────────────────────────
            ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ModBlocks.FOUNDRY)
                .pattern("SRS")
                .pattern("RBR")
                .pattern("SRS")
                .define('S', Items.SMOOTH_STONE)
                .define('R', Items.REDSTONE_BLOCK)
                .define('B', Items.BLAST_FURNACE)
                .unlockedBy("has_blast_furnace", has(Items.BLAST_FURNACE))
                .save(output)

            // ── Slag Bricks (2×2 → 4) ─────────────────────────────────────────
            ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SLAG_BRICKS, 4)
                .pattern("SS")
                .pattern("SS")
                .define('S', ModItems.SLAG)
                .unlockedBy("has_slag", has(ModItems.SLAG))
                .save(output)

            // ── Foundry smelting ──────────────────────────────────────────────
            smelt("iron_ingot",    Items.RAW_IRON,          Items.IRON_INGOT,    0.35f, 160,  0.7f)
            smelt("gold_ingot",    Items.RAW_GOLD,          Items.GOLD_INGOT,    0.35f, 160,  1.0f)
            smelt("copper_ingot",  Items.RAW_COPPER,        Items.COPPER_INGOT,  0.30f, 150,  0.7f)
            smelt("iron_block",    Items.RAW_IRON_BLOCK,    Items.IRON_BLOCK,    3.15f, 1200, 6.3f)
            smelt("gold_block",    Items.RAW_GOLD_BLOCK,    Items.GOLD_BLOCK,    3.15f, 1200, 9.0f)
            smelt("copper_block",  Items.RAW_COPPER_BLOCK,  Items.COPPER_BLOCK,  2.70f, 1200, 6.3f)
            smelt("stone",         Items.COBBLESTONE,       Items.STONE,         0.0f,  100,  0.1f)
            smelt("flint",         Items.GRAVEL,            Items.FLINT,         0.0f,  120,  0.1f)
            smelt("glass",         Items.SAND,              Items.GLASS,         0.0f,  200,  0.1f)
            smelt("glass_red_sand",Items.RED_SAND,          Items.GLASS,         0.0f,  200,  0.1f)
        }

        private fun smelt(
            name: String,
            input: Item,
            result: Item,
            byproductChance: Float,
            cookingTime: Int,
            experience: Float,
        ) {
            val key = ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "foundry/$name"),
            )
            output.accept(
                key,
                FoundryRecipe(
                    ingredient      = Ingredient.of(input),
                    result          = ItemStackTemplate(result),
                    byproductChance = byproductChance,
                    cookingTime     = cookingTime,
                    experience      = experience,
                ),
                null,
            )
        }
    }

    override fun getName() = "Foundry Recipes"
}
