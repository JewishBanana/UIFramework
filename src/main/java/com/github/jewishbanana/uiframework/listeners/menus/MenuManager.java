package com.github.jewishbanana.uiframework.listeners.menus;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;

import com.github.jewishbanana.uiframework.UIFramework;

public class MenuManager implements Listener {
	
	private static Map<Inventory, InventoryHandler> inventories = new HashMap<>();
	
	public MenuManager(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onOpen(InventoryOpenEvent e) {
		InventoryHandler handler = inventories.get(e.getInventory());
		if (handler != null)
			handler.onOpen(e);
	}
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		InventoryHandler handler = inventories.get(e.getInventory());
		if (handler != null) {
			handler.onClose(e);
			inventories.remove(e.getInventory());
		}
	}
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		InventoryHandler handler = inventories.get(e.getInventory());
		if (handler != null)
			handler.onClick(e);
	}
	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		InventoryHandler handler = inventories.get(e.getInventory());
		if (handler != null)
			handler.onDrag(e);
	}
	@EventHandler
	public void onPrepareAnvil(PrepareAnvilEvent e) {
		InventoryHandler handler = inventories.get(e.getView().getTopInventory());
		if (handler != null)
			handler.onPrepareAnvil(e);
	}
	public static void registerInventory(Inventory inventory, InventoryHandler handler) {
		inventories.put(inventory, handler);
	}
	public static boolean isRegisteredInventory(Inventory inventory) {
		return inventories.containsKey(inventory);
	}
}
