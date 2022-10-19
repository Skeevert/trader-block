package net.fabricmc.traderblockmod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.village.Merchant;

public class TraderBlockScreenHandler extends MerchantScreenHandler {
	public TraderBlockScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(syncId, playerInventory);
	}
	
	public TraderBlockScreenHandler(int syncId, PlayerInventory playerInventory, Merchant merchant) {
		super(syncId, playerInventory, merchant);
	}
	
	/*
	 * You may ask, why the hell I have to derive from MerchantScreenHandler class in general and override this method in particular.
	 * Well, the problem with the original transferSlot method is that it calls the private method playYesSound, which assumes that the caller is entity.
	 * Which, as you may guess, is incorrect in case of this mod.
	 * 
	 * So I have to almost copy a method, but without a call to playYesSound. Yay.
	 */
	
	// TODO: Check if this could be refactored to a more readable form.
	@Override
	public ItemStack transferSlot(PlayerEntity player, int index) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot = (Slot)this.slots.get(index);
		
		if (slot == null || !slot.hasStack()) {
			return itemStack;
		}
		
		ItemStack itemStack2 = slot.getStack();
		itemStack = itemStack2.copy();
		
		if (index == 2) {
			if (!this.insertItem(itemStack2, 3, 39, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickTransfer(itemStack2, itemStack);
		} else if (index == 0 || index == 1 ? !this.insertItem(itemStack2, 3, 39, false) : (index >= 3 && index < 30 ? !this.insertItem(itemStack2, 30, 39, false) : index >= 30 && index < 39 && !this.insertItem(itemStack2, 3, 30, false))) {
			return ItemStack.EMPTY;
		}
		
		if (itemStack2.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		
		if (itemStack2.getCount() == itemStack.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTakeItem(player, itemStack2);
		
		return itemStack;
	}
}
