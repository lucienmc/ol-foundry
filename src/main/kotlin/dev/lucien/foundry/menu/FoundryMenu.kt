package dev.lucien.foundry.menu

import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.recipe.FoundryRecipeInput
import dev.lucien.foundry.registry.ModMenuTypes
import dev.lucien.foundry.registry.ModRecipes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.RecipeManager

class FoundryMenu private constructor(
    containerId: Int,
    private val inventory: Inventory,
    private val container: Container,
    private val data: ContainerData,
    private val foundry: FoundryBlockEntity?
) : AbstractContainerMenu(ModMenuTypes.FOUNDRY, containerId) {

    constructor(containerId: Int, inventory: Inventory) : this(
        containerId,
        inventory,
        SimpleContainer(FoundryBlockEntity.INVENTORY_SIZE),
        SimpleContainerData(6),
        null
    )

    constructor(
        containerId: Int, inventory: Inventory, blockEntity: FoundryBlockEntity, data: ContainerData
    ) : this(containerId, inventory, blockEntity as Container, data, blockEntity)

    init {
        checkContainerSize(container, FoundryBlockEntity.INVENTORY_SIZE)
        container.startOpen(inventory.player)

        // ── Input / fuel ──────────────────────────────────────────────────────
        addSlot(object : Slot(container, FoundryBlockEntity.INPUT_SLOT, INPUT_X + 1, INPUT_Y + 1) {
            override fun mayPlace(stack: ItemStack) = isSmeltable(stack)
        })
        addSlot(Slot(container, FoundryBlockEntity.FUEL_SLOT, FUEL_X + 1, FUEL_Y + 1))

        // ── Three output slots (fills left-to-right) ──────────────────────────
        addSlot(object :
            Slot(container, FoundryBlockEntity.OUTPUT_SLOT, OUTPUT1_X + 1, OUTPUT1_Y + 1) {
            override fun mayPlace(stack: ItemStack) = false
            override fun onTake(player: Player, stack: ItemStack) {
                if (!player.level().isClientSide) foundry?.popExperience(player.level() as ServerLevel)
                super.onTake(player, stack)
            }
        })
        addSlot(object :
            Slot(container, FoundryBlockEntity.OUTPUT_SLOT_2, OUTPUT2_X + 1, OUTPUT2_Y + 1) {
            override fun mayPlace(stack: ItemStack) = false
            override fun onTake(player: Player, stack: ItemStack) {
                if (!player.level().isClientSide) foundry?.popExperience(player.level() as ServerLevel)
                super.onTake(player, stack)
            }
        })
        addSlot(object :
            Slot(container, FoundryBlockEntity.OUTPUT_SLOT_3, OUTPUT3_X + 1, OUTPUT3_Y + 1) {
            override fun mayPlace(stack: ItemStack) = false
            override fun onTake(player: Player, stack: ItemStack) {
                if (!player.level().isClientSide) foundry?.popExperience(player.level() as ServerLevel)
                super.onTake(player, stack)
            }
        })

        // ── Byproduct (slag) below Output3 ───────────────────────────────────
        addSlot(object :
            Slot(container, FoundryBlockEntity.BYPRODUCT_SLOT, BYPRODUCT_X + 1, BYPRODUCT_Y + 1) {
            override fun mayPlace(stack: ItemStack) = false
        })

        // ── Lava-bucket slot (below gauge, far left) ──────────────────────────
        addSlot(object : Slot(
            container, FoundryBlockEntity.LAVA_BUCKET_SLOT, LAVA_BUCKET_X + 1, LAVA_BUCKET_Y + 1
        ) {
            override fun mayPlace(stack: ItemStack) = stack.`is`(Items.LAVA_BUCKET)
            override fun getMaxStackSize() = 1
        })

        addStandardInventorySlots(inventory, PLAYER_INV_X, PLAYER_INV_Y)
        addDataSlots(data)
    }

    // ── Recipe check ──────────────────────────────────────────────────────────

    private fun isSmeltable(stack: ItemStack): Boolean {
        val level = inventory.player.level()
        val manager = level.recipeAccess() as? RecipeManager ?: return true
        return manager.getRecipeFor(
            ModRecipes.FOUNDRY_RECIPE_TYPE, FoundryRecipeInput(stack, false), level
        ).isPresent
    }

    // ── Progress accessors (used by FoundryScreen) ────────────────────────────

    fun getSmeltProgress(): Int = data.get(0)
    fun getSmeltTotal(): Int = data.get(1).coerceAtLeast(1)
    fun getFuelBurnLeft(): Int = data.get(2)
    fun getFuelBurnMax(): Int = data.get(3).coerceAtLeast(1)
    fun getLavaPercent(): Int = data.get(4)
    fun getLavaMb(): Int = data.get(5)   // 0..4000 mB
    fun isBurning(): Boolean = getFuelBurnLeft() > 0

    // ── Shift-click routing ───────────────────────────────────────────────────

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY

        val stack = slot.getItem()
        val original = stack.copy()

        val containerEnd = FoundryBlockEntity.INVENTORY_SIZE
        val inventoryEnd = slots.size

        if (slotIndex < containerEnd) {
            if (!moveItemStackTo(stack, containerEnd, inventoryEnd, true)) return ItemStack.EMPTY
        } else {
            val moved = when {
                stack.`is`(Items.LAVA_BUCKET) -> moveItemStackTo(
                    stack,
                    FoundryBlockEntity.LAVA_BUCKET_SLOT,
                    FoundryBlockEntity.LAVA_BUCKET_SLOT + 1,
                    false
                )

                isSmeltable(stack) -> moveItemStackTo(
                    stack, FoundryBlockEntity.INPUT_SLOT, FoundryBlockEntity.INPUT_SLOT + 1, false
                )

                else -> moveItemStackTo(
                    stack, FoundryBlockEntity.FUEL_SLOT, FoundryBlockEntity.FUEL_SLOT + 1, false
                )
            }
            if (!moved) return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        if (stack.count == original.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return original
    }

    override fun stillValid(player: Player): Boolean = container.stillValid(player)

    override fun removed(player: Player) {
        super.removed(player)
        container.stopOpen(player)
    }

    // ── Layout constants — single source of truth for Menu AND Screen ─────────
    //
    // The Screen imports FoundryMenu already, so it reads FoundryMenu.INPUT_X etc.
    // Changing a position here automatically fixes both the slot hitbox and the
    // rendered slot outline — they can never drift apart.

    companion object {

        // Slot positions (top-left corner of the 18×18 slot area, GUI-relative)
        const val INPUT_X = 25
        const val INPUT_Y = 16
        const val FUEL_X = 25
        const val FUEL_Y = 52
        const val OUTPUT1_X = 80
        const val OUTPUT1_Y = 24
        const val OUTPUT2_X = 98
        const val OUTPUT2_Y = 24
        const val OUTPUT3_X = 116
        const val OUTPUT3_Y = 24
        const val BYPRODUCT_X = 98
        const val BYPRODUCT_Y = 44
        const val LAVA_BUCKET_X = 151
        const val LAVA_BUCKET_Y = 59

        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 84

        // Flame indicator (between input slot and fuel slot)
        const val FLAME_X = 26
        const val FLAME_Y = 36
        const val FLAME_W = 13
        const val FLAME_H = 14

        // Progress arrow
        const val ARROW_X = 50
        const val ARROW_Y = 35
        const val ARROW_W = 22
        const val ARROW_H = 15

        // Lava gauge (far-left column, above the lava-bucket slot)
        const val BAR_X = 152
        const val BAR_Y = 7
        const val BAR_W = 16
        const val BAR_H = 50
    }
}
