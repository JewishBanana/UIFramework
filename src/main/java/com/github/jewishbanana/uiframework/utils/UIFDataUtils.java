package com.github.jewishbanana.uiframework.utils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice.SlotOrder;

public class UIFDataUtils {
	
	private static UIFramework plugin;
	private static DecimalFormat decimalFormat;
	static {
		plugin = UIFramework.getInstance();
		decimalFormat = new DecimalFormat("0.#");
	}
	
	public static int getConfigInt(String path) {
		try {
			return plugin.getConfig().getInt(path);
		} catch (NumberFormatException e) {
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eWARNING while reading &dinteger &evalue from config path '"+path+"' please fix this value!"));
			return 0;
		}
	}
	public static double getConfigDouble(String path) {
		try {
			return plugin.getConfig().getDouble(path);
		} catch (NumberFormatException e) {
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eWARNING while reading &ddouble &evalue from config path '"+path+"' please fix this value!"));
			return 0.0;
		}
	}
	public static boolean getConfigBoolean(String path) {
		try {
			return plugin.getConfig().getBoolean(path);
		} catch (NumberFormatException e) {
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eWARNING while reading &dboolean &evalue from config path '"+path+"' please fix this value!"));
			return false;
		}
	}
	public static String getConfigString(String path) {
		try {
			return plugin.getConfig().getString(path);
		} catch (IllegalArgumentException e) {
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eWARNING while reading &dstring &evalue from config path '"+path+"' please fix this value!"));
			return null;
		}
	}
	public static int getDataFileInt(String path) {
		if (!UIFramework.dataFile.contains(path)) {
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eWARNING while reading &dinteger &evalue from data file path '"+path+"' the path is missing!"));
			return 0;
		}
		try {
			return UIFramework.dataFile.getInt(path);
		} catch (NumberFormatException e) {
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eWARNING while reading &dinteger &evalue from data file path '"+path+"' please fix this value!"));
			return 0;
		}
	}
	public static String formatEnchant(Enchantment enchant) {
		StringBuilder temp = new StringBuilder(enchant.getKey().getKey());
		temp.setCharAt(0, Character.toUpperCase(temp.charAt(0)));
		for (int i=1; i < temp.length(); i++)
			if (temp.charAt(i) == ' ' && temp.length() > i+1 && temp.charAt(i+1) != ' ')
				temp.setCharAt(i+1, Character.toUpperCase(temp.charAt(i+1)));
		return temp.toString();
	}
	public static <T> String getDecimalFormatted(T num) {
		return decimalFormat.format(num);
	}
	public static double map(double value, double istart, double istop, double ostart, double ostop) {
		return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
	}
	public static boolean isEqualsNoNull(Object arg1, Object arg2) {
		if (arg1 == null || arg2 == null)
			return false;
		return arg1.equals(arg2);
	}
	public static <T extends Recipe & Keyed> void writeRecipeToSection(ConfigurationSection section, T recipe) {
		String recipeType = null;
		if (recipe instanceof ShapedRecipe)
			recipeType = "shaped";
		else if (recipe instanceof ShapelessRecipe)
			recipeType = "shapeless";
		else if (recipe instanceof AnvilRecipe)
			recipeType = "anvil";
		if (recipeType == null)
			throw new IllegalArgumentException("[UIFramework]: Cannot write recipe to file because the recipe type "+recipe.getClass().getName()+" is not supported!");
		section.set("type", recipeType);
		switch (recipeType) {
		case "shaped":
			ShapedRecipe shaped = (ShapedRecipe) recipe;
			section.set("shape", Arrays.asList(shaped.getShape()));
			section.createSection("exact");
			section.createSection("material");
			shaped.getChoiceMap().forEach((k, v) -> {
				if (v != null) {
					if (v instanceof RecipeChoice.ExactChoice)
						section.set("exact."+k, ((RecipeChoice.ExactChoice) v).getChoices());
					else
						section.set("material."+k, ((RecipeChoice.MaterialChoice) v).getChoices().stream().map(e -> e.toString()).collect(Collectors.toList()));
				}
			});
			break;
		case "shapeless":
			ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
			section.set("exact", shapeless.getChoiceList().stream().filter(k -> k instanceof RecipeChoice.ExactChoice).map(k -> ((RecipeChoice.ExactChoice) k).getChoices()).collect(Collectors.toList()));
			section.set("material", shapeless.getChoiceList().stream().filter(k -> k instanceof RecipeChoice.MaterialChoice).map(k -> ((RecipeChoice.MaterialChoice) k).getChoices().stream().map(l -> l.toString()).collect(Collectors.toList())).collect(Collectors.toList()));
			break;
		case "anvil":
			AnvilRecipe anvil = (AnvilRecipe) recipe;
			section.set("isRepair", anvil.isRepair());
			section.set("levelCost", anvil.getLevelCost());
			section.set("repairAmount", anvil.getRepairAmount());
			if (anvil.getAnvilChoice().getFirstSlot() instanceof RecipeChoice.ExactChoice) {
				section.set("firstType", "exact");
				section.set("first", ((RecipeChoice.ExactChoice) anvil.getAnvilChoice().getFirstSlot()).getChoices());
			} else {
				section.set("firstType", "material");
				section.set("first", ((RecipeChoice.MaterialChoice) anvil.getAnvilChoice().getFirstSlot()).getChoices().stream().map(e -> e.toString()).collect(Collectors.toList()));
			}
			if (anvil.getAnvilChoice().getSecondSlot() instanceof RecipeChoice.ExactChoice) {
				section.set("secondType", "exact");
				section.set("second", ((RecipeChoice.ExactChoice) anvil.getAnvilChoice().getSecondSlot()).getChoices());
			} else {
				section.set("secondType", "material");
				section.set("second", ((RecipeChoice.MaterialChoice) anvil.getAnvilChoice().getSecondSlot()).getChoices().stream().map(e -> e.toString()).collect(Collectors.toList()));
			}
			section.set("slotOrder", anvil.getAnvilChoice().getSlotOrder().toString());
			break;
		}
	}
	@SuppressWarnings("unchecked")
	public static <T extends Recipe & Keyed> T createRecipeFromSection(ConfigurationSection section, ItemStack result, NamespacedKey key) {
		try {
			String shape = section.getString("type");
			switch (shape) {
			default:
				return null;
			case "shaped":
				ShapedRecipe shapedRecipe = new ShapedRecipe(key, result);
				shapedRecipe.shape(section.getStringList("shape").toArray(new String[0]));
				for (String s : section.getConfigurationSection("exact").getKeys(false)) {
					shapedRecipe.setIngredient(s.charAt(0), new RecipeChoice.ExactChoice(((List<ItemStack>) section.get("exact."+s))));
				}
				for (String s : section.getConfigurationSection("material").getKeys(false))
					shapedRecipe.setIngredient(s.charAt(0), new RecipeChoice.MaterialChoice(((List<String>) section.get("material."+s)).stream().map(e -> Material.getMaterial(e)).collect(Collectors.toList())));
				return (T) shapedRecipe;
			case "shapeless":
				ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, result);
				for (List<ItemStack> list : ((List<List<ItemStack>>) section.get("exact")))
					shapelessRecipe.addIngredient(new RecipeChoice.ExactChoice(list));
				for (List<String> list : ((List<List<String>>) section.get("material")))
					shapelessRecipe.addIngredient(new RecipeChoice.MaterialChoice(list.stream().map(k -> Material.getMaterial(k)).collect(Collectors.toList())));
				return (T) shapelessRecipe;
			case "anvil":
				RecipeChoice firstSlot = null;
				if (section.getString("firstType").equals("exact"))
					firstSlot = new RecipeChoice.ExactChoice((List<ItemStack>) section.get("first"));
				else
					firstSlot = new RecipeChoice.MaterialChoice(section.getStringList("first").stream().map(e -> Material.valueOf(e)).collect(Collectors.toList()));
				RecipeChoice secondSlot = null;
				if (section.getString("secondType").equals("exact"))
					firstSlot = new RecipeChoice.ExactChoice((List<ItemStack>) section.get("second"));
				else
					firstSlot = new RecipeChoice.MaterialChoice(section.getStringList("second").stream().map(e -> Material.valueOf(e)).collect(Collectors.toList()));
				if (!section.getBoolean("isRepair"))
					return (T) new AnvilRecipe(key, new AnvilChoice(firstSlot, secondSlot, SlotOrder.valueOf(section.getString("slotOrder"))), result, section.getInt("levelCost"));
				else
					return (T) new AnvilRecipe(key, firstSlot, secondSlot, section.getInt("repairAmount"), section.getInt("levelCost"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR could not read recipe data from &e'"+section+"' &cplease fix this!"));
			return null;
		}
	}
}
