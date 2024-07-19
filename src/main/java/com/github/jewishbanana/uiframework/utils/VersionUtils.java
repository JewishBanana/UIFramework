package com.github.jewishbanana.uiframework.utils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public class VersionUtils {
	
	private static Enchantment sharpness;
	private static Enchantment smite;
	private static Enchantment arthropods;
	
	private static Map<Enchantment, String> enchantNames = new HashMap<>();
	
	private static PotionEffectType strength;
	
	static {
		sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
		smite = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("smite"));
		arthropods = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("bane_of_arthropods"));
		Enchantment luckOfTheSea = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("luck_of_the_sea"));
		
		Registry.ENCHANTMENT.forEach(enchant -> {
			if (enchant == arthropods || enchant == luckOfTheSea) {
				StringBuilder temp = new StringBuilder(enchant.getKey().getKey().replace('_', ' '));
				temp.setCharAt(0, Character.toUpperCase(temp.charAt(0)));
				for (int i=temp.length()-1; i > 0; i--)
					if (temp.charAt(i) == ' ') {
						temp.setCharAt(i+1, Character.toUpperCase(temp.charAt(i+1)));
						break;
					}
				enchantNames.put(enchant, temp.toString());
			} else {
				StringBuilder temp = new StringBuilder(enchant.getKey().getKey());
				temp.setCharAt(0, Character.toUpperCase(temp.charAt(0)));
				for (int i=1; i < temp.length(); i++)
					if (temp.charAt(i) == '_' && temp.length() > i+1) {
						temp.setCharAt(i, ' ');
						temp.setCharAt(i+1, Character.toUpperCase(temp.charAt(i+1)));
					}
				enchantNames.put(enchant, temp.toString());
			}
		});
		
		strength = Registry.EFFECT.get(NamespacedKey.minecraft("strength"));
	}
	
	public static Enchantment getSharpness() {
		return sharpness;
	}
	public static Enchantment getSmite() {
		return smite;
	}
	public static Enchantment getArthropods() {
		return arthropods;
	}
	public static String getFormattedEnchantName(Enchantment enchant) {
		return enchantNames.get(enchant);
	}
	public static PotionEffectType getStrength() {
		return strength;
	}
}
