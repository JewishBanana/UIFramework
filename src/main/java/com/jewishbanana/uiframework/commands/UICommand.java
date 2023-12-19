package com.jewishbanana.uiframework.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.ItemType;
import com.jewishbanana.uiframework.listeners.menus.ItemsMenu;
import com.jewishbanana.uiframework.listeners.menus.MenuManager;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class UICommand implements CommandExecutor,TabCompleter {
	
	private UIFramework plugin;
	
	public UICommand(UIFramework plugin) {
		this.plugin = plugin;
		
		plugin.getCommand("uiframework").setExecutor(this);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		switch (args[0].toLowerCase()) {
		case "give":
			if (!(sender instanceof Player)) {
				if (args.length != 3) {
					sender.sendMessage("Usage: /uiframework give <item> <player>");
					return true;
				}
			} else if (!((Player) sender).hasPermission("ultimateitems.give")) {
				sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.permission_error")));
				return true;
			}
			if (args.length < 2 || args.length > 3) {
				sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework give <item> [player]"));
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
			int slot = target.getInventory().firstEmpty();
			if (slot == -1) {
				sender.sendMessage(UIFUtils.convertString("&cPlayer's inventory is full!"));
				return true;
			}
			ItemStack item = id.getBuilder().getItem();
			target.getInventory().addItem(item);
			sender.sendMessage(UIFUtils.convertString("&aGave ["+item.getItemMeta().getDisplayName()+"&a] to "+target.getName()));
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
			ItemsMenu menu = new ItemsMenu(1);
			MenuManager.registerInventory(menu.getInventory(), menu);
			((Player) sender).openInventory(menu.getInventory());
			return true;
			
			default:
				break;
		}
		if (sender instanceof Player && !((Player) sender).hasPermission("uiframework.*")) {
			sender.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.permission_error")));
			return true;
		}
		sender.sendMessage(UIFUtils.convertString("&cUsage: /uiframework <help|reload|give|recipes>..."));
		return true;
	}
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		final List<String> list = new ArrayList<>();
		switch (args.length) {
		default:
		case 0:
			return list;
		case 1:
			final List<String> tempList = new ArrayList<>();
			if (sender.hasPermission("uiframework.give"))
				tempList.add("give");
			if (sender.hasPermission("uiframework.reload"))
				tempList.add("reload");
			if (sender.hasPermission("uiframework.recipes"))
				tempList.add("recipes");
			StringUtil.copyPartialMatches(args[0], tempList, list);
			Collections.sort(list);
			break;
		case 2:
			if (args[0].equalsIgnoreCase("give") && sender.hasPermission("uiframework.give")) {
				StringUtil.copyPartialMatches(args[1], ItemType.getItemsMap().keySet().stream().map(e -> e.toLowerCase()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
			break;
		case 3:
			if (args[0].equalsIgnoreCase("give") && sender.hasPermission("uiframework.give")) {
				StringUtil.copyPartialMatches(args[2], Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList()), list);
				Collections.sort(list);
				return list;
			}
			break;
		}
		return list;
	}
}
