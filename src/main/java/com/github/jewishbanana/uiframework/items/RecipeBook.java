package com.github.jewishbanana.uiframework.items;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.listeners.menus.ItemsMenu;
import com.github.jewishbanana.uiframework.listeners.menus.MenuManager;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class RecipeBook extends GenericItem {

	public RecipeBook(ItemStack item) {
		super(item);
	}
	public boolean interacted(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemsMenu menu = new ItemsMenu(1, e.getPlayer().hasPermission("ultimateitems.modifyRecipes"));
			MenuManager.registerInventory(menu.getInventory(), menu);
			e.getPlayer().openInventory(menu.getInventory());
			return true;
		}
		return false;
	}
	@Override
	public ItemBuilder createItem() {
		getType().setDisplayName(UIFUtils.convertString(UIFramework.getLangString("items.recipeBook")));
		getType().setLore(Arrays.asList(UIFUtils.convertString(UIFramework.getLangString("items.recipeBookLore"))));
		return ItemBuilder.create(getType(), Material.BOOK).registerName(getType().getDisplayName()).assembleLore().build();
	}
	public ItemCategory getItemCategory() {
		return ItemCategory.DefaultCategory.MISCELLANEOUS.getItemCategory();
	}
}
