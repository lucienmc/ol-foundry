package dev.lucien.foundry.util

import net.minecraft.core.NonNullList
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * A helper interface that provides default implementations of [Container]
 * backed by a [NonNullList]. Implementing classes only need to provide [getItems].
 */
interface ImplementedContainer : Container {
    fun getItems(): NonNullList<ItemStack>

    override fun getContainerSize(): Int = getItems().size

    override fun isEmpty(): Boolean = getItems().all { it.isEmpty }

    override fun getItem(slot: Int): ItemStack = getItems()[slot]

    override fun removeItem(slot: Int, amount: Int): ItemStack =
        ContainerHelper.removeItem(getItems(), slot, amount)

    override fun removeItemNoUpdate(slot: Int): ItemStack =
        ContainerHelper.takeItem(getItems(), slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        getItems()[slot] = stack
        if (stack.count > getMaxStackSize(stack)) {
            stack.count = getMaxStackSize(stack)
        }
        setChanged()
    }

    override fun clearContent() {
        for (i in getItems().indices) {
            getItems()[i] = ItemStack.EMPTY
        }
    }

    override fun stillValid(player: Player): Boolean = true
}
