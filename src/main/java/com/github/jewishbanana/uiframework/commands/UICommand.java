package com.github.jewishbanana.uiframework.commands;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.entities.CustomEntity;
import com.github.jewishbanana.uiframework.entities.UIEntityManager;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.UIEnchantment;
import com.github.jewishbanana.uiframework.items.UIItemType;
import com.github.jewishbanana.uiframework.listeners.menus.ItemsMenu;
import com.github.jewishbanana.uiframework.listeners.menus.MenuManager;
import com.github.jewishbanana.uiframework.listeners.menus.RecipeMenu;
import com.github.jewishbanana.uiframework.utils.UIFUtils;
import com.mojang.datafixers.util.Pair;

public class UICommand implements CommandExecutor, TabCompleter {
	
	private UIFramework plugin;
	private String usage;
	
	public UICommand(UIFramework plugin) {
		this.plugin = plugin;
		this.usage = UIFUtils.convertString("&cUsage: /uiframework <give|enchant|summon|recipes|entities|help|reload>...");
		
		plugin.getCommand("uiframework").setExecutor(this);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework help"));
			return true;
		}
		switch (args[0].toLowerCase()) {
		case "give":
			if (!(sender instanceof Player)) {
				if (args.length < 3 || args.length > 4) {
					sender.sendMessage("Usage: /uiframework give <item> <player> [amount]");
					return true;
				}
			} else if (!sender.hasPermission("ultimateitems.give")) {
				sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.permission_error")));
				return true;
			}
			if (args.length < 2 || args.length > 4) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework give <item> [player] [amount]"));
				return true;
			}
			UIItemType id = UIItemType.getItemType(args[1]);
			if (id == null) {
				sender.sendMessage(UIFUtils.convertString("&cThere is no item '"+args[1]+"'!"));
				return true;
			}
			Player target = null;
			if (args.length == 3) {
				for (Player p : Bukkit.getOnlinePlayers())
					if (p.getName().equalsIgnoreCase(args[2])) {
						target = p;
						break;
					}
				if (target == null) {
					sender.sendMessage(UIFUtils.convertString("&cCould not find player '"+args[2]+"'!"));
					return true;
				}
			} else
				target = (Player) sender;
			if (target.getInventory().firstEmpty() == -1) {
				sender.sendMessage(UIFUtils.convertString("&cPlayer's inventory is full!"));
				return true;
			}
			int amount = 1;
			if (args.length == 4) {
				try {
					amount = Integer.parseInt(args[3]);
				} catch (NumberFormatException ex) {
					sender.sendMessage(UIFUtils.convertString("&cThe amount must be a valid integer!"));
					return true;
				}
			}
			ItemStack item = id.getItem();
			GenericItem giveBase = GenericItem.getItemBase(item);
			if (giveBase != null)
				giveBase.refreshItemLore();
			int given = 0;
			for (int i=0; i < amount; i++) {
				target.getInventory().addItem(item);
				given++;
				if (target.getInventory().firstEmpty() == -1) {
					sender.sendMessage(UIFUtils.convertString("&aGave &b"+given+" &a["+giveBase.getDisplayName()+"&a] to "+target.getName()+" &7(Targets inventory was full and could not give the full amount)"));
					target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
					return true;
				}
			}
			sender.sendMessage(UIFUtils.convertString("&aGave &b"+given+" &a["+giveBase.getDisplayName()+"&a] to "+target.getName()));
			target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
			return true;
		case "reload":
			if (sender instanceof Player && !sender.hasPermission("uiframework.reload")) {
				sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.permission_error")));
				return true;
			}
			plugin.reload();
			sender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&aSuccessfully reloaded the config!"));
			return true;
		case "recipes":
			if (!(sender instanceof Player)) {
				sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.console_error_message")));
				return true;
			} else if (!sender.hasPermission("uiframework.recipes")) {
				sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.permission_error")));
				return true;
			}
			if (args.length > 2) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework recipes [item]"));
				return true;
			}
			if (args.length == 2) {
				UIItemType type = UIItemType.getItemType(args[1]);
				if (type == null) {
					sender.sendMessage(UIFUtils.convertString("&cThere is no such item named '"+args[1]+"'!"));
					return true;
				}
				RecipeMenu menu = new RecipeMenu(type, GenericItem.getItemBaseNoID(type.getBuilder().getItem()).getDisplayName(), 1, 1, ((Player) sender).hasPermission("ultimateitems.modifyRecipes"));
				MenuManager.registerInventory(menu.getInventory(), menu);
				((Player) sender).openInventory(menu.getInventory());
				return true;
			}
			ItemsMenu menu = new ItemsMenu(1, sender.hasPermission("ultimateitems.modifyRecipes"));
			MenuManager.registerInventory(menu.getInventory(), menu);
			((Player) sender).openInventory(menu.getInventory());
			return true;
		case "enchant":
			if (!(sender instanceof Player)) {
				sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.console_error_message")));
				return true;
			} else if (!sender.hasPermission("uiframework.enchant")) {
				sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.permission_error")));
				return true;
			}
			if (args.length < 2 || args.length > 4) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework enchant <enchantment> [level] [force]"));
				return true;
			}
			UIEnchantment enchant = UIEnchantment.getEnchant(args[1]);
			if (enchant == null) {
				sender.sendMessage(UIFUtils.convertString("&cThere is no custom enchantment named '"+args[1]+"'!"));
				return true;
			}
			int level = 1;
			if (args.length >= 3)
				try {
					level = Integer.parseInt(args[2]);
				} catch (NumberFormatException exception) {
					sender.sendMessage(UIFUtils.convertString("&c'"+args[2]+"' is not a valid level!"));
					return true;
				}
			if (level < 1) {
				sender.sendMessage(UIFUtils.convertString("&cLevel cannot be lower than 1!"));
				return true;
			}
			if (level > enchant.getMaxLevel()) {
				sender.sendMessage(UIFUtils.convertString("&c'"+args[1]+"' does not allow a higher level than "+enchant.getMaxLevel()+"!"));
				return true;
			}
			ItemStack hand = ((Player) sender).getEquipment().getItemInMainHand();
			if (hand.getType() == Material.AIR) {
				sender.sendMessage(UIFUtils.convertString("&cYou must be holding an item to enchant!"));
				return true;
			}
			GenericItem base = GenericItem.createItemBase(hand);
			if (base == null) {
				sender.sendMessage(UIFUtils.convertString("&cFailed to apply the custom enchant to your hand item!"));
				return true;
			}
			if (args.length == 4)
				if (args[3].equalsIgnoreCase("true")) {
					if (enchant.addEnchant(base, level, true, true)) {
						base.getType().getBuilder().assembleLore(base.getItem(), base.getItem().getItemMeta(), base.getType(), base);
						((Player) sender).getEquipment().setItemInMainHand(base.getItem(), true);
						sender.sendMessage(UIFUtils.convertString("&aEnchanted your hand item with &7"+enchant.getTrimmedName()+' '+UIFUtils.getNumerical(level)));
						return true;
					}
					sender.sendMessage(UIFUtils.convertString("&cFailed to apply the custom enchant to your hand item!"));
					return true;
				} else if (!args[3].equalsIgnoreCase("false")) {
					sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework enchant <enchantment> [true|false]"));
					return true;
				}
			if (!enchant.canBeEnchanted(hand)) {
				sender.sendMessage(UIFUtils.convertString("&cYour hand item is not compatible with the enchant "+enchant.getTrimmedName()));
				return true;
			}
			if (enchant.addEnchant(base, level, true, true)) {
				base.getType().getBuilder().assembleLore(base.getItem(), base.getItem().getItemMeta(), base.getType(), base);
				((Player) sender).getEquipment().setItemInMainHand(base.getItem(), true);
				sender.sendMessage(UIFUtils.convertString("&aEnchanted your hand item with &7"+enchant.getTrimmedName()+' '+UIFUtils.getNumerical(level)));
				return true;
			}
			sender.sendMessage(UIFUtils.convertString("&cFailed to apply the custom enchant to your hand item!"));
			return true;
		case "summon":
			Location toSpawn = null;
			Player targetToSpawn = null;
			if (sender instanceof Player) {
				if (!sender.hasPermission("uiframework.summon")) {
					sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.permission_error")));
					return true;
				}
				targetToSpawn = (Player) sender;
				toSpawn = targetToSpawn.getLocation();
			}
			if (args.length < 2) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework summon <entity> [player|x y z] [world]"));
				return true;
			}
			UIEntityManager type = UIEntityManager.getEntityType(args[1]);
			if (type == null) {
				sender.sendMessage(UIFUtils.convertString("&cThere is no such registered entity type with the name '"+args[1]+"'!"));
				return true;
			}
			World spawnWorld = null;
			if (args.length == 2) {
				if (toSpawn == null) {
					sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework summon <entity> <player|x y z> [world]"));
					return true;
				}
			} else if (args.length == 3) {
				for (Player p : Bukkit.getOnlinePlayers())
					if (p.getName().equalsIgnoreCase(args[2])) {
						targetToSpawn = p;
						break;
					}
				if (targetToSpawn == null) {
					sender.sendMessage(UIFUtils.convertString("&cCould not find player '"+args[2]+"'!"));
					return true;
				}
				toSpawn = targetToSpawn.getLocation();
			} else if (args.length >= 4) {
				if (toSpawn != null)
					spawnWorld = toSpawn.getWorld();
				else
					spawnWorld = args.length == 6 ? Bukkit.getWorld(args[5]) : Bukkit.getWorlds().get(0);
				if (spawnWorld == null) {
					sender.sendMessage(UIFUtils.convertString("&cCould not find world!"));
					return true;
				}
				try {
					toSpawn = new Location(spawnWorld, Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4])).add(.5, 0, .5);
				} catch (NumberFormatException e) {
					sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework summon <entity> <x> <y> <z> [world]"));
					return true;
				}
			} else {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework summon <entity> [player|x y z]"));
				return true;
			}
			UISummonCommandParameters parameters = new UISummonCommandParameters(sender, toSpawn, targetToSpawn);
			if (type.getSpawnByCommandConditions() != null && !type.getSpawnByCommandConditions().test(parameters))
				return true;
			CustomEntity<? extends Entity> customEntity = UIEntityManager.spawnEntity(parameters.location, type.getEntityClass());
			if (customEntity == null) {
				sender.sendMessage(UIFUtils.convertString("&cAn error occurred while trying to spawn this entity!"));
				return true;
			}
			sender.sendMessage(UIFUtils.convertString("&aSummoned &f"+customEntity.getDisplayName()+" &aat &b"+toSpawn.getBlockX()+' '+toSpawn.getBlockY()+' '+toSpawn.getBlockZ()+(spawnWorld == null ? "" : " &9("+toSpawn.getWorld().getName()+')')));
			return true;
		case "entities":
			if (sender instanceof Player) {
				if (!sender.hasPermission("uiframework.entities")) {
					sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.permission_error")));
					return true;
				}
			}
			if (args.length < 2) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework entities <list|kill> [species] [world]"));
				return true;
			}
			UIEntityManager entityType = null;
			World searchWorld = null;
			if (args.length >= 3) {
				entityType = UIEntityManager.getEntityType(args[2]);
				if (entityType == null) {
					sender.sendMessage(UIFUtils.convertString("&cThere is no such registered entity type with the name '"+args[2]+"'!"));
					return true;
				}
				if (args.length >= 4) {
					searchWorld = Bukkit.getWorld(args[3]);
					if (searchWorld == null) {
						sender.sendMessage(UIFUtils.convertString("&cCould not find world '"+args[3]+"'!"));
						return true;
					}
				}
			}
			Queue<Entity> entities = new ArrayDeque<>();
			if (searchWorld != null)
				entities.addAll(searchWorld.getEntities());
			else if (sender instanceof Player)
				entities.addAll(((Player) sender).getWorld().getEntities());
			else
				Bukkit.getWorlds().forEach(w -> entities.addAll(w.getEntities()));
			if (args[1].equalsIgnoreCase("list")) {
				if (entityType == null) {
					Map<String, Pair<String, Integer>> map = new HashMap<>();
					entities.forEach(e -> {
						CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(e);
						if (entity == null)
							return;
						Pair<String, Integer> pair = map.get(entity.getDisplayName());
						if (pair == null)
							map.put(entity.getDisplayName(), Pair.of(UIEntityManager.getRegistry().entrySet().stream().filter(entry -> entry.getValue().getEntityClass().equals(entity.getClass())).findFirst().get().getKey(), 1));
						else
							map.replace(entity.getDisplayName(), Pair.of(pair.getFirst(), pair.getSecond() + 1));
					});
					sender.sendMessage(UIFUtils.convertString("&aCurrently spawned entities"+(searchWorld == null ? "" : " in world &d"+searchWorld.getName()+"&a")+":"));
					if (UIFUtils.usingSpigot && sender instanceof Player)
						map.forEach((k, v) -> {
							net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(UIFUtils.convertString("&3- &f"+k+" &7- &f"+v.getSecond()));
							message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/ui entities list "+v.getFirst()));
							((Player) sender).spigot().sendMessage(message);
						});
					else
						map.forEach((k, v) -> sender.sendMessage(UIFUtils.convertString("&3- &f"+k+" &7- &f"+v.getSecond())));
					return true;
				} else {
					DecimalFormat format = new DecimalFormat("0.0");
					sender.sendMessage(UIFUtils.convertString("&aCurrently spawned &6"+args[2]+(searchWorld == null ? "" : " &ain world &d"+searchWorld.getName())+"&a:"));
					for (Entity e : entities) {
						CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(e);
						if (entity == null || !entity.getClass().equals(entityType.getEntityClass()))
							continue;
						String health = "&f(N/A)";
						if (e instanceof LivingEntity) {
							LivingEntity le = (LivingEntity) e;
							double maxHealth = le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
							if (le.getHealth() <= maxHealth / 2.0)
								health = "&e("+format.format(le.getHealth())+'/'+format.format(maxHealth)+')';
							else if (le.getHealth() <= maxHealth / 4.0)
								health = "&c("+format.format(le.getHealth())+'/'+format.format(maxHealth)+')';
							else
								health = "&a("+format.format(le.getHealth())+'/'+format.format(maxHealth)+')';
						}
						Location entityLoc = e.getLocation();
						if (UIFUtils.usingSpigot && sender instanceof Player) {
							net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(UIFUtils.convertString("&3- &f"+e.getCustomName()+' '+health+" &7at &b"+entityLoc.getBlockX()+' '+entityLoc.getBlockY()+' '+entityLoc.getBlockZ()));
							message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/tp @s "+entityLoc.getBlockX()+' '+entityLoc.getBlockY()+' '+entityLoc.getBlockZ()));
							((Player) sender).spigot().sendMessage(message);
						} else
							sender.sendMessage(UIFUtils.convertString("&3- &f"+e.getCustomName()+' '+health+" &7at &b"+entityLoc.getBlockX()+' '+entityLoc.getBlockY()+' '+entityLoc.getBlockZ()+(sender instanceof Player && searchWorld == null ? "" : " &6("+entityLoc.getWorld().getName()+')')));
					}
					return true;
				}
			} else if (args[1].equalsIgnoreCase("kill")) {
				int count = 0;
				if (entityType == null) {
					for (Entity e : entities) {
						if (UIEntityManager.getEntity(e) == null)
							continue;
						e.remove();
						count++;
					}
					sender.sendMessage(UIFUtils.convertString("&bRemoved &f"+count+" &b"+(count == 1 ? "entity" : "entities")+'!'));
					return true;
				} else {
					for (Entity e : entities) {
						CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(e);
						if (entity == null || !entity.getClass().equals(entityType.getEntityClass()))
							continue;
						e.remove();
						count++;
					}
					sender.sendMessage(UIFUtils.convertString("&bRemoved &f"+count+" &bentities of the species &a"+args[2]+(searchWorld == null ? "" : " &bfrom world &6("+searchWorld.getName()+')')+"&b!"));
					return true;
				}
			}
			sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework entities <list|kill> [species] [world]"));
			return true;
		default:
			break;
		}
		if (sender instanceof Player && !sender.hasPermission("uiframework.*")) {
			sender.sendMessage(UIFUtils.convertString(UIFramework.getLangString("commands.permission_error")));
			return true;
		}
		sender.sendMessage(usage);
		return true;
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> list = new ArrayList<>();
		String keyword;
		switch (args.length) {
		default:
		case 0:
			return list;
		case 1:
			keyword = args[0].toLowerCase();
			if (sender.hasPermission("uiframework.give"))
				list.add("give");
			if (sender.hasPermission("uiframework.reload"))
				list.add("reload");
			if (sender.hasPermission("uiframework.recipes"))
				list.add("recipes");
			if (sender.hasPermission("uiframework.enchant"))
				list.add("enchant");
			if (sender.hasPermission("uiframework.summon"))
				list.add("summon");
			if (sender.hasPermission("uiframework.entities"))
				list.add("entities");
			list.removeIf(e -> !e.contains(keyword));
			break;
		case 2:
			keyword = args[1].toLowerCase();
			if ((args[0].equalsIgnoreCase("give") && sender.hasPermission("uiframework.give")) || (args[0].equalsIgnoreCase("recipes") && sender.hasPermission("uiframework.recipes"))) {
				list.addAll(UIItemType.getRegistry().keySet().stream().filter(e -> !e.equals("_null") && e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			if (args[0].equalsIgnoreCase("enchant") && sender.hasPermission("uiframework.enchant")) {
				list.addAll(UIEnchantment.getRegistry().keySet().stream().filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			if (args[0].equalsIgnoreCase("summon") && sender.hasPermission("uiframework.summon")) {
				list.addAll(UIEntityManager.getRegistry().keySet().stream().filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			if (args[0].equalsIgnoreCase("entities") && sender.hasPermission("uiframework.entities")) {
				list.addAll(Arrays.asList("list", "kill").stream().filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			break;
		case 3:
			keyword = args[2].toLowerCase();
			if ((args[0].equalsIgnoreCase("give") && sender.hasPermission("uiframework.give")) || (args[0].equalsIgnoreCase("summon") && sender.hasPermission("uiframework.summon"))) {
				list.addAll(Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName()).filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			if (args[0].equalsIgnoreCase("entities") && sender.hasPermission("uiframework.entities")) {
				list.addAll(UIEntityManager.getRegistry().keySet().stream().filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			break;
		case 4:
			keyword = args[3].toLowerCase();
			if (args[0].equalsIgnoreCase("enchant") && sender.hasPermission("uiframework.enchant")) {
				list.addAll(Arrays.asList("true", "false").stream().filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			if (args[0].equalsIgnoreCase("entities") && sender.hasPermission("uiframework.entities")) {
				list.addAll(Bukkit.getWorlds().stream().filter(e -> e.getName().contains(keyword)).map(e -> e.getName()).collect(Collectors.toList()));
				return list;
			}
			break;
		case 6:
			keyword = args[5].toLowerCase();
			if (args[0].equalsIgnoreCase("summon") && sender.hasPermission("uiframework.summon")) {
				list.addAll(Bukkit.getWorlds().stream().filter(e -> e.getName().contains(keyword)).map(e -> e.getName()).collect(Collectors.toList()));
				return list;
			}
			break;
		}
		return list;
	}
	public class UISummonCommandParameters {
		
		public CommandSender sender;
		public Location location;
		public Player target;
		
		private UISummonCommandParameters(CommandSender sender, Location location, Player target) {
			this.sender = sender;
			this.location = location;
			this.target = target;
		}
	}
}
