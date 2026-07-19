package com.github.jewishbanana.uiframework.utils;

import java.util.Objects;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;

import com.github.jewishbanana.uiframework.items.GenericItem;

public class BrewingRecipe implements Recipe, Keyed {

	public static final int DEFAULT_BREWING_TIME = 400;

	private final NamespacedKey key;
	private final RecipeChoice input;
	private final RecipeChoice ingredient;
	private final ItemStack result;
	private final int brewingTime;

	public BrewingRecipe(NamespacedKey key, RecipeChoice input, RecipeChoice ingredient, ItemStack result) {
		this(key, input, ingredient, result, DEFAULT_BREWING_TIME);
	}
	public BrewingRecipe(NamespacedKey key, RecipeChoice input, RecipeChoice ingredient, ItemStack result, int brewingTime) {
		if (key == null || input == null || ingredient == null || result == null || result.getType() == Material.AIR)
			throw new IllegalArgumentException("Brewing recipes require a key, input, ingredient, and non-air result");
		if (brewingTime < 1)
			throw new IllegalArgumentException("Brewing time must be at least one tick");
		this.key = key;
		this.input = input;
		this.ingredient = ingredient;
		this.result = result.clone();
		this.brewingTime = brewingTime;
	}
	@Override
	public NamespacedKey getKey() {
		return key;
	}
	@Override
	public ItemStack getResult() {
		return result.clone();
	}
	public RecipeChoice getInput() {
		return input;
	}
	public RecipeChoice getIngredient() {
		return ingredient;
	}
	public int getBrewingTime() {
		return brewingTime;
	}
	public boolean matches(ItemStack input, ItemStack ingredient) {
		return matchesChoice(this.input, input) && matchesChoice(this.ingredient, ingredient);
	}
	public boolean matchesInput(ItemStack input) {
		return matchesChoice(this.input, input);
	}
	public boolean matchesIngredient(ItemStack ingredient) {
		return matchesChoice(this.ingredient, ingredient);
	}
	private static boolean matchesChoice(RecipeChoice choice, ItemStack sample) {
		if (sample == null || sample.getType() == Material.AIR)
			return false;
		if (choice instanceof RecipeChoice.ExactChoice exact) {
			for (ItemStack item : exact.getChoices()) {
				if (isVanillaPotionChoice(sample, item)) {
					if (matchesPotionContents(sample, item))
						return true;
					continue;
				}
				if (UIFUtils.isItemSimilar(sample, item, true))
					return true;
			}
			return false;
		}
		return choice.test(sample);
	}
	private static boolean isVanillaPotionChoice(ItemStack sample, ItemStack expected) {
		return sample.getItemMeta() instanceof PotionMeta && expected.getItemMeta() instanceof PotionMeta
				&& GenericItem.getItemBaseNoID(sample) == null && GenericItem.getItemBaseNoID(expected) == null;
	}
	@SuppressWarnings("removal")
	private static boolean matchesPotionContents(ItemStack sample, ItemStack expected) {
		if (sample.getType() != expected.getType())
			return false;
		PotionMeta sampleMeta = (PotionMeta) sample.getItemMeta();
		PotionMeta expectedMeta = (PotionMeta) expected.getItemMeta();
		return Objects.equals(sampleMeta.getBasePotionData(), expectedMeta.getBasePotionData())
				&& Objects.equals(sampleMeta.getColor(), expectedMeta.getColor())
				&& sampleMeta.getCustomEffects().equals(expectedMeta.getCustomEffects());
	}
}
