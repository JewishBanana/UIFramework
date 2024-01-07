package com.jewishbanana.uiframework.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.GenericItem;
import com.jewishbanana.uiframework.utils.AnvilRecipe;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.UIFUtils;

import net.md_5.bungee.api.ChatColor;

public class ItemListener implements Listener {
	
	private Map<ItemStack, Double> durabilityMap = new HashMap<>();
	private Set<AnvilInventory> anvils = new HashSet<>();
	private static boolean coloredNames;

	public ItemListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(InventoryClickEvent e) {
		if (e.isCancelled())
			return;
		if (e.getWhoClicked().getItemOnCursor().getType() == Material.AIR && e.getClickedInventory() != null && anvils.remove(e.getClickedInventory()) && e.getRawSlot() == 2) {
			AnvilInventory inv = (AnvilInventory) e.getClickedInventory();
			ItemStack result = inv.getItem(2);
			GenericItem base = GenericItem.getItemBase(result);
			if (base != null)
				base.getId().getBuilder().assembleLore(result, result.getItemMeta(), base.getId(), base);
			e.getWhoClicked().setItemOnCursor(result);
			for (ItemStack item : inv.getContents())
				item.setAmount(0);
			inv.setItem(2, new ItemStack(Material.AIR));
			Block block = inv.getLocation().getBlock();
			block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
			if (UIFUtils.getRandom().nextInt(3) == 0 && !UIFUtils.isPlayerImmune((Player) e.getWhoClicked())) {
				String data = block.getBlockData().getAsString();
				switch (block.getType()) {
				case ANVIL:
					block.setBlockData(Bukkit.createBlockData(data.replace("anvil", "chipped_anvil")));
					break;
				case CHIPPED_ANVIL:
					block.setBlockData(Bukkit.createBlockData(data.replace("chipped_anvil", "damaged_anvil")));
					break;
				case DAMAGED_ANVIL:
					block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 1, 1);
					block.breakNaturally(new ItemStack(Material.AIR));
					break;
				default:
					break;
				}
			}
			return;
		}
		if (e.getCurrentItem() != null) {
			GenericItem item = GenericItem.getItemBase(e.getCursor());
			if (item != null)
				item.getId().getBuilder().assembleLore(e.getCursor(), e.getCursor().getItemMeta(), item.getId(), item);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryClose(InventoryCloseEvent e) {
		anvils.remove(e.getInventory());
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemDamage(PlayerItemDamageEvent e) {
		if (e.isCancelled())
			return;
		GenericItem item = GenericItem.getItemBase(e.getItem());
		if (item != null) {
			e.setCancelled(true);
			double damage = ((((double) e.getItem().getType().getMaxDurability()) / item.getId().getDurability()) * ((double) e.getDamage())) + (durabilityMap.containsKey(e.getItem()) ? durabilityMap.get(e.getItem()) : 0.0);
			int realDamage = 0;
			if (damage >= 1.0) {
				while (damage >= 1.0) {
					realDamage++;
					damage -= 1.0;
				}
				Damageable meta = (Damageable) e.getItem().getItemMeta();
				if (meta.getDamage()+realDamage > e.getItem().getType().getMaxDurability()) {
					e.setCancelled(false);
					return;
				}
				meta.setDamage(meta.getDamage()+realDamage);
				e.getItem().setItemMeta((ItemMeta) meta);
				item.setItem(e.getItem());
			}
			durabilityMap.put(e.getItem(), damage);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemMend(PlayerItemMendEvent e) {
		if (e.isCancelled())
			return;
		GenericItem item = GenericItem.getItemBase(e.getItem());
		if (item != null) {
			e.setCancelled(true);
			double damage = (durabilityMap.containsKey(e.getItem()) ? durabilityMap.get(e.getItem()) : 0.0) - ((((double) e.getItem().getType().getMaxDurability()) / item.getId().getDurability()) * ((double) e.getRepairAmount()));
			int realDamage = 0;
			if (damage < 0.0) {
				while (damage < 0.0) {
					realDamage++;
					damage += 1.0;
				}
				Damageable meta = (Damageable) e.getItem().getItemMeta();
				if (meta.getDamage()-realDamage < 0) {
					meta.setDamage(0);
					e.getItem().setItemMeta((ItemMeta) meta);
					item.setItem(e.getItem());
					durabilityMap.put(e.getItem(), 0.0);
					return;
				}
				meta.setDamage(meta.getDamage()-realDamage);
				e.getItem().setItemMeta((ItemMeta) meta);
				item.setItem(e.getItem());
			}
			durabilityMap.put(e.getItem(), damage);
		}
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemBreak(PlayerItemBreakEvent e) {
		GenericItem.removeBaseItem(e.getBrokenItem());
		durabilityMap.remove(e.getBrokenItem());
	}
	@EventHandler
	public void onItemCraft(PrepareItemCraftEvent e) {
		if (e.getRecipe() != null) {
			for (ItemStack item : e.getInventory().getMatrix()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base == null || base.getId().isAllowVanillaCrafts() || base.getId().recipes.contains(e.getRecipe()))
					continue;
				e.getInventory().setResult(new ItemStack(Material.AIR));
				return;
			}
		}
	}
	@EventHandler
	public void onSmithingCraft(PrepareSmithingEvent e) {
		if (e.getInventory().getRecipe() != null) {
			for (ItemStack item : e.getInventory().getContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base == null || base.getId().isAllowVanillaCrafts() || base.getId().recipes.contains(e.getInventory().getRecipe()))
					continue;
				e.getInventory().setResult(new ItemStack(Material.AIR));
				return;
			}
		}
	}
	@EventHandler
	public void onSmeltFinish(FurnaceSmeltEvent e) {
		GenericItem base = GenericItem.getItemBase(e.getSource());
		if (base != null && !base.getId().isAllowVanillaCrafts()) {
			e.setCancelled(true);
			return;
		}
	}
	@EventHandler
	public void onBrewFuel(BrewingStandFuelEvent e) {
		GenericItem base = GenericItem.getItemBase(e.getFuel());
		if (base != null && !base.getId().isAllowVanillaCrafts()) {
			e.setCancelled(true);
			return;
		}
	}
	@EventHandler
	public void onBrew(BrewEvent e) {
		if (e.isCancelled())
			return;
		ItemStack[] potions = {e.getContents().getStorageContents()[0], e.getContents().getStorageContents()[1], e.getContents().getStorageContents()[2]};
		for (int i=0; i < 3; i++) {
			GenericItem base = GenericItem.getItemBase(potions[i]);
			if (base != null && !base.getId().isAllowVanillaCrafts())
				e.getResults().set(i, base.getItem());
		}
	}
	@EventHandler
	public void onAnvilCraft(PrepareAnvilEvent e) {
		AnvilInventory inv = e.getInventory();
		if (inv.getItem(0) == null || inv.getItem(1) == null)
			return;
		ItemStack material = inv.getItem(0);
		ItemStack ingredient = inv.getItem(1);
		GenericItem base = GenericItem.getItemBase(material);
		if (base != null) {
			boolean customRecipe = false;
			loop:
			for (AnvilRecipe recipe : base.getId().anvilRecipes) {
				if (recipe.isExactIngredients()) {
					for (ItemStack tempItem : recipe.getIngredients())
						if (UIFUtils.isItemSimilar(ingredient, tempItem, true)) {
							ItemStack result = recipe.getResult();
							if (recipe.getFunction() != null)
								result = recipe.getFunction().apply(e);
							if (!inv.getRenameText().equals(ChatColor.stripColor(material.hasItemMeta() ? material.getItemMeta().getDisplayName() : ""))) {
								ItemMeta meta = result.getItemMeta();
								if (coloredNames)
									meta.setDisplayName(UIFUtils.convertString(inv.getRenameText()));
								else
									meta.setDisplayName(inv.getRenameText());
								result.setItemMeta(meta);
							}
							e.setResult(result);
							anvils.add(inv);
							return;
						}
				} else {
					for (ItemStack tempItem : recipe.getIngredients())
						if (tempItem.getType() == ingredient.getType()) {
							ItemStack result = recipe.getResult();
							if (recipe.getFunction() != null)
								result = recipe.getFunction().apply(e);
							if (!inv.getRenameText().equals(ChatColor.stripColor(material.hasItemMeta() ? material.getItemMeta().getDisplayName() : ""))) {
								ItemMeta meta = result.getItemMeta();
								if (coloredNames)
									meta.setDisplayName(UIFUtils.convertString(inv.getRenameText()));
								else
									meta.setDisplayName(inv.getRenameText());
								result.setItemMeta(meta);
							}
							e.setResult(result);
							customRecipe = true;
							anvils.add(inv);
							break loop;
						}
				}
			}
			if (!customRecipe && !base.getId().isAllowVanillaCrafts()) {
				e.setResult(new ItemStack(Material.AIR));
				return;
			}
		}
		base = GenericItem.getItemBase(ingredient);
		if (base != null && !base.getId().isAllowVanillaCrafts()) {
			e.setResult(new ItemStack(Material.AIR));
			anvils.remove(inv);
			return;
		}
	}
	public static void reload() {
		coloredNames = UIFDataUtils.getConfigBoolean("general.allow_rename_colors");
	}
}