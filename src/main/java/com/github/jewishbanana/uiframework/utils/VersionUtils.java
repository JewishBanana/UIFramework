package com.github.jewishbanana.uiframework.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public class VersionUtils {
	
	private static final Integer[] mcVersion;
	
	private static final Enchantment sharpness;
	private static final Enchantment smite;
	private static final Enchantment arthropods;
	
	private static final Map<Enchantment, String> enchantNames = new HashMap<>();
	
	private static final PotionEffectType strength;
	
	private static final Particle item_crack;
	
	static {
		mcVersion = Arrays.stream(Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf('-')).split("\\.")).map(e -> Integer.parseInt(e)).toArray(Integer[]::new);
		
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
		
		if (isMCVersionOrAbove("1.20.5"))
			item_crack = Particle.ITEM;
		else
			item_crack = Particle.valueOf("ITEM_CRACK");
	}
	public static boolean isMCVersionOrAbove(String version) {
		try {
			String[] test = version.split("\\.");
			for (int i = 0; i < test.length; i++) {
	            if (i >= mcVersion.length)
	                return false;
	            int testSegment = Integer.parseInt(test[i]);
	            if (mcVersion[i] > testSegment)
	                return true;
	            else if (mcVersion[i] < testSegment)
	                return false;
	        }
	        return true;
		} catch (NumberFormatException ex) {
			throw new NumberFormatException("The version string you supplied '"+version+"' is not a valid version string! Format must be as follows: '1.2.3' or '1.2' or '1'!");
		}
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
	public static Particle getItemCrack() {
		return item_crack;
	}
}
