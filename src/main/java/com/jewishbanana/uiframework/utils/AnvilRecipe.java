package com.jewishbanana.uiframework.utils;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

public class AnvilRecipe {

	private Queue<ItemStack> ingredients;
	private ItemStack result;
	private boolean exactIngredients;
	private Function<PrepareAnvilEvent, ItemStack> function;
	
	public AnvilRecipe(List<ItemStack> ingredients, ItemStack result, boolean exactIngredients) {
		this.ingredients = new ArrayDeque<>(ingredients.stream().map(k -> k.clone()).collect(Collectors.toList()));
		this.result = result.clone();
		this.exactIngredients = exactIngredients;
	}
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
