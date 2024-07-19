package com.github.jewishbanana.uiframework.listeners.menus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class InventoryHandler {
	
	protected Inventory inventory;
	protected Map<Integer, InventoryButton> buttons = new HashMap<>();
	
	public void decorate() {
		buttons.forEach((k, v) -> inventory.setItem(k, v.getItem()));
	}
	public void onOpen(InventoryOpenEvent event) {
	}
	public void onClose(InventoryCloseEvent event) {
	}
	public void onClick(InventoryClickEvent event) {
		event.setCancelled(true);
		int slot = event.getRawSlot();
		InventoryButton button = buttons.get(slot);
		if (button != null)
			button.getFunction().accept(event);
	}
	public void addButton(int slot, InventoryButton button) {
		this.buttons.put(slot, button);
	}
	
	public abstract Inventory createInventory();
	
	public Inventory getInventory() {
		return this.inventory;
	}
	
	public class InventoryButton {
		
		private ItemStack item;
		private Consumer<InventoryClickEvent> consumer;
		
		public InventoryButton create(ItemStack item) {
			this.item = item;
			return this;
		}
		public InventoryButton function(Consumer<InventoryClickEvent> consumer) {
			this.consumer = consumer;
			return this;
		}
		public ItemStack getItem() {
			return item;
		}
		public Consumer<InventoryClickEvent> getFunction() {
			return consumer;
		}
	}
}