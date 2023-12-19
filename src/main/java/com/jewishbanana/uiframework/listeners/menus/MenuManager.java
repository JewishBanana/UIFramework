package com.jewishbanana.uiframework.listeners.menus;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import com.jewishbanana.uiframework.UIFramework;

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
	public static void registerInventory(Inventory inventory, InventoryHandler handler) {
		inventories.put(inventory, handler);
	}
}
