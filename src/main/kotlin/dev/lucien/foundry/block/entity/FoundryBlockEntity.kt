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
        Direction.UP -> intArrayOf(INPUT_SLOT)
        Direction.DOWN -> intArrayOf(OUTPUT_SLOT, OUTPUT_SLOT_2, OUTPUT_SLOT_3, BYPRODUCT_SLOT)
        else -> intArrayOf(FUEL_SLOT, LAVA_BUCKET_SLOT)
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
    ): Boolean =
        index == OUTPUT_SLOT || index == OUTPUT_SLOT_2 || index == OUTPUT_SLOT_3 || index == BYPRODUCT_SLOT

    val lava = FoundryLavaTank { setChanged() }
    val state = FoundryState()

    val containerData: ContainerData = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            0 -> state.smeltProgress
            1 -> state.smeltTotal
            2 -> state.fuelBurnTimeLeft.coerceAtMost(Short.MAX_VALUE.toInt())
            3 -> state.maxFuelBurnTime.coerceAtMost(Short.MAX_VALUE.toInt())
            4 -> lava.percent
            5 -> lava.mb
            else -> 0
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                0 -> state.smeltProgress = value
                1 -> state.smeltTotal = value
                2 -> state.fuelBurnTimeLeft = value
                3 -> state.maxFuelBurnTime = value
                // indices 4 and 5 are read-only on client
            }
        }

        override fun getCount(): Int = 6
    }

    private fun serverTick(level: ServerLevel) {
        processBucketSlot()

        val inputStack = items[INPUT_SLOT]
        val recipeInput = FoundryRecipeInput(inputStack, lava.hasLava)

        val recipeHolder =
            level.recipeAccess().getSynchronizedRecipes()
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
                state.smeltProgress = (state.smeltProgress - 2).coerceAtLeast(0)
                return true
            }
            return false
        }

        state.fuelBurnTimeLeft--
        val smelting = recipe ?: return true
        if (!canSmelt) return true

        if (state.smeltProgress == 0) state.smeltTotal = smelting.cookingTime
        val hasLavaBoost = lava.hasLava
        state.smeltProgress += if (hasLavaBoost) 4 else 2
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

    private fun addToOutputSlots(item: ItemStack): Boolean {
        for (slotIndex in intArrayOf(OUTPUT_SLOT, OUTPUT_SLOT_2, OUTPUT_SLOT_3)) {
            val slot = items[slotIndex]
            when {
                slot.isEmpty -> {
                    items[slotIndex] = item.copy(); return true
                }

                ItemStack.isSameItemSameComponents(
                    slot, item
                ) && slot.count + item.count <= slot.maxStackSize -> {
                    slot.grow(item.count); return true
                }
            }
        }
        return false
    }

    private fun canOutput(recipe: FoundryRecipe): Boolean {
        if (items[INPUT_SLOT].isEmpty) return false
        val result = recipe.result.create()

        // At least one of the three output slots must be able to accept the result
        val canFitOutput = canFitInSlot(OUTPUT_SLOT, result) || canFitInSlot(
            OUTPUT_SLOT_2, result
        ) || canFitInSlot(OUTPUT_SLOT_3, result)
        if (!canFitOutput) return false

        if (recipe.byproductChance > 0f) {
            val minSlag = recipe.byproductChance.toInt().coerceAtLeast(1)
            val slag = ItemStack(ModItems.SLAG, minSlag)
            if (!canFitInSlot(BYPRODUCT_SLOT, slag)) return false
        }

        return true
    }

    private fun craftResult(recipe: FoundryRecipe, level: ServerLevel) {
        val result = recipe.result.create()

        items[INPUT_SLOT].shrink(1)
        addToOutputSlots(result)
        state.storedXp += recipe.experience

        if (recipe.bonusResultChance > 0f) {
            val lavaPreconditionMet = !recipe.bonusRequiresLava || lava.hasLava
            val bonusRolled = level.random.nextFloat() < recipe.bonusResultChance
            if (lavaPreconditionMet && bonusRolled) addToOutputSlots(result.copy())
        }

        // floor(byproductChance) = guaranteed slag; fraction = roll for +1 (supports >1 for bulk recipes)
        if (recipe.byproductChance > 0f) {
            val guaranteed = recipe.byproductChance.toInt()
            val fraction = recipe.byproductChance - guaranteed
            val count =
                guaranteed + if (fraction > 0f && level.random.nextFloat() < fraction) 1 else 0
            if (count > 0) {
                val slag = ItemStack(ModItems.SLAG)
                val byproduct = items[BYPRODUCT_SLOT]
                when {
                    byproduct.isEmpty -> items[BYPRODUCT_SLOT] = ItemStack(ModItems.SLAG, count)

                    ItemStack.isSameItemSameComponents(
                        byproduct, slag
                    ) && byproduct.count + count <= byproduct.maxStackSize -> byproduct.grow(count)
                }
            }
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
