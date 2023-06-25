package com.jewishbanana.uiframework.listeners.menus;

import java.util.Arrays;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.ItemType;
import com.jewishbanana.uiframework.utils.ItemBuilder;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class ItemsMenu extends InventoryHandler {

	private int page;
	
	public ItemsMenu(int page) {
		this.inventory = this.createInventory();
		this.page = page;
		this.decorate();
	}
	public void decorate() {
		ItemStack whiteGlass = ItemBuilder.create(Material.WHITE_STAINED_GLASS_PANE).registerName(" ").build().getItem();
		for (int i=0; i < 45; i++)
			if (i < 10 || i >= 35 || (i >= 17 && i <= 18) || (i >= 26 && i <= 27))
				this.getInventory().setItem(i, whiteGlass);
		this.getInventory().setItem(4, ItemBuilder.create(Material.CRAFTING_TABLE).registerName(UIFUtils.convertString("&aUltimateItems "+UIFramework.getLangString("menu.recipe"))).setLoreList(Arrays.asList(UIFUtils.convertString(UIFramework.getLangString("menu.recipeInfo")))).build().getItem());
		int end = page * 21, i = 0, c = 10;
		for (Entry<String, ItemType> entry : ItemType.getItemsMap().entrySet()) {
			if (i >= end)
				break;
			else if (i < end-21) {
				i++;
				continue;
			}
			if (i != 0 && i % 7 == 0)
				c += 2;
			this.addButton(c, new InventoryButton().create(entry.getValue().getBuilder().getItem()).function(event -> {
				RecipeMenu menu = new RecipeMenu(entry.getValue(), 1, page, event.getWhoClicked().hasPermission("ultimateitems.modifyRecipes"));
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
			c++;
			i++;
		}
		if (page > 1)
			this.addButton(45, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.page").replace("%number%", (page-1)+""))).build().getItem()).function(event -> {
				ItemsMenu menu = new ItemsMenu(page-1);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		if (page < Math.ceil(((double) ItemType.getItemsMap().size()) / 36.0))
			this.addButton(53, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.page").replace("%number%", (page+1)+""))).build().getItem()).function(event -> {
				ItemsMenu menu = new ItemsMenu(page+1);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		super.decorate();
	}
	public void onClick(InventoryClickEvent event) {
		int slot = event.getRawSlot();
		if (slot > 53 && !(event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT))
			return;
		event.setCancelled(true);
		InventoryButton button = buttons.get(slot);
		if (button != null) {
			button.getFunction().accept(event);
			return;
		}
	}
	@Override
	public Inventory createInventory() {
		return Bukkit.createInventory(null, 54, UIFUtils.convertString("&9&lUltimateItems "+UIFramework.getLangString("menu.recipe")));
	}
}
