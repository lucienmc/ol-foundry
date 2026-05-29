package dev.lucien.foundry.block.entity

import dev.lucien.foundry.menu.FoundryMenu
import dev.lucien.foundry.recipe.FoundryRecipe
import dev.lucien.foundry.recipe.FoundryRecipeInput
import dev.lucien.foundry.registry.ModBlockEntities
import dev.lucien.foundry.registry.ModItems
import dev.lucien.foundry.registry.ModRecipes
import dev.lucien.foundry.util.ImplementedContainer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3

class FoundryBlockEntity(pos: BlockPos, blockState: BlockState) :
    BlockEntity(ModBlockEntities.FOUNDRY, pos, blockState), ImplementedContainer, WorldlyContainer,
    MenuProvider {

    private val items: NonNullList<ItemStack> =
        NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)

    override fun getItems(): NonNullList<ItemStack> = items

    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun getSlotsForFace(side: Direction): IntArray = when (side) {
        Direction.UP -> SLOTS_TOP
        Direction.DOWN -> EXTRACT_SLOTS
        else -> SLOTS_SIDE
    }

    override fun canPlaceItemThroughFace(
        index: Int,
        stack: ItemStack,
        direction: Direction?
    ): Boolean =
        when (index) {
            INPUT_SLOT -> isSmeltable(stack)
            FUEL_SLOT -> !stack.`is`(Items.LAVA_BUCKET) && !isSmeltable(stack)
            LAVA_BUCKET_SLOT -> stack.`is`(Items.LAVA_BUCKET)
            else -> false
        }

    private fun isSmeltable(stack: ItemStack): Boolean =
        isSmeltable(level ?: return true, stack)

    override fun canTakeItemThroughFace(
        index: Int,
        stack: ItemStack,
        direction: Direction
    ): Boolean = index in EXTRACT_SLOTS

    val lava = FoundryLavaTank { setChanged() }
    val state = FoundryState()

    val containerData: ContainerData = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            DATA_SMELT_PROGRESS -> state.smeltProgress
            DATA_SMELT_TOTAL -> state.smeltTotal
            DATA_FUEL_LEFT -> state.fuelBurnTimeLeft.coerceAtMost(Short.MAX_VALUE.toInt())
            DATA_FUEL_MAX -> state.maxFuelBurnTime.coerceAtMost(Short.MAX_VALUE.toInt())
            DATA_LAVA_PERCENT -> lava.percent
            DATA_LAVA_MB -> lava.mb
            else -> 0
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                DATA_SMELT_PROGRESS -> state.smeltProgress = value
                DATA_SMELT_TOTAL -> state.smeltTotal = value
                DATA_FUEL_LEFT -> state.fuelBurnTimeLeft = value
                DATA_FUEL_MAX -> state.maxFuelBurnTime = value
                // DATA_LAVA_PERCENT / DATA_LAVA_MB are server-derived and read-only on the client
            }
        }

        override fun getCount(): Int = DATA_COUNT
    }

    private fun serverTick(level: ServerLevel) {
        processBucketSlot()

        val inputStack = items[INPUT_SLOT]
        val recipeInput = FoundryRecipeInput(inputStack, lava.hasLava)

        val recipeHolder =
            level.recipeAccess().synchronizedRecipes
                .getFirstMatch(ModRecipes.FOUNDRY_RECIPE_TYPE, recipeInput, level)
                .orElse(null)

        if (tickProgress(recipeHolder?.value(), level)) setChanged()

        val shouldBeLit = state.isBurning
        if (blockState.getValue(AbstractFurnaceBlock.LIT) != shouldBeLit) {
            level.setBlock(blockPos, blockState.setValue(AbstractFurnaceBlock.LIT, shouldBeLit), 3)
        }
    }

    private fun processBucketSlot() {
        lava.tryConsumeBucket(items[LAVA_BUCKET_SLOT])?.let { items[LAVA_BUCKET_SLOT] = it }
    }

    private fun tickProgress(recipe: FoundryRecipe?, level: ServerLevel): Boolean {
        val canSmelt = recipe != null && canOutput(recipe)
        if (canSmelt && state.fuelBurnTimeLeft <= 0) tryStartFuel(level)

        val isHot = state.fuelBurnTimeLeft > 0
        if (!isHot) {
            if (state.smeltProgress > 0) {
                state.smeltProgress = (state.smeltProgress - PROGRESS_DECAY).coerceAtLeast(0)
                return true
            }
            return false
        }

        state.fuelBurnTimeLeft--
        val smelting = recipe ?: return true
        if (!canSmelt) return true

        if (state.smeltProgress == 0) state.smeltTotal = smelting.cookingTime * PROGRESS_RESOLUTION
        val hasLavaBoost = lava.hasLava
        val speed = state.fuelSpeed.coerceAtLeast(BASE_FUEL_SPEED)
        state.smeltProgress += if (hasLavaBoost) speed * LAVA_SPEED_MULTIPLIER else speed
        if (hasLavaBoost) lava.drainForBoost()

        if (state.smeltProgress >= state.smeltTotal) {
            state.smeltProgress = 0
            craftResult(smelting, level)
        }
        return true
    }

    private fun tryStartFuel(level: ServerLevel) {
        val fuel = items[FUEL_SLOT]
        if (fuel.isEmpty) return
        val burnTime = getFuelBurnTime(level, fuel)
        if (burnTime <= 0) return

        state.maxFuelBurnTime = burnTime
        state.fuelBurnTimeLeft = burnTime
        state.fuelSpeed = fuelSpeedFor(fuel)

        val remainder = fuel.item.craftingRemainder?.create() ?: ItemStack.EMPTY
        fuel.shrink(1)
        if (fuel.isEmpty && !remainder.isEmpty) {
            items[FUEL_SLOT] = remainder
        }
    }

    private fun getFuelBurnTime(level: ServerLevel, stack: ItemStack): Int {
        val vanillaTime = level.server.fuelValues().burnDuration(stack)
        if (vanillaTime > 0) return vanillaTime
        if (stack.`is`(ModItems.SLAG)) return 800

        return 0
    }

    private fun canFitInSlot(slotIndex: Int, item: ItemStack): Boolean {
        val slot = items[slotIndex]
        return when {
            slot.isEmpty -> true
            !ItemStack.isSameItemSameComponents(slot, item) -> false
            else -> slot.count + item.count <= slot.maxStackSize
        }
    }

    /** Merges [item] into [slotIndex] if it fits, returning whether it was placed. */
    private fun insertInto(slotIndex: Int, item: ItemStack): Boolean {
        if (!canFitInSlot(slotIndex, item)) return false
        val slot = items[slotIndex]
        if (slot.isEmpty) items[slotIndex] = item.copy() else slot.grow(item.count)
        return true
    }

    private fun addToResultSlots(item: ItemStack): Boolean =
        RESULT_SLOTS.any { insertInto(it, item) }

    private fun canOutput(recipe: FoundryRecipe): Boolean {
        if (items[INPUT_SLOT].isEmpty) return false

        // A pooled recipe's exact output isn't known until it's rolled, so require an empty
        // result slot to guarantee whatever is produced can be placed.
        val hasOutputRoom =
            if (recipe.isPooled) RESULT_SLOTS.any { items[it].isEmpty }
            else recipe.result.create().let { result -> RESULT_SLOTS.any { canFitInSlot(it, result) } }
        if (!hasOutputRoom) return false

        if (recipe.byproductChance > 0f) {
            val minSlag = recipe.byproductChance.toInt().coerceAtLeast(1)
            if (!canFitInSlot(BYPRODUCT_SLOT, ItemStack(ModItems.SLAG, minSlag))) return false
        }

        return true
    }

    private fun craftResult(recipe: FoundryRecipe, level: ServerLevel) {
        items[INPUT_SLOT].shrink(1)
        addToResultSlots(recipe.rollResult(level.random))
        state.storedXp += recipe.experience

        if (recipe.bonusResultChance > 0f) {
            val lavaPreconditionMet = !recipe.bonusRequiresLava || lava.hasLava
            val bonusRolled = level.random.nextFloat() < recipe.bonusResultChance
            if (lavaPreconditionMet && bonusRolled) addToResultSlots(recipe.rollResult(level.random))
        }

        // floor(byproductChance) = guaranteed slag; fraction = roll for +1 (supports >1 for bulk recipes)
        if (recipe.byproductChance > 0f) {
            val guaranteed = recipe.byproductChance.toInt()
            val fraction = recipe.byproductChance - guaranteed
            val count =
                guaranteed + if (fraction > 0f && level.random.nextFloat() < fraction) 1 else 0
            if (count > 0) insertInto(BYPRODUCT_SLOT, ItemStack(ModItems.SLAG, count))
        }
    }

    fun popExperience(serverLevel: ServerLevel) {
        val whole = state.storedXp.toInt()
        val frac = state.storedXp - whole
        val total = whole + if (serverLevel.random.nextFloat() < frac) 1 else 0
        state.storedXp = 0f
        if (total > 0) ExperienceOrb.award(serverLevel, Vec3.atCenterOf(blockPos), total)
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        ContainerHelper.saveAllItems(output, items)
        state.save(output)
        lava.save(output)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        ContainerHelper.loadAllItems(input, items)
        state.load(input)
        lava.load(input)
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registryLookup)

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getDisplayName(): Component = Component.translatable("block.foundry.foundry")

    override fun createMenu(
        containerId: Int, inventory: Inventory, player: Player
    ): AbstractContainerMenu = FoundryMenu(containerId, inventory, this, containerData)

    companion object {
        const val INPUT_SLOT = 0
        const val FUEL_SLOT = 1
        const val OUTPUT_SLOT = 2
        const val OUTPUT_SLOT_2 = 3
        const val OUTPUT_SLOT_3 = 4
        const val BYPRODUCT_SLOT = 5
        const val LAVA_BUCKET_SLOT = 6
        const val INVENTORY_SIZE = 7

        /**
         * Progress is tracked at [PROGRESS_RESOLUTION]× the recipe cook time so fractional speed
         * multipliers stay whole numbers. Per-tick progress by fuel (before the lava multiplier):
         * coal/charcoal → 1.5×, blaze rod → 3×. Lava in the tank doubles whichever is active.
         */
        const val PROGRESS_RESOLUTION = 2
        const val PROGRESS_DECAY = 2
        const val BASE_FUEL_SPEED = 3    // 1.5× × PROGRESS_RESOLUTION
        const val BLAZE_FUEL_SPEED = 6   // 3×   × PROGRESS_RESOLUTION
        const val LAVA_SPEED_MULTIPLIER = 2

        /** Per-tick smelting speed granted by [stack] as fuel, before the lava multiplier. */
        fun fuelSpeedFor(stack: ItemStack): Int =
            if (stack.`is`(Items.BLAZE_ROD)) BLAZE_FUEL_SPEED else BASE_FUEL_SPEED

        /** The three result slots, filled left-to-right. */
        val RESULT_SLOTS = intArrayOf(OUTPUT_SLOT, OUTPUT_SLOT_2, OUTPUT_SLOT_3)

        /** Slots a hopper may pull from (bottom face). */
        val EXTRACT_SLOTS = intArrayOf(OUTPUT_SLOT, OUTPUT_SLOT_2, OUTPUT_SLOT_3, BYPRODUCT_SLOT)

        private val SLOTS_TOP = intArrayOf(INPUT_SLOT)
        private val SLOTS_SIDE = intArrayOf(FUEL_SLOT, LAVA_BUCKET_SLOT)

        // ContainerData indices — shared between this entity and FoundryMenu.
        const val DATA_SMELT_PROGRESS = 0
        const val DATA_SMELT_TOTAL = 1
        const val DATA_FUEL_LEFT = 2
        const val DATA_FUEL_MAX = 3
        const val DATA_LAVA_PERCENT = 4
        const val DATA_LAVA_MB = 5
        const val DATA_COUNT = 6

        fun isSmeltable(level: Level, stack: ItemStack): Boolean {
            val input = FoundryRecipeInput(stack, false)
            return level.recipeAccess()
                .synchronizedRecipes
                .getFirstMatch(ModRecipes.FOUNDRY_RECIPE_TYPE, input, level)
                .isPresent
        }

        @JvmStatic
        fun tick(level: Level, entity: FoundryBlockEntity) {
            if (level.isClientSide) return
            entity.serverTick(level as ServerLevel)
        }
    }
}
