package com.jewishbanana.uiframework.listeners.menus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.ItemType;
import com.jewishbanana.uiframework.utils.ItemBuilder;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class RecipeCreateMenu extends InventoryHandler {

	private static UIFramework plugin;
	static {
		plugin = UIFramework.getInstance();
	}
	private ItemType type;
	private String recipeType;
	public RecipeMenu returnMenu;
	private RecipeCreateMenu returnRecipeMenu;
	private Recipe recipe;
	
	public RecipeCreateMenu(ItemType type, String recipeType, RecipeMenu returnMenu) {
		this.type = type;
		this.recipeType = recipeType;
		this.inventory = this.createInventory();
		this.returnMenu = returnMenu;
		this.decorate();
	}
	public RecipeCreateMenu(ItemType type, String recipeType, RecipeCreateMenu returnRecipeMenu) {
		this.type = type;
		this.recipeType = recipeType;
		this.inventory = this.createInventory();
		this.returnRecipeMenu = returnRecipeMenu;
		this.decorate();
	}
	public void decorate() {
		ItemStack whiteGlass = ItemBuilder.create(Material.WHITE_STAINED_GLASS_PANE).registerName(" ").build().getItem();
		ItemStack greenGlass = ItemBuilder.create(Material.LIME_STAINED_GLASS_PANE).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.creates"))).build().getItem();
		if (recipeType == null) {
			for (int i=0; i < 54; i++)
				if (i < 10 || i >= 44 || (i >= 17 && i <= 18) || (i >= 26 && i <= 27) || (i >= 35 && i <= 36))
					this.getInventory().setItem(i, whiteGlass);
			this.getInventory().setItem(4, ItemBuilder.create(Material.CRAFTING_TABLE).registerName(UIFUtils.convertString("&aUltimateItems "+UIFramework.getLangString("menu.recipe"))).setLoreList(Arrays.asList(UIFUtils.convertString(UIFramework.getLangString("menu.recipeType")))).build().getItem());
			this.addButton(49, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.returnRecipe"))).build().getItem()).function(event -> {
				MenuManager.registerInventory(returnMenu.getInventory(), returnMenu);
				event.getWhoClicked().openInventory(returnMenu.getInventory());
			}));
			this.addButton(10, new InventoryButton().create(ItemBuilder.create(Material.SUGAR).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.shapedRecipe"))).build().getItem()).function(event -> {
				RecipeCreateMenu menu = new RecipeCreateMenu(type, "shaped", this);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
			this.addButton(11, new InventoryButton().create(ItemBuilder.create(Material.GUNPOWDER).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.shapelessRecipe"))).build().getItem()).function(event -> {
				RecipeCreateMenu menu = new RecipeCreateMenu(type, "shapeless", this);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		} else {
			ItemStack air = new ItemStack(Material.AIR);
			String key = null;
			for (int i=0; i < 999999; i++)
				if (!UIFramework.dataFile.contains(type.getDataPath()+".recipes."+type.getRegisteredName()+"_recipe_"+i)) {
					key = type.getRegisteredName()+"_recipe_"+i;
					break;
				}
			if (key == null)
				throw new IllegalArgumentException("[UltimateItems]: Cannot create recipe for '"+type.getRegisteredName()+"' as there is no default available namespace!");
			for (int i=0; i < 45; i++)
				this.getInventory().setItem(i, whiteGlass);
			switch (recipeType) {
			case "shaped":
				recipe = new ShapedRecipe(new NamespacedKey(plugin, key), type.getBuilder().getItem());
			case "shapeless":
				if (recipe == null)
					recipe = new ShapelessRecipe(new NamespacedKey(plugin, key), type.getBuilder().getItem());
				for (int i=0; i < 45; i++)
					if (i % 9 < 5)
						this.getInventory().setItem(i, greenGlass);
				this.getInventory().setItem(23, greenGlass);
				this.getInventory().setItem(24, greenGlass);
				this.getInventory().setItem(25, recipe.getResult());
				for (int i=1; i <= 9; i++) {
					int slot = 10+(i == 1 ? 0 : ((i-1)/3)*9)+((i-1) % 3);
					this.getInventory().setItem(slot, air);
				}
				break;
			}
			this.addButton(45, new InventoryButton().create(ItemBuilder.create(Material.ARROW).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.returnRecipe"))).build().getItem()).function(event -> {
				MenuManager.registerInventory(returnRecipeMenu.getInventory(), returnRecipeMenu);
				event.getWhoClicked().openInventory(returnRecipeMenu.getInventory());
			}));
			this.addButton(53, new InventoryButton().create(ItemBuilder.create(Material.GREEN_WOOL).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.acceptRecipe"))).build().getItem()).function(event -> {
				if (recipe instanceof ShapedRecipe) {
					ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
					Map<Character, RecipeChoice.ExactChoice> choiceMap = new HashMap<>();
					String[] shape = new String[3];
					for (int i=0; i < 3; i++)
						shape[i] = "";
					for (int i=0; i < 3; i++)
						for (int j=0; j < 3; j++) {
							int slot = ((i+1)*9)+j+1;
							ItemStack item = this.getInventory().getItem(slot);
							if (item == null || item.getType() == Material.AIR)
								shape[i] += ' ';
							else {
								char c = Character.forDigit((i*3)+j, 10);
								shape[i] += c;
								choiceMap.put(c, new RecipeChoice.ExactChoice(item));
							}
						}
					if (!choiceMap.isEmpty()) {
						shapedRecipe.shape(shape);
						choiceMap.forEach((k, v) -> shapedRecipe.setIngredient(k, v));
						ItemType.registerRecipe(type, shapedRecipe);
						RecipeMenu menu = new RecipeMenu(type, type.recipes.size(), returnRecipeMenu.returnMenu.memoryPage, returnRecipeMenu.returnMenu.adminControls);
						MenuManager.registerInventory(menu.getInventory(), menu);
						event.getWhoClicked().openInventory(menu.getInventory());
						return;
					}
				} else if (recipe instanceof ShapelessRecipe) {
					ShapelessRecipe shapedRecipe = (ShapelessRecipe) recipe;
					for (int i=0; i < 3; i++)
						for (int j=0; j < 3; j++) {
							int slot = ((i+1)*9)+j+1;
							ItemStack item = this.getInventory().getItem(slot);
							if (item != null)
								shapedRecipe.addIngredient(new RecipeChoice.ExactChoice(item));
						}
					if (!shapedRecipe.getChoiceList().isEmpty()) {
						ItemType.registerRecipe(type, shapedRecipe);
						RecipeMenu menu = new RecipeMenu(type, type.recipes.size(), returnRecipeMenu.returnMenu.memoryPage, returnRecipeMenu.returnMenu.adminControls);
						MenuManager.registerInventory(menu.getInventory(), menu);
						event.getWhoClicked().openInventory(menu.getInventory());
						return;
					}
				}
				MenuManager.registerInventory(returnRecipeMenu.returnMenu.getInventory(), returnRecipeMenu.returnMenu);
				event.getWhoClicked().openInventory(returnRecipeMenu.returnMenu.getInventory());
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
		if (recipeType != null)
			switch (recipeType) {
			default:
				return;
			case "shaped":
			case "shapeless":
				if ((slot >= 10 && slot <= 12) || (slot >= 19 && slot <= 21) || (slot >= 28 && slot <= 30))
					event.setCancelled(false);
				if (slot > 53 && event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
					for (int i=1; i <= 9; i++)
						if (this.getInventory().getItem(10+(i == 1 ? 0 : ((i-1)/3)*9)+((i-1) % 3)) == null)
							return;
					event.setCancelled(true);
				}
				break;
			}
	}
	public void onClose(InventoryCloseEvent e) {
		if (recipeType != null) {
			Player p = (Player) e.getPlayer();
			switch (recipeType) {
			default:
				return;
			case "shaped":
			case "shapeless":
				for (int i=1; i <= 9; i++) {
					int slot = 10+(i == 1 ? 0 : ((i-1)/3)*9)+((i-1) % 3);
					ItemStack item = this.getInventory().getItem(slot);
					if (item != null) {
						if (p.getInventory().firstEmpty() == -1)
							p.getWorld().dropItem(p.getLocation(), item);
						else
							p.getInventory().addItem(item);
					}
				}
				break;
			}
		}
	}
	@Override
	public Inventory createInventory() {
		return Bukkit.createInventory(null, 54, UIFUtils.convertString(type.getBuilder().getItem().getItemMeta().getDisplayName()+" &9"+UIFramework.getLangString("menu.recipe")));
	}
}
