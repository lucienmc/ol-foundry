package dev.lucien.foundry.menu

import dev.lucien.foundry.block.entity.FoundryBlockEntity
import dev.lucien.foundry.registry.ModMenuTypes
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class FoundryMenu private constructor(
    containerId: Int,
    private val inventory: Inventory,
    private val container: Container,
    private val data: ContainerData
) : AbstractContainerMenu(ModMenuTypes.FOUNDRY, containerId) {

    // ── Client-side constructor (called by MenuType factory) ──────────────────
    constructor(containerId: Int, inventory: Inventory) : this(
        containerId,
        inventory,
        SimpleContainer(FoundryBlockEntity.INVENTORY_SIZE),
        SimpleContainerData(5)
    )

    // ── Server-side constructor (called from FoundryBlockEntity.createMenu) ───
    constructor(
        containerId: Int,
        inventory: Inventory,
        blockEntity: FoundryBlockEntity,
        data: ContainerData
    ) : this(containerId, inventory, blockEntity as Container, data)

    init {
        checkContainerSize(container, FoundryBlockEntity.INVENTORY_SIZE)
        container.startOpen(inventory.player)

        // Foundry slots
        addSlot(Slot(container, FoundryBlockEntity.INPUT_SLOT,     56, 17))   // Input
        addSlot(Slot(container, FoundryBlockEntity.FUEL_SLOT,      56, 53))   // Fuel
        addSlot(Slot(container, FoundryBlockEntity.OUTPUT_SLOT,   116, 26)) // Output
        addSlot(Slot(container, FoundryBlockEntity.BYPRODUCT_SLOT, 116, 50)) // Byproduct (slag)
        // Lava bucket slot: accepts only lava buckets, stacks of 1
        addSlot(object : Slot(container, FoundryBlockEntity.LAVA_BUCKET_SLOT, 151, 59) {
            override fun mayPlace(stack: ItemStack) = stack.`is`(Items.LAVA_BUCKET)
            override fun getMaxStackSize() = 1
        })

        // Player inventory (27 slots + 9 hotbar)
        addStandardInventorySlots(inventory, 8, 84)

        // Sync progress data
        addDataSlots(data)
    }

    // ── Progress accessors (used by FoundryScreen) ────────────────────────────

    fun getSmeltProgress(): Int    = data.get(0)
    fun getSmeltTotal(): Int       = data.get(1).coerceAtLeast(1)
    fun getFuelBurnLeft(): Int     = data.get(2)
    fun getFuelBurnMax(): Int      = data.get(3).coerceAtLeast(1)
    fun getLavaPercent(): Int      = data.get(4)   // 0-100
    fun isBurning(): Boolean       = getFuelBurnLeft() > 0

    // ── Shift-click logic ─────────────────────────────────────────────────────

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY

        val stack    = slot.getItem()
        val original = stack.copy()

        val containerEnd = FoundryBlockEntity.INVENTORY_SIZE
        val inventoryEnd = slots.size

        if (slotIndex < containerEnd) {
            // Container → player inventory
            if (!moveItemStackTo(stack, containerEnd, inventoryEnd, true)) {
                return ItemStack.EMPTY
            }
        } else {
            // Player inventory → container: lava bucket → bucket slot; then input; then fuel
            val movedToBucket = stack.`is`(Items.LAVA_BUCKET) &&
                moveItemStackTo(stack, FoundryBlockEntity.LAVA_BUCKET_SLOT, FoundryBlockEntity.LAVA_BUCKET_SLOT + 1, false)
            if (!movedToBucket) {
                if (!moveItemStackTo(stack, FoundryBlockEntity.INPUT_SLOT, FoundryBlockEntity.INPUT_SLOT + 1, false)) {
                    if (!moveItemStackTo(stack, FoundryBlockEntity.FUEL_SLOT, FoundryBlockEntity.FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY
                    }
                }
            }
        }

        if (stack.isEmpty) {
            slot.setByPlayer(ItemStack.EMPTY)
        } else {
            slot.setChanged()
        }

        if (stack.count == original.count) return ItemStack.EMPTY
        slot.onTake(player, stack)

        return original
    }

    override fun stillValid(player: Player): Boolean =
        container.stillValid(player)

    override fun removed(player: Player) {
        super.removed(player)
        container.stopOpen(player)
    }
}
