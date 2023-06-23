package com.jewishbanana.uiframework.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import com.jewishbanana.uiframework.UIFramework;

public class UIFUtils {
	
	private static UIFramework plugin;
	private static Random rand;
	public static int descriptionLine;
	public static String prefix;
	private static Pattern hexPattern;
	private static boolean usingSpigot;
	private static Map<DyeColor, ChatColor> dyeChatMap = new HashMap<>();
	static
	{
		hexPattern = Pattern.compile("\\(hex:#[a-fA-F0-9]{6}\\)");
		plugin = UIFramework.getInstance();
		rand = new Random();
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
		default:
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
		}
	}
	public static double getEnchantDamage(ItemStack item, LivingEntity toDamage) {
		ItemMeta meta = item.getItemMeta();
		double damage = 0.0;
		if (meta.hasEnchant(Enchantment.DAMAGE_ALL))
			damage += 0.5 * (meta.getEnchantLevel(Enchantment.DAMAGE_ALL) - 1) + 1.0;
		if (toDamage != null && meta.hasEnchant(Enchantment.DAMAGE_UNDEAD) && toDamage.getCategory() == EntityCategory.UNDEAD)
			damage += 2.5 * meta.getEnchantLevel(Enchantment.DAMAGE_UNDEAD);
		if (toDamage != null && meta.hasEnchant(Enchantment.DAMAGE_ARTHROPODS) && toDamage.getCategory() == EntityCategory.ARTHROPOD)
			damage += 2.5 * meta.getEnchantLevel(Enchantment.DAMAGE_ARTHROPODS);
		if (toDamage != null && meta.hasEnchant(Enchantment.IMPALING) && toDamage.getCategory() == EntityCategory.WATER)
			damage += 2.5 * meta.getEnchantLevel(Enchantment.IMPALING);
		return damage;
	}
	public static ArmorStand lockArmorStand(ArmorStand stand, boolean setInvisible, boolean setGravity, boolean setMarker) {
		if (plugin.mcVersion >= 1.17)
			stand.setInvisible(setInvisible);
		else
			stand.setVisible(!setInvisible);
		stand.setGravity(setGravity);
		stand.setArms(true);
		stand.setMarker(setMarker);
		if (plugin.mcVersion >= 1.17) {
			stand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		return stand;
	}
	public static Location findSmartYSpawn(Location pivot, Location spawn, int height, int maxDistance) {
		Block b = spawn.getBlock();
		Location loc1 = null, loc2 = null;
		down:
			for (int i = spawn.getBlockY(); i > spawn.getBlockY()-maxDistance; i--) {
				b = b.getRelative(BlockFace.DOWN);
				if (!b.isPassable() && b.getRelative(BlockFace.UP).isPassable() && !b.getRelative(BlockFace.UP).isLiquid()) {
					for (int c = 2; c <= height-1; c++)
						if (!b.getRelative(BlockFace.UP, c).isPassable())
							continue down;
					loc1 = b.getRelative(BlockFace.UP).getLocation().add(0.5,0.01,0.5);
					break down;
				}
			}
		b = spawn.getBlock();
		up:
			for (int i = spawn.getBlockY(); i < spawn.getBlockY()+maxDistance; i++) {
				b = b.getRelative(BlockFace.UP);
				if (b.isPassable() && !b.getRelative(BlockFace.DOWN).isPassable() && !b.isLiquid()) {
					for (int c = 1; c < height; c++)
						if (!b.getRelative(BlockFace.UP, c).isPassable())
							continue up;
					loc2 = b.getLocation().add(0.5,0.01,0.5);
					break up;
				}
			}
		if (loc1 != null && loc2 == null)
			return loc1;
		else if (loc1 == null && loc2 != null)
			return loc2;
		else if (loc1 == null && loc2 == null)
			return null;
		if (Math.abs(pivot.getY()-loc2.getY()) < Math.abs(pivot.getY()-loc1.getY()))
			return loc1;
		else
			return loc2;
	}
	public static Vector getVectorTowards(Location initial, Location towards) {
		return new Vector(towards.getX() - initial.getX(), towards.getY() - initial.getY(), towards.getZ() - initial.getZ()).normalize();
	}
	public static void makeEntityFaceLocation(Entity entity, Location to) {
		Vector dirBetweenLocations = to.toVector().subtract(entity.getLocation().toVector());
		entity.teleport(entity.getLocation().setDirection(dirBetweenLocations));
    }
	public static void runConsoleCommand(String command, World world) {
		Entity entity = world.spawnEntity(new Location(world, 0, 0, 0), EntityType.MINECART_COMMAND);
		World tempWorld = Bukkit.getWorld("world");
		boolean gameRule = tempWorld.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
		tempWorld.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
		plugin.getServer().dispatchCommand(entity, command);
		tempWorld.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, gameRule);
		entity.remove();
	}
	public static void mergeEntityData(Entity entity, String data) {
		Location entityLoc = entity.getLocation();
		runConsoleCommand("data merge entity @e[x="+entityLoc.getX()+",y="+entityLoc.getY()+",z="+entityLoc.getZ()+",distance=..0.1,limit=1] "+data, entity.getWorld());
	}
	public static BlockFace getBlockFace(Player player) {
	    List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 100);
	    if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isOccluding())
	    	return null;
	    Block targetBlock = lastTwoTargetBlocks.get(1);
	    Block adjacentBlock = lastTwoTargetBlocks.get(0);
	    return targetBlock.getFace(adjacentBlock);
	}
	public static void pureDamageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, Entity source) {
		if (entity.isDead())
			return;
		EntityDamageEvent event = new EntityDamageEvent(entity, DamageCause.CUSTOM, damage);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;
		entity.damage(0.00001, source);
		if (entity.getHealth()-event.getDamage() <= 0) {
			if (!ignoreTotem) {
				entity.setHealth(0.00001);
				if (meta != null && entity.getEquipment().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && entity.getEquipment().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING)
					entity.setMetadata(meta, plugin.fixedData);
				entity.damage(1);
				return;
			}
			if (meta != null)
				entity.setMetadata(meta, plugin.fixedData);
			entity.setHealth(0);
			return;
		}
		entity.setHealth(Math.max(entity.getHealth()-event.getDamage(), 0));
	}
	public static void pureDamageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, Entity source, boolean runEvent) {
		if (entity.isDead())
			return;
		EntityDamageEvent event = new EntityDamageEvent(entity, DamageCause.CUSTOM, damage);
		if (runEvent) {
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled())
				return;
		}
		entity.damage(0.00001, source);
		if (entity.getHealth()-event.getDamage() <= 0) {
			if (!ignoreTotem) {
				entity.setHealth(0.00001);
				if (meta != null && entity.getEquipment().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING && entity.getEquipment().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING)
					entity.setMetadata(meta, plugin.fixedData);
				entity.damage(1);
				return;
			}
			if (meta != null)
				entity.setMetadata(meta, plugin.fixedData);
			entity.setHealth(0);
			return;
		}
		entity.setHealth(Math.max(entity.getHealth()-event.getDamage(), 0));
	}
	public static void damageArmor(LivingEntity entity, double damage) {
		int dmg = Math.max((int) (damage + 4 / 4), 1);
		for (ItemStack armor : entity.getEquipment().getArmorContents()) {
			if (armor == null || armor.getItemMeta() == null)
				continue;
			ItemMeta meta = armor.getItemMeta();
			if (((Damageable) meta).getDamage() >= armor.getType().getMaxDurability()) armor.setAmount(0);
			else ((Damageable) meta).setDamage(((Damageable) meta).getDamage()+dmg);
			armor.setItemMeta(meta);
		}
	}
	public static void damageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem) {
		EntityDamageEvent event = new EntityDamageEvent(entity, DamageCause.CUSTOM, damage);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;
		double armor = entity.getAttribute(Attribute.GENERIC_ARMOR).getValue();
		double toughness = entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
		double actualDamage = event.getDamage() * (1 - Math.min(20, Math.max(armor / 5, armor - event.getDamage() / (2 + toughness / 4))) / 25);
		UIFUtils.pureDamageEntity(entity, actualDamage, meta, ignoreTotem, null);
		UIFUtils.damageArmor(entity, actualDamage);
	}
	public static void damageEntity(LivingEntity entity, double damage, String meta, boolean ignoreTotem, Entity source) {
		EntityDamageEvent event = new EntityDamageEvent(entity, DamageCause.CUSTOM, damage);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;
		double armor = entity.getAttribute(Attribute.GENERIC_ARMOR).getValue();
		double toughness = entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
		double actualDamage = event.getDamage() * (1 - Math.min(20, Math.max(armor / 5, armor - event.getDamage() / (2 + toughness / 4))) / 25);
		UIFUtils.pureDamageEntity(entity, actualDamage, meta, ignoreTotem, source);
		UIFUtils.damageArmor(entity, actualDamage);
	}
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist) {
		for (int i=0; i < maxAttempts; i++) {
			Location tempLoc = location.clone();
			Vector tempVec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize();
			for (int c=0; c < maxRange; c++) {
				tempLoc.add(tempVec);
				Block b = tempLoc.getBlock();
				if (!b.isPassable()) {
					if (c < minRange || (materialWhitelist != null && !materialWhitelist.contains(b.getType())))
						break;
					return b;
				}
			}
		}
		return null;
	}
	public static Block rayCastForBlock(Location location, int minRange, int maxRange, int maxAttempts, Set<Material> materialWhitelist, Set<Block> blockWhitelist) {
		for (int i=0; i < maxAttempts; i++) {
			Location tempLoc = location.clone();
			Vector tempVec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize();
			for (int c=0; c < maxRange; c++) {
				tempLoc.add(tempVec);
				Block b = tempLoc.getBlock();
				if (!b.isPassable()) {
					if (c < minRange || !blockWhitelist.contains(b) || (materialWhitelist != null && !materialWhitelist.contains(b.getType())))
						break;
					return b;
				}
			}
		}
		return null;
	}
	public static void damageItem(ItemStack toDamage, int damage) {
		ItemMeta meta = toDamage.getItemMeta();
		((Damageable) meta).setDamage(((Damageable) meta).getDamage()+damage);
		if (((Damageable) meta).getDamage() >= toDamage.getType().getMaxDurability())
			toDamage.setAmount(0);
		else toDamage.setItemMeta(meta);
	}
	public static void repairItem(ItemStack toRepair, int health) {
		ItemMeta meta = toRepair.getItemMeta();
		((Damageable) meta).setDamage(Math.max(((Damageable) meta).getDamage()-health, 0));
		toRepair.setItemMeta(meta);
	}
	public static boolean isEnvironment(World world, Environment environment) {
		return world.getEnvironment() == environment || world.getEnvironment() == Environment.CUSTOM;
	}
	public static Vector randomVector() {
		return new Vector(rand.nextDouble()*2-1.0, rand.nextDouble()*2-1.0, rand.nextDouble()*2-1.0);
	}
	public static EquipmentSlot getEquipmentSlot(EntityEquipment inventory, ItemStack item) {
		for (EquipmentSlot slot : EquipmentSlot.values())
			if (inventory.getItem(slot).equals(item))
				return slot;
		return null;
	}
	public static boolean isPlayerImmune(Player player) {
		GameMode mode = player.getGameMode();
		return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
	}
}
