package com.github.jewishbanana.uiframework.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.RedstoneWire.Connection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.jewishbanana.uiframework.items.ActivatedSlot;
import com.github.jewishbanana.uiframework.items.GenericItem;

public class UIFUtils {
	
	public static int descriptionLine;
	public static String prefix;
	private static Pattern hexPattern;
	public static boolean usingSpigot;
	private static Random random;
	private static Map<DyeColor, ChatColor> dyeChatMap = new HashMap<>();
	static
	{
		random = new Random();
		hexPattern = Pattern.compile("\\(hex:#[a-fA-F0-9]{6}\\)");
		prefix = convertString("&a[UIFramework]: ");
		
		dyeChatMap.put(DyeColor.BLACK, ChatColor.BLACK);
		dyeChatMap.put(DyeColor.BLUE, ChatColor.DARK_BLUE);
		dyeChatMap.put(DyeColor.BROWN, ChatColor.GOLD);
		dyeChatMap.put(DyeColor.CYAN, ChatColor.AQUA);
		dyeChatMap.put(DyeColor.GRAY, ChatColor.DARK_GRAY);
		dyeChatMap.put(DyeColor.GREEN, ChatColor.DARK_GREEN);
		dyeChatMap.put(DyeColor.LIGHT_BLUE, ChatColor.BLUE);
		dyeChatMap.put(DyeColor.LIGHT_GRAY, ChatColor.GRAY);
		dyeChatMap.put(DyeColor.LIME, ChatColor.GREEN);
		dyeChatMap.put(DyeColor.MAGENTA, ChatColor.LIGHT_PURPLE);
		dyeChatMap.put(DyeColor.ORANGE, ChatColor.GOLD);
		dyeChatMap.put(DyeColor.PINK, ChatColor.LIGHT_PURPLE);
		dyeChatMap.put(DyeColor.PURPLE, ChatColor.DARK_PURPLE);
		dyeChatMap.put(DyeColor.RED, ChatColor.DARK_RED);
		dyeChatMap.put(DyeColor.WHITE, ChatColor.WHITE);
		dyeChatMap.put(DyeColor.YELLOW, ChatColor.YELLOW);
		
		try {
	        Class.forName("org.bukkit.entity.Player$Spigot");
	        usingSpigot = true;
	    } catch (Throwable tr) {
	    	usingSpigot = false;
	    }
	}
	
	public static String convertString(String text) {
		if (text == null)
			return null;
		String s = text;
		Matcher match = hexPattern.matcher(s);
		if (usingSpigot) {
		    while (match.find()) {
		        String color = s.substring(match.start(), match.end());
		        s = s.replace(color, net.md_5.bungee.api.ChatColor.of(color.substring(5, color.length()-1))+"");
		        match = hexPattern.matcher(s);
		    }
		    return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
		}
	    while (match.find()) {
	        String color = s.substring(match.start(), match.end());
	        Color col = Color.decode(color);
	        s = s.replace(color, dyeChatMap.getOrDefault(DyeColor.getByColor(org.bukkit.Color.fromRGB(col.getRed(), col.getGreen(), col.getBlue())), ChatColor.WHITE)+"");
	        match = hexPattern.matcher(s);
	    }
	    return ChatColor.translateAlternateColorCodes('&', s);
	}
	public static List<String> chopLore(List<String> lore) {
		List<String> tempLore = new ArrayList<>();
		if (lore != null)
			for (String line : lore) {
				line = UIFUtils.convertString(line);
				int offset = 0;
				for (int i=0; i < line.length(); i++)
					if (line.charAt(i) == ChatColor.COLOR_CHAR)
						offset += 2;
				int max_length = descriptionLine + offset;
				if (line.length()-1 > max_length) {
					int c = 0;
					for (int i=max_length; i > 0; i--) {
						if (i == 0) {
							tempLore.add(UIFUtils.convertString(ChatColor.getLastColors(line.substring(0, c))+line.substring(c)));
							break;
						}
						if (line.charAt(i) == ' ') {
							tempLore.add(UIFUtils.convertString(ChatColor.getLastColors(line.substring(0, c+1))+line.substring(c, i)));
							c += i-c+1;
							if (i+max_length >= line.length()) {
								tempLore.add(UIFUtils.convertString(ChatColor.getLastColors(line.substring(0, c))+line.substring(c, line.length())));
								break;
							}
							i = c+max_length;
						}
					}
				} else
					tempLore.add(line);
			}
		return tempLore;
	}
	public static String getNumerical(int num) {
		switch (num) {
		case 1: return "I";
		case 2: return "II";
		case 3: return "III";
		case 4: return "IV";
		case 5: return "V";
		case 6: return "VI";
		case 7: return "VII";
		case 8: return "VIII";
		case 9: return "IX";
		case 10: return "X";
		default: return ""+num;
		}
	}
	@SuppressWarnings("deprecation")
	public static double getEnchantDamage(ItemStack item, LivingEntity toDamage) {
		ItemMeta meta = item.getItemMeta();
		double damage = 0.0;
		if (meta.hasEnchant(VersionUtils.getSharpness()))
			damage += 0.5 * (meta.getEnchantLevel(VersionUtils.getSharpness()) - 1) + 1.0;
		if (toDamage != null && meta.hasEnchant(VersionUtils.getSmite()) && toDamage.getCategory() == EntityCategory.UNDEAD)
			damage += 2.5 * meta.getEnchantLevel(VersionUtils.getSmite());
		if (toDamage != null && meta.hasEnchant(VersionUtils.getArthropods()) && toDamage.getCategory() == EntityCategory.ARTHROPOD)
			damage += 2.5 * meta.getEnchantLevel(VersionUtils.getArthropods());
		if (toDamage != null && meta.hasEnchant(Enchantment.IMPALING) && toDamage.getCategory() == EntityCategory.WATER)
			damage += 2.5 * meta.getEnchantLevel(Enchantment.IMPALING);
		return damage;
	}
	public static boolean isPlayerImmune(Player player) {
		GameMode mode = player.getGameMode();
		return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
	}
	public static boolean isInteractable(Block block) {
		Material type = block.getType();
		if (!type.isInteractable())
			return false;
		if (Tag.STAIRS.isTagged(type) || Tag.FENCES.isTagged(type) || Tag.CANDLES.isTagged(type) || Tag.CANDLE_CAKES.isTagged(type) || Tag.CAULDRONS.isTagged(type))
			return false;
		switch (type) {
		case MOVING_PISTON:
		case PUMPKIN:
		case CAKE:
			return false;
		case REDSTONE_WIRE:
			RedstoneWire wire = (RedstoneWire) block.getBlockData();
			for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST))
				if (wire.getFace(face) == Connection.NONE)
					continue;
				else if (wire.getFace(face) == Connection.SIDE) {
					if (block.getRelative(face).getType() == Material.REDSTONE_WIRE)
						return false;
				} else
					return false;
			return true;
		default:
			return true;
		}
	}
	public static boolean isItemSimilar(ItemStack sample, ItemStack expected, boolean ignoreName) {
		if (sample == null || expected == null || sample.getType() != expected.getType() || sample.hasItemMeta() != expected.hasItemMeta())
        	return false;
        GenericItem base = GenericItem.getItemBaseNoID(sample);
        ItemStack item = sample.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
        	if (ignoreName)
            	meta.setDisplayName(expected.getItemMeta().getDisplayName());
        	else if (!meta.getDisplayName().equals(expected.getItemMeta().getDisplayName()))
        		return false;
        	if (meta instanceof Damageable)
        		((Damageable) meta).setDamage(0);
        }
        if (base != null) {
        	base.getType().getBuilder().assembleLore(item, meta, base.getType(), null);
        	base.stripTags(meta);
    		return Bukkit.getItemFactory().equals(meta, expected.getItemMeta());
        }
        return (sample.hasItemMeta() ? Bukkit.getItemFactory().equals(meta, expected.getItemMeta()) : true);
	}
	public static Random getRandom() {
		return random;
	}
	public static Map<GenericItem, ActivatedSlot> getEntityContents(LivingEntity entity) {
		Map<GenericItem, ActivatedSlot> items = new LinkedHashMap<>();
		GenericItem mainHand = GenericItem.getItemBase(entity.getEquipment().getItemInMainHand());
		if (mainHand != null)
			items.put(mainHand, ActivatedSlot.MAIN_HAND);
		GenericItem offHand = GenericItem.getItemBase(entity.getEquipment().getItemInOffHand());
		if (offHand != null)
			items.put(offHand, ActivatedSlot.OFF_HAND);
		for (ItemStack stack : entity.getEquipment().getArmorContents()) {
			GenericItem base = GenericItem.getItemBase(stack);
			if (base != null)
				items.put(base, ActivatedSlot.ARMOR);
		}
		if (entity instanceof Player) {
			Player player = (Player) entity;
			for (int i=0; i < 36; i++) {
				if (i < 9) {
					GenericItem base = GenericItem.getItemBase(player.getInventory().getContents()[i]);
					if (base != null)
						items.putIfAbsent(base, ActivatedSlot.HOTBAR);
					continue;
				}
				GenericItem base = GenericItem.getItemBase(player.getInventory().getContents()[i]);
				if (base != null)
					items.put(base, ActivatedSlot.STORAGE);
			}
		}
		return items;
	}
	public static boolean isActivatingSlot(ActivatedSlot test, ActivatedSlot category, ActivatedSlot active, GenericItem base) {
		switch (category) {
		default:
		case ANY:
			return true;
		case PARENT:
			return isActivatingSlot(test, base.getActivatingSlot(), active, base);
		case ACTIVE_SLOT:
			if (test == active || active == ActivatedSlot.ANY)
				return true;
			break;
		case HAND:
			if (test == ActivatedSlot.HAND)
				return true;
		case MAIN_HAND:
			if (test == ActivatedSlot.MAIN_HAND)
				return true;
		case OFF_HAND:
			if (test == ActivatedSlot.OFF_HAND)
				return true;
			break;
		case ARMOR:
			if (test == ActivatedSlot.ARMOR)
				return true;
			break;
		case STORAGE:
			if (test == ActivatedSlot.STORAGE)
				return true;
		case HOTBAR:
			if (test == ActivatedSlot.HOTBAR)
				return true;
			break;
		}
		return false;
	}
}
