package com.github.jewishbanana.uiframework.listeners.menus;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.ItemBuilder;
import com.github.jewishbanana.uiframework.items.UIItemType;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

import net.md_5.bungee.api.ChatColor;

public class ItemsMenu extends InventoryHandler {
	
	private static String categoryTranslation;

	private int page;
	private boolean adminControls;
	
	public ItemsMenu(int page, boolean adminControls) {
		this.inventory = this.createInventory();
		this.page = page;
		this.adminControls = adminControls;
		this.decorate();
	}
	public void decorate() {
		ItemStack whiteGlass = ItemBuilder.create(Material.WHITE_STAINED_GLASS_PANE).registerName(" ").build().getItem();
		for (int i=0; i < 45; i++)
			if (i < 10 || i >= 35 || (i >= 17 && i <= 18) || (i >= 26 && i <= 27))
				this.getInventory().setItem(i, whiteGlass);
		this.getInventory().setItem(4, ItemBuilder.create(Material.CRAFTING_TABLE).registerName(UIFUtils.convertString("&aUltimateItems "+UIFramework.getLangString("menu.recipe"))).setLoreList(Arrays.asList(UIFUtils.convertString(UIFramework.getLangString("menu.recipeInfo")))).build().getItem());
		int start = (page - 1) * 21, end = page * 21, i = 0, c = 10;
		for (Entry<String, UIItemType> entry : UIItemType.getRegistry().entrySet().stream().sorted(Comparator.comparing(entry -> entry.getValue().getItemCategory().getCategoryValue()))
	            .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)).entrySet()) {
			if (entry.getKey().equals("_null"))
				continue;
			if (i < start) {
		        i++;
		        continue;
		    }
		    if (i >= end)
		        break;
			if (i != start && i % 7 == 0)
				c += 2;
			GenericItem base = GenericItem.getItemBaseNoID(entry.getValue().getItem());
			ItemStack temp = base.stripItemID().getItem();
			ItemMeta tempMeta = temp.getItemMeta();
			List<String> lore = tempMeta.getLore();
			lore.addAll(Arrays.asList(" ",
					categoryTranslation + base.getItemCategory().getDisplayName(),
					" ",
					(base.getType().getRecipes().isEmpty() ? ChatColor.RED : ChatColor.GREEN)+UIFramework.getLangString("menu.creationRecipes").replace("%value%", ""+base.getType().getRecipes().size()),
					(base.getType().getUsedRecipes().isEmpty() ? ChatColor.RED : ChatColor.BLUE)+UIFramework.getLangString("menu.usedRecipes").replace("%value%", ""+base.getType().getUsedRecipes().size())));
			if (adminControls)
				lore.add(UIFUtils.convertString(UIFramework.getLangString("menu.giveItem")));
			tempMeta.setLore(lore);
			temp.setItemMeta(tempMeta);
			this.addButton(c, new InventoryButton().create(temp).function(event -> {
				if (adminControls && event.getClick() == ClickType.RIGHT && event.getWhoClicked() instanceof Player) {
					Player p = (Player) event.getWhoClicked();
					if (p.getInventory().firstEmpty() == -1)
						p.sendMessage(UIFUtils.convertString("&cYour inventory is full!"));
					else {
						ItemStack item = entry.getValue().getItem();
						GenericItem giveBase = GenericItem.getItemBase(item);
						giveBase.refreshItemLore();
						p.getInventory().addItem(giveBase.getItem());
						p.sendMessage(UIFUtils.convertString("&aGave &b1 &a["+giveBase.getDisplayName()+"&a] to "+p.getName()));
						p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
					}
					return;
				}
				RecipeMenu menu = new RecipeMenu(entry.getValue(), base.getDisplayName(), 1, page, adminControls);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
			c++;
			i++;
		}
		if (page > 1)
			this.addButton(45, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.page").replace("%number%", (page-1)+""))).build().getItem()).function(event -> {
				ItemsMenu menu = new ItemsMenu(page-1, adminControls);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		if (page < Math.ceil(((double) UIItemType.getRegistry().size()) / 21.0))
			this.addButton(53, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.page").replace("%number%", (page+1)+""))).build().getItem()).function(event -> {
				ItemsMenu menu = new ItemsMenu(page+1, adminControls);
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
	public static void reload() {
		categoryTranslation = UIFUtils.convertString("&f&l"+UIFramework.getLangString("menu.category"))+": ";
	}
}
