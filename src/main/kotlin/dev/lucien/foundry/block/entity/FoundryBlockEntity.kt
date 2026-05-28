package dev.lucien.foundry.block.entity

import dev.lucien.foundry.menu.FoundryMenu
import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.recipe.FoundryRecipeInput
import dev.lucien.foundry.registry.ModBlockEntities
import dev.lucien.foundry.registry.ModItems
import dev.lucien.foundry.registry.ModRecipes
import dev.lucien.foundry.util.ImplementedContainer
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.phys.Vec3
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class FoundryBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.FOUNDRY, pos, state),
    ImplementedContainer,
    MenuProvider {

    // ── Inventory ────────────────────────────────────────────────────────────

    private val items: NonNullList<ItemStack> =
        NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)

    override fun getItems(): NonNullList<ItemStack> = items

    /** Expose Container.stillValid backed by block proximity check */
    override fun stillValid(player: Player): Boolean =
        net.minecraft.world.Container.stillValidBlockEntity(this, player)

    // ── Fluid Tank ────────────────────────────────────────────────────────────

    /**
     * Lava storage tank (capacity: 4 buckets).
     * Exposed to pipes via Fabric Transfer API (see ModBlockEntities).
     */
    val fluidStorage = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant =
            FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = LAVA_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean =
            variant.fluid == Fluids.LAVA

        override fun canExtract(variant: FluidVariant): Boolean =
            true

        override fun onFinalCommit() {
            setChanged()
        }
    }

    // ── Smelting State ────────────────────────────────────────────────────────

    /** Remaining burn ticks for the currently burning fuel piece */
    var fuelBurnTimeLeft: Int = 0

    /** Total burn ticks of the fuel piece that is currently burning */
    var maxFuelBurnTime: Int = 0

    /** How many ticks of progress have been made on the current recipe */
    var smeltProgress: Int = 0

    /** Total ticks needed to complete the current recipe (may vary by recipe) */
    var smeltTotal: Int = DEFAULT_COOK_TIME

    /** Fractional XP banked from completed recipes; awarded when player takes output */
    var storedXp: Float = 0f

    // ── ContainerData (synced to client for progress bars) ────────────────────

    val containerData: ContainerData = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            0 -> smeltProgress
            1 -> smeltTotal
            2 -> fuelBurnTimeLeft.coerceAtMost(Short.MAX_VALUE.toInt())
            3 -> maxFuelBurnTime.coerceAtMost(Short.MAX_VALUE.toInt())
            4 -> if (fluidStorage.amount > 0L) ((fluidStorage.amount * 100L) / LAVA_CAPACITY).toInt() else 0
            // slot 5: lava in millibuckets (81 droplets = 1 mB); max 4000 mB, fits in Int
            5 -> (fluidStorage.amount / 81L).toInt()
            else -> 0
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                0 -> smeltProgress = value
                1 -> smeltTotal = value
                2 -> fuelBurnTimeLeft = value
                3 -> maxFuelBurnTime = value
                // indices 4 and 5 are read-only on client
            }
        }

        override fun getCount(): Int = 6
    }

    // ── Server Tick ───────────────────────────────────────────────────────────

    private fun serverTick(level: ServerLevel) {
        // Process lava bucket slot: consume a lava bucket to fill the tank
        val bucketStack = items[LAVA_BUCKET_SLOT]
        if (!bucketStack.isEmpty && bucketStack.`is`(Items.LAVA_BUCKET)) {
            val space = LAVA_CAPACITY - fluidStorage.amount
            if (space >= FluidConstants.BUCKET) {
                Transaction.openOuter().use { tx ->
                    fluidStorage.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx)
                    tx.commit()
                }
                items[LAVA_BUCKET_SLOT] = ItemStack(Items.BUCKET)
            }
        }

        val inputStack = items[INPUT_SLOT]
        val recipeInput = FoundryRecipeInput(inputStack, fluidStorage.amount > 0L)

        val recipeHolder = level.recipeAccess()
            .getRecipeFor(ModRecipes.FOUNDRY_RECIPE_TYPE, recipeInput, level)
            .orElse(null)

        val recipe = recipeHolder?.value()
        val canSmelt = recipe != null && canOutput(recipe)

        // Start burning a new fuel piece if needed
        if (canSmelt && fuelBurnTimeLeft <= 0) {
            tryStartFuel(level)
        }

        val isHot = fuelBurnTimeLeft > 0
        var dirty = false
        if (isHot) { fuelBurnTimeLeft--; dirty = true }

        if (isHot && canSmelt) {
            if (smeltProgress == 0) smeltTotal = recipe!!.cookingTime

            val hasLavaBoost = fluidStorage.amount > 0L
            val speedMultiplier = if (hasLavaBoost) 4 else 2
            smeltProgress += speedMultiplier
            dirty = true

            if (hasLavaBoost) {
                Transaction.openOuter().use { tx ->
                    fluidStorage.extract(FluidVariant.of(Fluids.LAVA), LAVA_DRAIN_PER_TICK, tx)
                    tx.commit()
                }
            }

            if (smeltProgress >= smeltTotal) {
                smeltProgress = 0
                craftResult(recipe, level)
            }
        } else if (smeltProgress > 0 && !isHot) {
            smeltProgress = (smeltProgress - 2).coerceAtLeast(0)
            dirty = true
        }

        if (dirty) setChanged()
    }

    private fun tryStartFuel(level: ServerLevel) {
        val fuel = items[FUEL_SLOT]
        if (fuel.isEmpty) return
        val burnTime = getFuelBurnTime(level, fuel)
        if (burnTime <= 0) return

        maxFuelBurnTime = burnTime
        fuelBurnTimeLeft = burnTime

        val remainder = fuel.item.getCraftingRemainder()?.create() ?: ItemStack.EMPTY
        fuel.shrink(1)
        if (fuel.isEmpty && !remainder.isEmpty) {
            items[FUEL_SLOT] = remainder
        }
    }

    /**
     * Checks vanilla fuel values, then falls back to custom fuels (slag).
     * FuelValues is obtained from the MinecraftServer instance.
     * If your version exposes it directly on Level, use `level.fuelValues()` instead.
     */
    private fun getFuelBurnTime(level: ServerLevel, stack: ItemStack): Int {
        val vanillaTime = level.server!!.fuelValues().burnDuration(stack)
        if (vanillaTime > 0) return vanillaTime
        // Custom fuel: slag (800 ticks ≈ wooden plank level)
        if (stack.`is`(ModItems.SLAG)) return 800
        return 0
    }

    /** Returns true if [item] fits in [slotIndex] (empty or same item with room). */
    private fun canFitInSlot(slotIndex: Int, item: ItemStack): Boolean {
        val slot = items[slotIndex]
        return when {
            slot.isEmpty -> true
            !ItemStack.isSameItemSameComponents(slot, item) -> false
            else -> slot.count + item.count <= slot.maxStackSize
        }
    }

    /**
     * Tries to place [item] in the first available output slot (OUTPUT_SLOT → OUTPUT_SLOT_2 → OUTPUT_SLOT_3).
     * Returns true if successfully placed.
     */
    private fun addToOutputSlots(item: ItemStack): Boolean {
        for (slotIndex in intArrayOf(OUTPUT_SLOT, OUTPUT_SLOT_2, OUTPUT_SLOT_3)) {
            val slot = items[slotIndex]
            when {
                slot.isEmpty -> { items[slotIndex] = item.copy(); return true }
                ItemStack.isSameItemSameComponents(slot, item) &&
                        slot.count + item.count <= slot.maxStackSize -> { slot.grow(item.count); return true }
            }
        }
        return false
    }

    private fun canOutput(recipe: FoundryRecipe): Boolean {
        if (items[INPUT_SLOT].isEmpty) return false
        val result = recipe.result.create()

        // At least one of the three output slots must be able to accept the result
        val canFitOutput = canFitInSlot(OUTPUT_SLOT, result)
                || canFitInSlot(OUTPUT_SLOT_2, result)
                || canFitInSlot(OUTPUT_SLOT_3, result)
        if (!canFitOutput) return false

        if (recipe.byproductChance > 0f) {
            val minSlag = recipe.byproductChance.toInt().coerceAtLeast(1)
            val slag    = ItemStack(ModItems.SLAG, minSlag)
            if (!canFitInSlot(BYPRODUCT_SLOT, slag)) return false
        }

        return true
    }

    private fun craftResult(recipe: FoundryRecipe, level: ServerLevel) {
        val result = recipe.result.create()

        // Consume one input item
        items[INPUT_SLOT].shrink(1)

        // Primary output → fills OUTPUT_SLOT first, spills to OUTPUT_SLOT_2/3 if needed
        addToOutputSlots(result)

        storedXp += recipe.experience

        // Bonus result (e.g. extra netherite scrap when lava is present)
        if (recipe.bonusResultChance > 0f) {
            val hasLava = fluidStorage.amount > 0L
            if (!recipe.bonusRequiresLava || hasLava) {
                if (level.random.nextFloat() < recipe.bonusResultChance) {
                    addToOutputSlots(result.copy())
                }
            }
        }

        // Byproduct: floor(chance) guaranteed slag + fractional roll for one more.
        // byproductChance > 1 is supported for bulk recipes (e.g. raw-ore-blocks).
        if (recipe.byproductChance > 0f) {
            val guaranteed = recipe.byproductChance.toInt()
            val fraction   = recipe.byproductChance - guaranteed
            val count = guaranteed + if (fraction > 0f && level.random.nextFloat() < fraction) 1 else 0
            if (count > 0) {
                val slag     = ItemStack(ModItems.SLAG)
                val byproduct = items[BYPRODUCT_SLOT]
                when {
                    byproduct.isEmpty ->
                        items[BYPRODUCT_SLOT] = ItemStack(ModItems.SLAG, count)
                    ItemStack.isSameItemSameComponents(byproduct, slag) &&
                            byproduct.count + count <= byproduct.maxStackSize ->
                        byproduct.grow(count)
                }
            }
        }
    }

    // ── Experience ────────────────────────────────────────────────────────────

    /** Spawn XP orbs at the block for all banked experience. Called when player takes output. */
    fun popExperience(serverLevel: ServerLevel) {
        val whole = storedXp.toInt()
        val frac = storedXp - whole
        val total = whole + if (serverLevel.random.nextFloat() < frac) 1 else 0
        storedXp = 0f
        if (total > 0) ExperienceOrb.award(serverLevel, Vec3.atCenterOf(blockPos), total)
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        ContainerHelper.saveAllItems(output, items)
        output.putInt("fuel_burn_time_left", fuelBurnTimeLeft)
        output.putInt("max_fuel_burn_time", maxFuelBurnTime)
        output.putInt("smelt_progress", smeltProgress)
        output.putInt("smelt_total", smeltTotal)
        output.putFloat("stored_xp", storedXp)
        output.putLong("lava_amount", fluidStorage.amount)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        ContainerHelper.loadAllItems(input, items)
        fuelBurnTimeLeft = input.getIntOr("fuel_burn_time_left", 0)
        maxFuelBurnTime = input.getIntOr("max_fuel_burn_time", 0)
        smeltProgress = input.getIntOr("smelt_progress", 0)
        smeltTotal = input.getIntOr("smelt_total", DEFAULT_COOK_TIME)
        storedXp = input.getFloatOr("stored_xp", 0f)
        val savedLava = input.getLongOr("lava_amount", 0L)
        if (savedLava > 0L) {
            fluidStorage.variant = FluidVariant.of(Fluids.LAVA)
            fluidStorage.amount = savedLava.coerceAtMost(LAVA_CAPACITY)
        }
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registryLookup)

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    // ── MenuProvider ──────────────────────────────────────────────────────────

    override fun getDisplayName(): Component =
        Component.translatable("block.foundry.foundry")

    override fun createMenu(
        containerId: Int,
        inventory: Inventory,
        player: Player
    ): AbstractContainerMenu =
        FoundryMenu(containerId, inventory, this, containerData)

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val INPUT_SLOT        = 0
        const val FUEL_SLOT         = 1
        const val OUTPUT_SLOT       = 2   // primary result
        const val OUTPUT_SLOT_2     = 3   // bonus result / overflow
        const val OUTPUT_SLOT_3     = 4   // overflow / future use
        const val BYPRODUCT_SLOT    = 5   // slag
        const val LAVA_BUCKET_SLOT  = 6
        const val INVENTORY_SIZE    = 7

        val LAVA_CAPACITY: Long = FluidConstants.BUCKET * 4          // 4-bucket tank
        val LAVA_DRAIN_PER_TICK: Long = FluidConstants.BUCKET / 1600 // ~0.025 bucket per boosted recipe
        const val DEFAULT_COOK_TIME = 200                             // ticks

        @JvmStatic
        fun tick(level: Level, pos: BlockPos, state: BlockState, entity: FoundryBlockEntity) {
            if (level.isClientSide) return
            entity.serverTick(level as ServerLevel)
        }
    }
}
