package com.jewishbanana.uiframework.utils;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import com.jewishbanana.uiframework.items.ItemType;

public class AnvilRecipe {

	private Queue<ItemStack> ingredients;
	private ItemStack result;
	private boolean exactIngredients;
	private Function<PrepareAnvilEvent, ItemStack> function;
	
	/**
	 * Creates a new AnvilRecipe which can be directly registered to an ItemType.
	 * <p>
	 * As you may have noticed, you are only required to provide a list of ingredients and a result item.
	 * This is because an AnvilRecipe can only be used to register with an ItemType so it is assumed that one of the slots already contains the ItemType you register this recipe with.
	 * 
	 * @param ingredients A list of ingredients to use with your item
	 * @param result The resulting item from this recipe
	 * @param exactIngredients If the ingredient used must be an exact match from one of the ingredients in the list or just a Material type comparison (Durability on items will be omitted from this check)
	 * 
	 * @see ItemType#registerRecipe(AnvilRecipe)
	 */
	public AnvilRecipe(List<ItemStack> ingredients, ItemStack result, boolean exactIngredients) {
		this.ingredients = new ArrayDeque<>(ingredients.stream().map(k -> k.clone()).collect(Collectors.toList()));
		this.result = result.clone();
		this.exactIngredients = exactIngredients;
	}
	/**
	 * Allows you to run a function on the event and the returned item will be the resulting item used. This is run as soon as all ingredients are present and the resulting item is just placed in the result slot.
	 * <p>
	 * <i>Useful for utilizing this recipe as a repair recipe or adding different variations of the result</i>
	 * 
	 * @param function The function that will be run (Only one function allowed per recipe)
	 */
	public void setFunction(Function<PrepareAnvilEvent, ItemStack> function) {
		this.function = function;
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
}
