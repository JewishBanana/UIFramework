package com.jewishbanana.uiframework.items;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.listeners.menus.ItemsMenu;
import com.jewishbanana.uiframework.listeners.menus.MenuManager;
import com.jewishbanana.uiframework.utils.ItemBuilder;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class RecipeBook extends GenericItem {

	public RecipeBook(ItemStack item) {
		super(item);
	}
	public boolean interacted(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemsMenu menu = new ItemsMenu(1);
			MenuManager.registerInventory(menu.getInventory(), menu);
			e.getPlayer().openInventory(menu.getInventory());
			return true;
		}
		return false;
	}
	@Override
	public ItemBuilder createItem() {
		return ItemBuilder.create(Material.BOOK).registerName(UIFUtils.convertString(UIFramework.getLangString("items.recipeBook"))).setLoreList(Arrays.asList(UIFUtils.convertString(UIFramework.getLangString("items.recipeBookLore")))).build();
	}
}
