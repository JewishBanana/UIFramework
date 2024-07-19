package com.github.jewishbanana.uiframework.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.ItemType;
import com.github.jewishbanana.uiframework.items.UIEnchantment;
import com.github.jewishbanana.uiframework.listeners.menus.ItemsMenu;
import com.github.jewishbanana.uiframework.listeners.menus.MenuManager;
import com.github.jewishbanana.uiframework.listeners.menus.RecipeMenu;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class UICommand implements CommandExecutor,TabCompleter {
	
	private UIFramework plugin;
	
	public UICommand(UIFramework plugin) {
		this.plugin = plugin;
		
		plugin.getCommand("uiframework").setExecutor(this);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework <help|reload|give|recipes|enchant>..."));
			return true;
		}
		switch (args[0].toLowerCase()) {
		case "give":
			if (!(sender instanceof Player)) {
				if (args.length < 3 || args.length > 4) {
					sender.sendMessage("Usage: /uiframework give <item> <player> [amount]");
					return true;
				}
			} else if (!((Player) sender).hasPermission("ultimateitems.give")) {
				sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.permission_error")));
				return true;
			}
			if (args.length < 2 || args.length > 4) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework give <item> [player] [amount]"));
				return true;
			}
			ItemType id = ItemType.getItemType(args[1]);
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
			ItemStack item = id.getBuilder().getItem();
			GenericItem giveBase = GenericItem.getItemBase(item);
			if (giveBase != null)
				giveBase.getType().getBuilder().assembleLore(item, item.getItemMeta(), giveBase.getType(), giveBase);
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
			if (sender instanceof Player && !((Player) sender).hasPermission("uiframework.reload")) {
				sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.permission_error")));
				return true;
			}
			plugin.reload();
			sender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&aSuccessfully reloaded the config!"));
			return true;
		case "recipes":
			if (!(sender instanceof Player)) {
				sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.console_error_message")));
				return true;
			} else if (!((Player) sender).hasPermission("uiframework.recipes")) {
				sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.permission_error")));
				return true;
			}
			if (args.length > 2) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework recipes [item]"));
				return true;
			}
			if (args.length == 2) {
				ItemType type = ItemType.getItemType(args[1]);
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
				sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.console_error_message")));
				return true;
			} else if (!((Player) sender).hasPermission("uiframework.enchant")) {
				sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.permission_error")));
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
					if (enchant.addEnchant(base, level, true)) {
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
			if (enchant.addEnchant(base, level, true)) {
				base.getType().getBuilder().assembleLore(base.getItem(), base.getItem().getItemMeta(), base.getType(), base);
				((Player) sender).getEquipment().setItemInMainHand(base.getItem(), true);
				sender.sendMessage(UIFUtils.convertString("&aEnchanted your hand item with &7"+enchant.getTrimmedName()+' '+UIFUtils.getNumerical(level)));
				return true;
			}
			sender.sendMessage(UIFUtils.convertString("&cFailed to apply the custom enchant to your hand item!"));
			return true;
		default:
			break;
		}
		if (sender instanceof Player && !((Player) sender).hasPermission("uiframework.*")) {
			sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.permission_error")));
			return true;
		}
		sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework <help|reload|give|recipes|enchant>..."));
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
			list.removeIf(e -> !e.contains(keyword));
			break;
		case 2:
			keyword = args[1].toLowerCase();
			if ((args[0].equalsIgnoreCase("give") && sender.hasPermission("uiframework.give")) || (args[0].equalsIgnoreCase("recipes") && sender.hasPermission("uiframework.recipes"))) {
				list.addAll(ItemType.getAllItems().keySet().stream().filter(e -> !e.equals("_null") && e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			if (args[0].equalsIgnoreCase("enchant") && sender.hasPermission("uiframework.enchant")) {
				list.addAll(UIEnchantment.getEnchantsMap().keySet().stream().filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			break;
		case 3:
			keyword = args[2].toLowerCase();
			if (args[0].equalsIgnoreCase("give") && sender.hasPermission("uiframework.give")) {
				list.addAll(Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName()).filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
			break;
		case 4:
			keyword = args[3].toLowerCase();
			if (args[0].equalsIgnoreCase("enchant") && sender.hasPermission("uiframework.enchant")) {
				list.addAll(Arrays.asList("true", "false").stream().filter(e -> e.contains(keyword)).collect(Collectors.toList()));
				return list;
			}
		}
		return list;
	}
}
