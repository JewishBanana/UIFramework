package com.github.jewishbanana.uiframework.utils;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.uiframework.items.ItemType;

@Deprecated
/**
 * @deprecated This class will be completely changed in a future update
 */
public class AnvilRecipe {

	private Queue<ItemStack> ingredients;
	private ItemStack result;
	private boolean exactIngredients;
	private Function<PrepareAnvilEvent, ItemStack> function;
	private AnvilSlot slot = AnvilSlot.FIRST;
	
	public enum AnvilSlot {
		FIRST,
		SECOND
	}
	/**
	 * Creates a new AnvilRecipe which can be directly registered to an ItemType.
	 * <p>
	 * As you may have noticed, you are only required to provide a list of ingredients and a result item.
	 * This is because an AnvilRecipe can only be used to register with an ItemType so it is assumed that one of the slots already contains the ItemType you register this recipe with.
	 * 
	 * @param ingredients A list of ingredients to use with your item
	 * @param result The resulting item from this recipe
	 * @param exactIngredients If true the ingredient used must be an exact match of one of the ingredients in the provided list (Durability on items will be omitted from this check). If false will just use a Material type comparison.
	 * 
	 * @see ItemType#registerRecipe(AnvilRecipe)
	 */
	public AnvilRecipe(List<ItemStack> ingredients, ItemStack result, boolean exactIngredients) {
		this.ingredients = new ArrayDeque<>(ingredients.stream().map(k -> k.clone()).collect(Collectors.toList()));
		this.result = result.clone();
		this.exactIngredients = exactIngredients;
	}
	/**
	 * Creates a new AnvilRecipe which can be directly registered to an ItemType.
	 * <p>
	 * As you may have noticed, you are only required to provide a list of ingredients and a result function.
	 * This is because an AnvilRecipe can only be used to register with an ItemType so it is assumed that one of the slots already contains the ItemType you register this recipe with.
	 * 
	 * @param ingredients A list of ingredients to use with your item
	 * @param function The resulting function run on the event if this recipes criteria is met
	 * @param exactIngredients If true the ingredient used must be an exact match of one of the ingredients in the provided list (Durability on items will be omitted from this check). If false will just use a Material type comparison.
	 * 
	 * @see ItemType#registerRecipe(AnvilRecipe)
	 */
	public AnvilRecipe(List<ItemStack> ingredients, Function<PrepareAnvilEvent, ItemStack> function, boolean exactIngredients) {
		this.ingredients = new ArrayDeque<>(ingredients.stream().map(k -> k.clone()).collect(Collectors.toList()));
		this.result = new ItemStack(Material.AIR);
		this.function = function;
		this.exactIngredients = exactIngredients;
	}
	public Function<PrepareAnvilEvent, ItemStack> getFunction() {
		return function;
	}
	public Queue<ItemStack> getIngredients() {
		return ingredients;
	}
	public void setIngredients(Queue<ItemStack> ingredients) {
		this.ingredients = ingredients;
	}
	public ItemStack getResult() {
		return result.clone();
	}
	public void setResult(ItemStack result) {
		this.result = result.clone();
	}
	public boolean isExactIngredients() {
		return exactIngredients;
	}
	public void setExactIngredients(boolean exactIngredients) {
		this.exactIngredients = exactIngredients;
	}
	public AnvilSlot getSlot() {
		return slot;
	}
	public void setSlot(AnvilSlot slot) {
		this.slot = slot;
	}
}
