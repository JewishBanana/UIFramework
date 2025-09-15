package com.github.jewishbanana.uiframework.listeners.menus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.RecipeChoice.ExactChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.ItemBuilder;
import com.github.jewishbanana.uiframework.items.UIItemType;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.UIAnvilInventory;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class RecipeMenu extends InventoryHandler {

	private static UIFramework plugin;
	private static ItemStack whiteGlass;
	private static ItemStack createGlass;
	private static ItemStack returnTable;
	private static ItemStack removeRecipeItem;
	private static ItemStack addRecipeItem;
	private static ItemStack craftingRecipePage;
	private static ItemStack anvilRecipePage;
	private static String pageString;
	static {
		plugin = UIFramework.getInstance();
		whiteGlass = ItemBuilder.create(Material.WHITE_STAINED_GLASS_PANE).registerName(" ").build().getItem();
	}
	private UIItemType type;
	private String itemDisplayName;
	public int page, memoryPage;
	private Map<Integer, Queue<ItemStack>> choiceMap = new HashMap<>();
	private BukkitTask task;
	private Runnable anvilRunnable;
	public boolean adminControls;
	
	public RecipeMenu(UIItemType type, String itemDisplayName, int page, int memoryPage, boolean adminControls) {
		this.type = type;
		this.itemDisplayName = itemDisplayName;
		this.inventory = this.createInventory();
		this.page = page;
		this.memoryPage = memoryPage;
		this.adminControls = adminControls;
		this.decorate();
	}
	public void decorate() {
		for (int i=0; i < 45; i++)
			this.getInventory().setItem(i, whiteGlass);
		if (!type.getRecipes().isEmpty() && page <= type.getRecipes().size()) {
			Recipe original = type.getRecipes().get(page-1);
			if (original instanceof ShapedRecipe shapedRecipe) {
				for (int i=0; i < 45; i++)
					if (i % 9 < 5)
						this.getInventory().setItem(i, createGlass);
				this.getInventory().setItem(7, craftingRecipePage);
				this.getInventory().setItem(23, createGlass);
				this.getInventory().setItem(24, createGlass);
				this.getInventory().setItem(25, GenericItem.createItemBaseNoID(shapedRecipe.getResult()).getItem());
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
								this.getInventory().setItem(slot, GenericItem.createItemBaseNoID(exact.getChoices().get(0)).getItem());
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
			} else if (original instanceof ShapelessRecipe shapelessRecipe) {
				for (int i=0; i < 45; i++)
					if (i % 9 < 5)
						this.getInventory().setItem(i, createGlass);
				this.getInventory().setItem(7, craftingRecipePage);
				this.getInventory().setItem(23, createGlass);
				this.getInventory().setItem(24, createGlass);
				this.getInventory().setItem(25, GenericItem.createItemBaseNoID(shapelessRecipe.getResult()).getItem());
				for (int i=1; i <= 9; i++) {
					int slot = 10+(i == 1 ? 0 : ((i-1)/3)*9)+((i-1) % 3);
					if (shapelessRecipe.getChoiceList().size() < i)
						this.getInventory().setItem(slot, new ItemStack(Material.AIR));
					else {
						RecipeChoice choice = shapelessRecipe.getChoiceList().get(i-1);
						if (choice instanceof RecipeChoice.ExactChoice) {
							RecipeChoice.ExactChoice exact = (ExactChoice) choice;
							this.getInventory().setItem(slot, GenericItem.createItemBaseNoID(exact.getChoices().get(0)).getItem());
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
			} else if (original instanceof AnvilRecipe anvilRecipe) {
				for (int i=9; i < 33; i++)
					if (i % 9 < 6 && i % 9 > 0)
						this.getInventory().setItem(i, createGlass);
				this.getInventory().setItem(4, anvilRecipePage);
				this.getInventory().setItem(24, createGlass);
				this.getInventory().setItem(21, ItemBuilder.create(Material.NETHER_STAR).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.combined"))).build().getItem());
				RecipeChoice choice = anvilRecipe.getAnvilChoice().getFirstSlot();
				if (choice instanceof RecipeChoice.ExactChoice exact) {
					this.getInventory().setItem(20, GenericItem.createItemBaseNoID(exact.getChoices().get(0)).getItem());
					if (exact.getChoices().size() > 1)
						choiceMap.put(20, new ArrayDeque<>(exact.getChoices()));
				} else {
					RecipeChoice.MaterialChoice exact = (RecipeChoice.MaterialChoice) choice;
					Queue<ItemStack> list = new ArrayDeque<>(exact.getChoices().stream().map(k -> new ItemStack(k)).collect(Collectors.toList()));
					this.getInventory().setItem(20, list.peek());
					if (list.size() > 1)
						choiceMap.put(20, list);
				}
				choice = anvilRecipe.getAnvilChoice().getSecondSlot();
				if (choice instanceof RecipeChoice.ExactChoice exact) {
					this.getInventory().setItem(22, GenericItem.createItemBaseNoID(exact.getChoices().get(0)).getItem());
					if (exact.getChoices().size() > 1)
						choiceMap.put(22, new ArrayDeque<>(exact.getChoices()));
				} else {
					RecipeChoice.MaterialChoice exact = (RecipeChoice.MaterialChoice) choice;
					Queue<ItemStack> list = new ArrayDeque<>(exact.getChoices().stream().map(k -> new ItemStack(k)).collect(Collectors.toList()));
					this.getInventory().setItem(22, list.peek());
					if (list.size() > 1)
						choiceMap.put(22, list);
				}
				if (anvilRecipe.getResult() != null)
					this.getInventory().setItem(25, GenericItem.createItemBaseNoID(anvilRecipe.getResult()).getItem());
				else {
					anvilRunnable = new Runnable() {
						@Override
						public void run() {
							Inventory inv = Bukkit.createInventory(null, InventoryType.ANVIL);
							inv.setItem(0, getInventory().getItem(20));
							inv.setItem(1, getInventory().getItem(22));
							GenericItem base = GenericItem.createItemBaseNoID(anvilRecipe.getAnvilResult(new UIAnvilInventory(inv)).result);
							if (base != null)
								getInventory().setItem(25, base.getItem());
						}
					};
					anvilRunnable.run();
				}
			}
		}
		if (page > 1)
			this.addButton(45, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(pageString.replace("%number%", (page-1)+""))).build().getItem()).function(event -> {
				RecipeMenu menu = new RecipeMenu(type, itemDisplayName, page-1, memoryPage, adminControls);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		if (page < type.getRecipes().size())
			this.addButton(53, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(pageString.replace("%number%", (page+1)+""))).build().getItem()).function(event -> {
				RecipeMenu menu = new RecipeMenu(type, itemDisplayName, page+1, memoryPage, adminControls);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		this.addButton(49, new InventoryButton().create(returnTable).function(event -> {
			ItemsMenu menu = new ItemsMenu(memoryPage, adminControls);
			MenuManager.registerInventory(menu.getInventory(), menu);
			event.getWhoClicked().openInventory(menu.getInventory());
		}));
		if (adminControls) {
			if (type.getRecipes().size() >= page)
				this.addButton(47, new InventoryButton().create(removeRecipeItem).function(event -> {
					Recipe recipe = type.getRecipes().remove(page-1);
					NamespacedKey key = ((Keyed) recipe).getKey();
					UIFramework.dataFile.set(type.getDataPath()+".recipes."+key.getKey(), null);
					if (!key.getKey().contains(type.getRegisteredName().replaceFirst(":", "-")+"_user_recipe_")) {
						List<String> removed = new ArrayList<>();
						if (UIFramework.dataFile.contains(type.getDataPath()+".removed_recipes"))
							removed.addAll(UIFramework.dataFile.getStringList(type.getDataPath()+".removed_recipes"));
						removed.add(key.getKey());
						UIFramework.dataFile.set(type.getDataPath()+".removed_recipes", removed);
					}
					UIFramework.getInstance().getServer().removeRecipe(key);
					RecipeMenu menu = null;
					if (type.getRecipes().size() >= page)
						menu = new RecipeMenu(type, itemDisplayName, page, memoryPage, adminControls);
					else if (!type.getRecipes().isEmpty())
						menu = new RecipeMenu(type, itemDisplayName, page-1, memoryPage, adminControls);
					else {
						ItemsMenu itemMenu = new ItemsMenu(memoryPage, adminControls);
						MenuManager.registerInventory(itemMenu.getInventory(), itemMenu);
						event.getWhoClicked().openInventory(itemMenu.getInventory());
						return;
					}
					MenuManager.registerInventory(menu.getInventory(), menu);
					event.getWhoClicked().openInventory(menu.getInventory());
				}));
			this.addButton(51, new InventoryButton().create(addRecipeItem).function(event -> {
				RecipeCreateMenu menu = new RecipeCreateMenu(type, itemDisplayName, null, this);
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
			task = new BukkitRunnable() {
				@Override
				public void run() {
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
					if (anvilRunnable != null)
						anvilRunnable.run();
				}
			}.runTaskTimer(plugin, 20, 20);
	}
	public void onClose(InventoryCloseEvent e) {
		if (task != null)
			task.cancel();
	}
	@Override
	public Inventory createInventory() {
		return Bukkit.createInventory(null, 54, UIFUtils.convertString(itemDisplayName+" &9"+UIFramework.getLangString("menu.recipe")));
	}
	public static void reload() {
		createGlass = ItemBuilder.create(Material.LIME_STAINED_GLASS_PANE).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.creates"))).build().getItem();
		returnTable = ItemBuilder.create(Material.PAPER).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.returnRecipe"))).build().getItem();
		removeRecipeItem = ItemBuilder.create(Material.RED_WOOL).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.removeRecipe"))).build().getItem();
		addRecipeItem = ItemBuilder.create(Material.GREEN_WOOL).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.addRecipe"))).build().getItem();
		craftingRecipePage = ItemBuilder.create(Material.CRAFTING_TABLE).registerName(UIFUtils.convertString("&a"+UIFramework.getLangString("menu.craftingTableRecipe"))).build().getItem();
		anvilRecipePage = ItemBuilder.create(Material.ANVIL).registerName(UIFUtils.convertString("&a"+UIFramework.getLangString("menu.anvilRecipe"))).build().getItem();
		pageString = UIFUtils.convertString(UIFramework.getLangString("menu.page"));
	}
}
