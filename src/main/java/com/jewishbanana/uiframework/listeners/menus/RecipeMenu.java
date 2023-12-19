package com.jewishbanana.uiframework.listeners.menus;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.RecipeChoice.ExactChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.scheduler.BukkitTask;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.ItemType;
import com.jewishbanana.uiframework.utils.ItemBuilder;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class RecipeMenu extends InventoryHandler {

	private static UIFramework plugin;
	static {
		plugin = UIFramework.getInstance();
	}
	private ItemType type;
	public int page, memoryPage;
	private Map<Integer, Queue<ItemStack>> choiceMap = new HashMap<>();
	private BukkitTask task;
	public boolean adminControls;
	
	public RecipeMenu(ItemType type, int page, int memoryPage, boolean adminControls) {
		this.type = type;
		this.inventory = this.createInventory();
		this.page = page;
		this.memoryPage = memoryPage;
		this.adminControls = adminControls;
		this.decorate();
	}
	public void decorate() {
		ItemStack whiteGlass = ItemBuilder.create(Material.WHITE_STAINED_GLASS_PANE).registerName(" ").build().getItem();
		for (int i=0; i < 45; i++)
			this.getInventory().setItem(i, whiteGlass);
		if (!type.recipes.isEmpty() && page <= type.recipes.size()) {
			Recipe original = type.recipes.get(page-1);
			if (original instanceof ShapedRecipe) {
				ShapedRecipe shapedRecipe = (ShapedRecipe) original;
				ItemStack greenGlass = ItemBuilder.create(Material.LIME_STAINED_GLASS_PANE).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.creates"))).build().getItem();
				for (int i=0; i < 45; i++)
					if (i % 9 < 5)
						this.getInventory().setItem(i, greenGlass);
				this.getInventory().setItem(23, greenGlass);
				this.getInventory().setItem(24, greenGlass);
				this.getInventory().setItem(25, shapedRecipe.getResult());
				for (int i=0; i < 3; i++) {
					String s = shapedRecipe.getShape().length > i ? shapedRecipe.getShape()[i] : "   ";
					for (int j=0; j < 3; j++) {
						int slot = ((i+1)*9)+j+1;
						char c = s.charAt(j);
						if (c == ' ')
							this.getInventory().setItem(slot, new ItemStack(Material.AIR));
						else {
							RecipeChoice choice = shapedRecipe.getChoiceMap().get(c);
							if (choice instanceof RecipeChoice.ExactChoice) {
								RecipeChoice.ExactChoice exact = (ExactChoice) choice;
								this.getInventory().setItem(slot, exact.getChoices().get(0));
								if (exact.getChoices().size() > 1)
									choiceMap.put(slot, new ArrayDeque<>(exact.getChoices()));
							} else {
								RecipeChoice.MaterialChoice exact = (RecipeChoice.MaterialChoice) choice;
								Queue<ItemStack> list = new ArrayDeque<>(exact.getChoices().stream().map(k -> new ItemStack(k)).collect(Collectors.toList()));
								this.getInventory().setItem(slot, list.peek());
								if (list.size() > 1)
									choiceMap.put(slot, list);
							}
						}
					}
				}
			} else if (original instanceof ShapelessRecipe) {
				ShapelessRecipe shapelessRecipe = (ShapelessRecipe) original;
				ItemStack greenGlass = ItemBuilder.create(Material.LIME_STAINED_GLASS_PANE).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.creates"))).build().getItem();
				for (int i=0; i < 45; i++)
					if (i % 9 < 5)
						this.getInventory().setItem(i, greenGlass);
				this.getInventory().setItem(23, greenGlass);
				this.getInventory().setItem(24, greenGlass);
				this.getInventory().setItem(25, shapelessRecipe.getResult());
				for (int i=1; i <= 9; i++) {
					int slot = 10+(i == 1 ? 0 : ((i-1)/3)*9)+((i-1) % 3);
					if (shapelessRecipe.getChoiceList().size() < i)
						this.getInventory().setItem(slot, new ItemStack(Material.AIR));
					else {
						RecipeChoice choice = shapelessRecipe.getChoiceList().get(i-1);
						if (choice instanceof RecipeChoice.ExactChoice) {
							RecipeChoice.ExactChoice exact = (ExactChoice) choice;
							this.getInventory().setItem(slot, exact.getChoices().get(0));
							if (exact.getChoices().size() > 1)
								choiceMap.put(slot, new ArrayDeque<>(exact.getChoices()));
						} else {
							RecipeChoice.MaterialChoice exact = (RecipeChoice.MaterialChoice) choice;
							Queue<ItemStack> list = new ArrayDeque<>(exact.getChoices().stream().map(k -> new ItemStack(k)).collect(Collectors.toList()));
							this.getInventory().setItem(slot, list.peek());
							if (list.size() > 1)
								choiceMap.put(slot, list);
						}
					}
				}
			}
		}
		if (page > 1)
			this.addButton(45, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.page").replace("%number%", (page-1)+""))).build().getItem()).function(event -> {
				RecipeMenu menu = new RecipeMenu(type, page-1, memoryPage, adminControls);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		if (page < type.recipes.size())
			this.addButton(53, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.page").replace("%number%", (page+1)+""))).build().getItem()).function(event -> {
				RecipeMenu menu = new RecipeMenu(type, page+1, memoryPage, adminControls);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		this.addButton(49, new InventoryButton().create(ItemBuilder.create(Material.CRAFTING_TABLE).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.returnRecipe"))).build().getItem()).function(event -> {
			ItemsMenu menu = new ItemsMenu(memoryPage);
			MenuManager.registerInventory(menu.getInventory(), menu);
			event.getWhoClicked().openInventory(menu.getInventory());
		}));
		if (adminControls) {
			if (type.recipes.size() >= page)
				this.addButton(47, new InventoryButton().create(ItemBuilder.create(Material.RED_WOOL).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.removeRecipe"))).build().getItem()).function(event -> {
					Recipe recipe = type.recipes.remove(page-1);
					NamespacedKey key = ((Keyed) recipe).getKey();
					UIFramework.dataFile.set(type.getDataPath()+".recipes."+key.getKey(), null);
					UIFramework.getInstance().getServer().removeRecipe(key);
					RecipeMenu menu = null;
					if (type.recipes.size() >= page)
						menu = new RecipeMenu(type, page, memoryPage, adminControls);
					else if (!type.recipes.isEmpty())
						menu = new RecipeMenu(type, page-1, memoryPage, adminControls);
					else {
						ItemsMenu itemMenu = new ItemsMenu(memoryPage);
						MenuManager.registerInventory(itemMenu.getInventory(), itemMenu);
						event.getWhoClicked().openInventory(itemMenu.getInventory());
						return;
					}
					MenuManager.registerInventory(menu.getInventory(), menu);
					event.getWhoClicked().openInventory(menu.getInventory());
				}));
			this.addButton(51, new InventoryButton().create(ItemBuilder.create(Material.GREEN_WOOL).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.addRecipe"))).build().getItem()).function(event -> {
				RecipeCreateMenu menu = new RecipeCreateMenu(type, null, this);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		}
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
	public void onOpen(InventoryOpenEvent e) {
		if (!choiceMap.isEmpty())
			task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
				choiceMap.forEach((k, v) -> {
					ItemStack item = getInventory().getItem(k);
					Iterator<ItemStack> it = v.iterator();
					while (it.hasNext())
						if (it.next().equals(item)) {
							if (it.hasNext())
								getInventory().setItem(k, it.next());
							else
								getInventory().setItem(k, v.peek());
							break;
						}
				});
			}, 0, 20);
	}
	public void onClose(InventoryCloseEvent e) {
		if (task != null)
			task.cancel();
	}
	@Override
	public Inventory createInventory() {
		return Bukkit.createInventory(null, 54, UIFUtils.convertString(type.getBuilder().getItem().getItemMeta().getDisplayName()+" &9"+UIFramework.getLangString("menu.recipe")));
	}
}
