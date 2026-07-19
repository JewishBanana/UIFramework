package com.github.jewishbanana.uiframework.listeners.menus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.inventory.meta.PotionMeta;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.ItemBuilder;
import com.github.jewishbanana.uiframework.items.UIItemType;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice.SlotOrder;
import com.github.jewishbanana.uiframework.utils.BrewingRecipe;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class RecipeCreateMenu extends InventoryHandler {

	private static UIFramework plugin;
	static {
		plugin = UIFramework.getInstance();
	}
	private UIItemType type;
	private String itemDisplayName;
	private String recipeType;
	public RecipeMenu returnMenu;
	private RecipeCreateMenu returnRecipeMenu;
	private Recipe recipe;
	private NamespacedKey recipeKey;
	private int brewingTime = BrewingRecipe.DEFAULT_BREWING_TIME;
	private int anvilLevelCost = 1;
	
	public RecipeCreateMenu(UIItemType type, String itemDisplayName, String recipeType, RecipeMenu returnMenu) {
		this.type = type;
		this.itemDisplayName = itemDisplayName;
		this.recipeType = recipeType;
		this.inventory = this.createInventory();
		this.returnMenu = returnMenu;
		this.decorate();
	}
	public RecipeCreateMenu(UIItemType type, String itemDisplayName, String recipeType, RecipeCreateMenu returnRecipeMenu) {
		this.type = type;
		this.itemDisplayName = itemDisplayName;
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
				RecipeCreateMenu menu = new RecipeCreateMenu(type, itemDisplayName, "shaped", this);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
			this.addButton(11, new InventoryButton().create(ItemBuilder.create(Material.GUNPOWDER).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.shapelessRecipe"))).build().getItem()).function(event -> {
				RecipeCreateMenu menu = new RecipeCreateMenu(type, itemDisplayName, "shapeless", this);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
			this.addButton(12, new InventoryButton().create(ItemBuilder.create(Material.BREWING_STAND).registerName(UIFUtils.convertString("&6"+UIFramework.getLangString("menu.brewingRecipe"))).build().getItem()).function(event -> {
				RecipeCreateMenu menu = new RecipeCreateMenu(type, itemDisplayName, "brewing", this);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
			this.addButton(13, new InventoryButton().create(ItemBuilder.create(Material.ANVIL).registerName(UIFUtils.convertString("&7"+UIFramework.getLangString("menu.anvilRecipe"))).build().getItem()).function(event -> {
				RecipeCreateMenu menu = new RecipeCreateMenu(type, itemDisplayName, "anvil", this);
				MenuManager.registerInventory(menu.getInventory(), menu);
				event.getWhoClicked().openInventory(menu.getInventory());
			}));
		} else {
			ItemStack air = new ItemStack(Material.AIR);
			String key = null;
			for (int i=0; i < 999999; i++)
				if (!UIFramework.dataFile.contains(type.getDataPath()+".recipes."+type.getRegisteredName().replaceFirst(":", "-")+"_user_recipe_"+i)) {
					key = type.getRegisteredName().replaceFirst(":", "-")+"_user_recipe_"+i;
					break;
				}
			if (key == null)
				throw new IllegalArgumentException("[UltimateItems]: Cannot create recipe for '"+type.getRegisteredName()+"' as there is no default available namespace!");
			recipeKey = new NamespacedKey(plugin, key);
			for (int i=0; i < 45; i++)
				this.getInventory().setItem(i, whiteGlass);
			switch (recipeType) {
			case "shaped":
				recipe = new ShapedRecipe(recipeKey, type.getItem());
			case "shapeless":
				if (recipe == null)
					recipe = new ShapelessRecipe(recipeKey, type.getItem());
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
			case "brewing":
				for (int i=9; i < 45; i++)
					if (i % 9 < 8 && i % 9 > 0)
						this.getInventory().setItem(i, greenGlass);
				for (int slot=1; slot <= 5; slot++)
					this.getInventory().setItem(slot, greenGlass);
				for (int slot : new int[] {15, 16, 33, 34, 42, 43})
					this.getInventory().setItem(slot, whiteGlass);
				this.getInventory().setItem(11, createLabelGlass(Material.YELLOW_STAINED_GLASS_PANE, "menu.brewingIngredientLeft"));
				this.getInventory().setItem(12, air);
				this.getInventory().setItem(13, createLabelGlass(Material.YELLOW_STAINED_GLASS_PANE, "menu.brewingIngredientRight"));
				this.getInventory().setItem(21, greenGlass);
				this.getInventory().setItem(29, createLabelGlass(Material.BLUE_STAINED_GLASS_PANE, "menu.brewingPotionLeft"));
				this.getInventory().setItem(30, air);
				this.getInventory().setItem(31, createLabelGlass(Material.BLUE_STAINED_GLASS_PANE, "menu.brewingPotionRight"));
				this.getInventory().setItem(25, type.getItem());
				this.addButton(39, new InventoryButton().create(createBrewingTimeItem()).function(event -> {
					int change = event.isShiftClick() ? 100 : 20;
					if (event.isLeftClick())
						brewingTime = Math.min(12000, brewingTime + change);
					else if (event.isRightClick())
						brewingTime = Math.max(20, brewingTime - change);
					event.getInventory().setItem(39, createBrewingTimeItem());
				}));
				break;
			case "anvil":
				for (int i=9; i < 33; i++)
					if (i % 9 < 6 && i % 9 > 0)
						this.getInventory().setItem(i, greenGlass);
				this.getInventory().setItem(20, air);
				this.getInventory().setItem(21, ItemBuilder.create(Material.NETHER_STAR).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.combined"))).build().getItem());
				this.getInventory().setItem(22, air);
				this.getInventory().setItem(24, greenGlass);
				this.getInventory().setItem(25, type.getItem());
				this.addButton(30, new InventoryButton().create(createAnvilCostItem()).function(event -> {
					int change = event.isShiftClick() ? 5 : 1;
					if (event.isLeftClick())
						anvilLevelCost = Math.min(39, anvilLevelCost + change);
					else if (event.isRightClick())
						anvilLevelCost = Math.max(0, anvilLevelCost - change);
					event.getInventory().setItem(30, createAnvilCostItem());
				}));
				break;
			}
			this.addButton(45, new InventoryButton().create(ItemBuilder.create(Material.PAPER).registerName(UIFUtils.convertString(UIFramework.getLangString("menu.returnRecipe"))).build().getItem()).function(event -> {
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
								choiceMap.put(c, new RecipeChoice.ExactChoice(stripAndCopy(item)));
							}
						}
					if (!choiceMap.isEmpty()) {
						shapedRecipe.shape(shape);
						choiceMap.forEach((k, v) -> shapedRecipe.setIngredient(k, v));
						type.registerRecipe(shapedRecipe);
						RecipeMenu menu = new RecipeMenu(type, itemDisplayName, type.getRecipes().size(), returnRecipeMenu.returnMenu.memoryPage, returnRecipeMenu.returnMenu.adminControls);
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
								shapedRecipe.addIngredient(new RecipeChoice.ExactChoice(stripAndCopy(item)));
						}
					if (!shapedRecipe.getChoiceList().isEmpty()) {
						type.registerRecipe(shapedRecipe);
						RecipeMenu menu = new RecipeMenu(type, itemDisplayName, type.getRecipes().size(), returnRecipeMenu.returnMenu.memoryPage, returnRecipeMenu.returnMenu.adminControls);
						MenuManager.registerInventory(menu.getInventory(), menu);
						event.getWhoClicked().openInventory(menu.getInventory());
						return;
					}
				} else if ("brewing".equals(recipeType)) {
					ItemStack input = this.getInventory().getItem(30);
					ItemStack ingredient = this.getInventory().getItem(12);
					if (isPresent(input) && isPresent(ingredient)) {
						type.registerRecipe(new BrewingRecipe(recipeKey, createRecipeChoice(input),
								createRecipeChoice(ingredient), type.getItem(), brewingTime));
						openCreatedRecipe(event.getWhoClicked());
						return;
					}
				} else if ("anvil".equals(recipeType)) {
					ItemStack first = this.getInventory().getItem(20);
					ItemStack second = this.getInventory().getItem(22);
					if (isPresent(first) && isPresent(second)) {
						type.registerRecipe(new AnvilRecipe(recipeKey, new AnvilChoice(createRecipeChoice(first),
								createRecipeChoice(second), SlotOrder.MIXED), type.getItem(), anvilLevelCost));
						openCreatedRecipe(event.getWhoClicked());
						return;
					}
				}
				MenuManager.registerInventory(returnRecipeMenu.returnMenu.getInventory(), returnRecipeMenu.returnMenu);
				event.getWhoClicked().openInventory(returnRecipeMenu.returnMenu.getInventory());
			}));
		}
		super.decorate();
	}
	private ItemStack stripAndCopy(ItemStack item) {
		ItemStack cleaned = GenericItem.cleanRecipeItem(item);
		GenericItem base = GenericItem.getItemBaseNoID(cleaned);
		if (base == null)
			return cleaned;
		return UIFUtils.stripItemTags(base);
	}
	private RecipeChoice createRecipeChoice(ItemStack item) {
		ItemStack cleaned = GenericItem.cleanRecipeItem(item);
		if (GenericItem.getItemBaseNoID(cleaned) != null || cleaned.getItemMeta() instanceof PotionMeta)
			return new RecipeChoice.ExactChoice(stripAndCopy(cleaned));
		return new RecipeChoice.MaterialChoice(cleaned.getType());
	}
	private ItemStack createBrewingTimeItem() {
		ItemStack item = ItemBuilder.create(Material.CLOCK)
				.registerName(UIFUtils.convertString(UIFramework.getLangString("menu.brewingTime").replace("%seconds%", UIFDataUtils.getDecimalFormatted(brewingTime / 20.0))))
				.setLoreList(Arrays.asList(UIFUtils.convertString(UIFramework.getLangString("menu.adjustRecipeValue")), UIFUtils.convertString(UIFramework.getLangString("menu.adjustRecipeValueShift"))))
				.build().getItem();
		item.setAmount(Math.min(64, Math.max(1, brewingTime / 20)));
		return item;
	}
	private ItemStack createLabelGlass(Material material, String languageKey) {
		return ItemBuilder.create(material).registerName(UIFUtils.convertString(UIFramework.getLangString(languageKey))).build().getItem();
	}
	private ItemStack createAnvilCostItem() {
		ItemStack item = ItemBuilder.create(Material.EXPERIENCE_BOTTLE)
				.registerName(UIFUtils.convertString(UIFramework.getLangString("menu.anvilLevelCost").replace("%levels%", Integer.toString(anvilLevelCost))))
				.setLoreList(Arrays.asList(UIFUtils.convertString(UIFramework.getLangString("menu.adjustRecipeValue")), UIFUtils.convertString(UIFramework.getLangString("menu.adjustRecipeValueShift"))))
				.build().getItem();
		item.setAmount(Math.max(1, anvilLevelCost));
		return item;
	}
	private void openCreatedRecipe(HumanEntity player) {
		RecipeMenu menu = new RecipeMenu(type, itemDisplayName, type.getRecipes().size(), returnRecipeMenu.returnMenu.memoryPage, returnRecipeMenu.returnMenu.adminControls);
		MenuManager.registerInventory(menu.getInventory(), menu);
		player.openInventory(menu.getInventory());
	}
	private static boolean isPresent(ItemStack item) {
		return item != null && item.getType() != Material.AIR;
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
			case "brewing":
				if (slot == 12 || slot == 30)
					event.setCancelled(false);
				break;
			case "anvil":
				if (slot == 20 || slot == 22)
					event.setCancelled(false);
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
			case "brewing":
				returnItems(p, 12, 30);
				break;
			case "anvil":
				returnItems(p, 20, 22);
				break;
			}
		}
	}
	private void returnItems(Player player, int... slots) {
		for (int slot : slots) {
			ItemStack item = this.getInventory().getItem(slot);
			if (!isPresent(item))
				continue;
			if (player.getInventory().firstEmpty() == -1)
				player.getWorld().dropItem(player.getLocation(), item);
			else
				player.getInventory().addItem(item);
		}
	}
	@Override
	public Inventory createInventory() {
		return Bukkit.createInventory(null, 54, UIFUtils.convertString(itemDisplayName+" &9"+UIFramework.getLangString("menu.recipe")));
	}
}
