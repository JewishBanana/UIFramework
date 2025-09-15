package com.github.jewishbanana.uiframework.utils;

import java.util.function.Function;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.jewishbanana.uiframework.items.UIItemType;

public class AnvilRecipe implements Recipe, Keyed {

	private NamespacedKey key;
	private AnvilChoice choice;
	private ItemStack result;
	private Function<UIAnvilInventory, AnvilResult> function;
	private int levelCost;
	private int repairAmount;
	private boolean isRepair;
	
	/**
	 * Creates a new AnvilRecipe which can be directly registered to an ItemType.
	 * <p>
	 * As you may have noticed, you are only required to provide a list of ingredients and a result item.
	 * This is because an AnvilRecipe can only be used to register with an ItemType so it is assumed that one of the slots already contains the ItemType you register this recipe with.
	 * 
	 * @param choices A list of ingredients to use with your item
	 * @param result The resulting item from this recipe
	 * @param exactIngredients If true the ingredient used must be an exact match of one of the ingredients in the provided list (Durability on items will be omitted from this check). If false will just use a Material type comparison.
	 * 
	 * @see UIItemType#registerRecipe(AnvilRecipe)
	 */
	public AnvilRecipe(NamespacedKey key, RecipeChoice firstSlot, RecipeChoice secondSlot, ItemStack result, int levelCost) {
		if (key == null)
			throw new NullPointerException("Registered namespace key cannot be null!");
		if (firstSlot == null || secondSlot == null)
			throw new IllegalArgumentException("Ingredient choice cannot be null!");
		if (result == null)
			throw new NullPointerException("Resulting item cannot be null!");
		if (result.getType() == Material.AIR)
			throw new IllegalArgumentException("Resulting item cannot be air!");
		this.key = key;
		this.choice = new AnvilChoice(firstSlot, secondSlot);
		this.result = result.clone();
		this.levelCost = levelCost;
	}
	public AnvilRecipe(NamespacedKey key, AnvilChoice choice, ItemStack result, int levelCost) {
		if (key == null)
			throw new NullPointerException("Registered namespace key cannot be null!");
		if (choice == null)
			throw new IllegalArgumentException("Ingredient choice cannot be null!");
		if (result == null)
			throw new NullPointerException("Resulting item cannot be null!");
		if (result.getType() == Material.AIR)
			throw new IllegalArgumentException("Resulting item cannot be air!");
		this.key = key;
		this.choice = choice;
		this.result = result.clone();
		this.levelCost = levelCost;
	}
	public AnvilRecipe(NamespacedKey key, RecipeChoice toRepair, RecipeChoice materials, int repairAmount, int levelCost) {
		if (key == null)
			throw new NullPointerException("Registered namespace key cannot be null!");
		if (materials == null)
			throw new IllegalArgumentException("Ingredient choice cannot be null!");
		this.key = key;
		this.choice = new AnvilChoice(toRepair, materials);
		this.repairAmount = repairAmount;
		this.levelCost = levelCost;
		this.isRepair = true;
		this.function = inventory -> {
			ItemStack item = inventory.getFirstSlot().clone();
			int amount = inventory.getSecondSlot().getAmount();
			ItemMeta meta = item.getItemMeta();
			((Damageable) meta).setDamage(Math.max(((Damageable) meta).getDamage()-(amount*repairAmount), 0));
			item.setItemMeta(meta);
			return new AnvilResult(item, amount * levelCost);
		};
	}
	/**
	 * Creates a new AnvilRecipe which can be directly registered to an ItemType.
	 * <p>
	 * As you may have noticed, you are only required to provide a list of ingredients and a result function.
	 * This is because an AnvilRecipe can only be used to register with an ItemType so it is assumed that one of the slots already contains the ItemType you register this recipe with.
	 * 
	 * @param choices A list of ingredients to use with your item
	 * @param function The resulting function run on the event if this recipes criteria is met
	 * @param exactIngredients If true the ingredient used must be an exact match of one of the ingredients in the provided list (Durability on items will be omitted from this check). If false will just use a Material type comparison.
	 * 
	 * @see UIItemType#registerRecipe(AnvilRecipe)
	 */
	public AnvilRecipe(NamespacedKey key, AnvilChoice choice, Function<UIAnvilInventory, AnvilResult> function) {
		if (key == null)
			throw new NullPointerException("Registered namespace key cannot be null!");
		if (choice == null)
			throw new IllegalArgumentException("Ingredient choice cannot be null!");
		if (function == null)
			throw new NullPointerException("Resulting function cannot be null!");
		this.key = key;
		this.choice = choice;
		this.function = function;
	}
	public AnvilResult getAnvilResult(UIAnvilInventory inventory) {
		return function == null ? new AnvilResult(result, levelCost) : function.apply(inventory);
	}
	public AnvilChoice getAnvilChoice() {
		return choice;
	}
	public ItemStack getResult() {
		return result == null ? null : result.clone();
	}
	public int getLevelCost() {
		return levelCost;
	}
	public boolean isRepair() {
		return isRepair;
	}
	public int getRepairAmount() {
		return repairAmount;
	}
	public NamespacedKey getKey() {
		return key;
	}
	public static class UIAnvilInventory {
		
		private Inventory inventory;
		
		public UIAnvilInventory(Inventory inventory) {
			if (inventory.getType() != InventoryType.ANVIL)
				throw new IllegalArgumentException("Cannot create a UIAnvilInventory out of a non anvil type inventory!");
			this.inventory = inventory;
		}
		public ItemStack getFirstSlot() {
			return inventory.getItem(0);
		}
		public ItemStack getSecondSlot() {
			return inventory.getItem(1);
		}
		public ItemStack getResultSlot() {
			return inventory.getItem(2);
		}
	}
	public static class AnvilChoice {
		
		public enum SlotOrder {
			SPECIFIED,
			MIXED;
		}
		
		private RecipeChoice firstSlot;
		private RecipeChoice secondSlot;
		private SlotOrder order = SlotOrder.SPECIFIED;
		
		public AnvilChoice(RecipeChoice firstSlot, RecipeChoice secondSlot, SlotOrder order) {
			this.firstSlot = firstSlot;
			this.secondSlot = secondSlot;
			this.order = order;
		}
		public AnvilChoice(RecipeChoice firstSlot, RecipeChoice secondSlot) {
			this(firstSlot, secondSlot, SlotOrder.SPECIFIED);
		}
		public RecipeChoice getFirstSlot() {
			return firstSlot;
		}
		public RecipeChoice getSecondSlot() {
			return secondSlot;
		}
		public SlotOrder getSlotOrder() {
			return order;
		}
	}
	public static class AnvilResult {
		
		public ItemStack result;
		public int levelCost;
		
		public AnvilResult(ItemStack result, int levelCost) {
			this.result = result;
			this.levelCost = levelCost;
		}
		public AnvilResult(ItemStack result) {
			this(result, 0);
		}
	}
}
