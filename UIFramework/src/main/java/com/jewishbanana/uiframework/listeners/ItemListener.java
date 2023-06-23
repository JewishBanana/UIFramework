package com.jewishbanana.uiframework.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.GenericItem;

public class ItemListener implements Listener {
	
	private Map<ItemStack, Double> durabilityMap = new HashMap<>();

	public ItemListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(InventoryClickEvent e) {
		if (e.isCancelled())
			return;
		if (e.getCurrentItem() != null) {
			GenericItem item = GenericItem.getItemBase(e.getCursor());
			if (item != null)
				item.getId().getBuilder().assembleLore(e.getCursor(), e.getCursor().getItemMeta(), item.getId());
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemDamage(PlayerItemDamageEvent e) {
		if (e.isCancelled())
			return;
		GenericItem item = GenericItem.getItemBase(e.getItem());
		if (item != null) {
			e.setCancelled(true);
			double damage = ((((double) e.getItem().getType().getMaxDurability()) / item.getId().getDurability()) * ((double) e.getDamage())) + (durabilityMap.containsKey(e.getItem()) ? durabilityMap.get(e.getItem()) : 0.0);
			int realDamage = 0;
			if (damage >= 1.0) {
				while (damage >= 1.0) {
					realDamage++;
					damage -= 1.0;
				}
				Damageable meta = (Damageable) e.getItem().getItemMeta();
				if (meta.getDamage()+realDamage > e.getItem().getType().getMaxDurability()) {
					e.setCancelled(false);
					return;
				}
				meta.setDamage(meta.getDamage()+realDamage);
				e.getItem().setItemMeta((ItemMeta) meta);
				item.setItem(e.getItem());
			}
			durabilityMap.put(e.getItem(), damage);
		}
	}
	@EventHandler
	public void onItemBreak(PlayerItemBreakEvent e) {
		GenericItem.removeBaseItem(e.getBrokenItem());
		durabilityMap.remove(e.getBrokenItem());
	}
}
