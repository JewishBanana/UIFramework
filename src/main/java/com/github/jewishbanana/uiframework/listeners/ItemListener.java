package com.github.jewishbanana.uiframework.listeners;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilSlot;

@SuppressWarnings("deprecation")
public class ItemListener implements Listener {
	
	private static NamespacedKey metaMatchFix;
	static {
		metaMatchFix = new NamespacedKey(UIFramework.getInstance(), "uif-cro");
	}
	
	private NamespacedKey durabilityKey;
	private Set<AnvilInventory> anvils = new HashSet<>();
	
	private static boolean coloredNames;
	private static boolean refreshOnOpen;

	public ItemListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		this.durabilityKey = new NamespacedKey(plugin, "uif-d");
	}
	@EventHandler(priority = EventPriority.NORMAL)
	public void onItemClick(InventoryClickEvent e) {
		if (e.isCancelled())
			return;
		if (e.getWhoClicked().getItemOnCursor().getType() == Material.AIR && e.getClickedInventory() != null && anvils.remove(e.getClickedInventory()) && e.getRawSlot() == 2) {
			AnvilInventory inv = (AnvilInventory) e.getClickedInventory();
			ItemStack result = inv.getItem(2);
			if (result == null || result.getType() == Material.AIR)
				return;
			GenericItem base = GenericItem.getItemBase(result);
			if (base != null)
				base.getType().getBuilder().assembleLore(result, result.getItemMeta(), base.getType(), base);
			ItemMeta meta = result.getItemMeta();
			if (meta.getPersistentDataContainer().has(metaMatchFix, PersistentDataType.BYTE))
				meta.getPersistentDataContainer().remove(metaMatchFix);
			result.setItemMeta(meta);
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
		if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
			GenericItem item = GenericItem.getItemBase(e.getCurrentItem());
			if (item != null)
				item.getType().getBuilder().assembleLore(e.getCurrentItem(), e.getCurrentItem().getItemMeta(), item.getType(), item);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryOpen(InventoryOpenEvent e) {
		if (!refreshOnOpen)
			return;
		for (ItemStack item : e.getInventory().getContents())
			if (item != null && item.getType() != Material.AIR) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base != null)
					base.getType().getBuilder().assembleLore(item, item.getItemMeta(), base.getType(), base);
			}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryClose(InventoryCloseEvent e) {
		anvils.remove(e.getInventory());
	}
	@EventHandler(priority = EventPriority.LOW)
	public void onItemDamage(PlayerItemDamageEvent e) {
		if (e.isCancelled())
			return;
		ItemStack item = e.getItem();
		GenericItem base = GenericItem.getItemBase(item);
		if (base != null && !base.getType().getRegisteredName().equals("_null")) {
			e.setCancelled(true);
			if (base.getType().getDurability() < 0)
				return;
			ItemMeta itemMeta = item.getItemMeta();
			double damage = ((((double) item.getType().getMaxDurability()) / (base.getType().getDurability() == 0.0 ? item.getType().getMaxDurability() : base.getType().getDurability())) * ((double) e.getDamage())) + (itemMeta.getPersistentDataContainer().has(durabilityKey, PersistentDataType.DOUBLE) ? itemMeta.getPersistentDataContainer().get(durabilityKey, PersistentDataType.DOUBLE) : 0.0);
			int realDamage = 0;
			if (damage >= 1.0) {
				while (damage >= 1.0) {
					realDamage++;
					damage -= 1.0;
				}
				Damageable meta = (Damageable) itemMeta;
				if (meta.getDamage()+realDamage >= item.getType().getMaxDurability()-1) {
					e.setCancelled(false);
					return;
				}
				meta.setDamage(meta.getDamage()+realDamage);
			}
			if (damage > 0)
				itemMeta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.DOUBLE, damage);
			item.setItemMeta(itemMeta);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemMend(PlayerItemMendEvent e) {
		if (e.isCancelled())
			return;
		ItemStack item = e.getItem();
		GenericItem base = GenericItem.getItemBase(item);
		if (base != null && !base.getType().getRegisteredName().equals("_null")) {
			e.setCancelled(true);
			ItemMeta itemMeta = item.getItemMeta();
			double damage = (itemMeta.getPersistentDataContainer().has(durabilityKey, PersistentDataType.DOUBLE) ? itemMeta.getPersistentDataContainer().get(durabilityKey, PersistentDataType.DOUBLE) : 0.0) - ((((double) item.getType().getMaxDurability()) / (base.getType().getDurability() == 0.0 ? item.getType().getMaxDurability() : base.getType().getDurability())) * ((double) e.getRepairAmount()));
			int realDamage = 0;
			if (damage < 0.0) {
				while (damage < 0.0) {
					realDamage++;
					damage += 1.0;
				}
				Damageable meta = (Damageable) itemMeta;
				if (meta.getDamage()-realDamage < 0) {
					meta.setDamage(0);
					if (itemMeta.getPersistentDataContainer().has(durabilityKey, PersistentDataType.DOUBLE))
						itemMeta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.DOUBLE, 0.0);
					item.setItemMeta(meta);
					return;
				}
				meta.setDamage(meta.getDamage()-realDamage);
				item.setItemMeta(meta);
			}
			if (itemMeta.getPersistentDataContainer().has(durabilityKey, PersistentDataType.DOUBLE))
				itemMeta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.DOUBLE, damage);
		}
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemBreak(PlayerItemBreakEvent e) {
		GenericItem.removeBaseItem(e.getBrokenItem());
	}
	@EventHandler
	public void onItemCraft(PrepareItemCraftEvent e) {
		if (e.getRecipe() != null) {
			for (ItemStack item : e.getInventory().getMatrix()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base == null || base.getType().isAllowVanillaCrafts() || base.getType().getRecipes().contains(e.getRecipe()))
					continue;
				e.getInventory().setResult(new ItemStack(Material.AIR));
				return;
			}
		} else {
			boolean flag = false;
			ItemStack[] matrix = e.getInventory().getMatrix().clone();
			for (int i=0; i < matrix.length; i++)
				if (matrix[i] != null) {
					GenericItem base = GenericItem.getItemBaseNoID(matrix[i].clone());
					if (base == null)
						continue;
					flag = true;
					ItemStack tempItem = base.getItem();
					ItemMeta tempMeta = tempItem.getItemMeta();
					base.stripTags(tempMeta);
					base.getFields().clear();
					base.getEnchants().clear();
					base.getType().getBuilder().assembleLore(tempItem, tempMeta, base.getType(), base);
					matrix[i] = tempItem;
				}
			if (flag) {
				Recipe recipe = Bukkit.getServer().getCraftingRecipe(matrix, e.getView().getPlayer() != null ? e.getView().getPlayer().getWorld() : Bukkit.getWorlds().get(0));
				if (recipe != null && recipe.getResult() != null)
					e.getInventory().setResult(recipe.getResult());
			}
		}
	}
	@EventHandler
	public void onSmithingCraft(PrepareSmithingEvent e) {
		if (e.getInventory().getRecipe() != null) {
			for (ItemStack item : e.getInventory().getContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base == null || base.getType().isAllowVanillaCrafts() || base.getType().getRecipes().contains(e.getInventory().getRecipe()))
					continue;
				e.getInventory().setResult(new ItemStack(Material.AIR));
				return;
			}
		}
	}
	@EventHandler
	public void onSmeltFinish(FurnaceSmeltEvent e) {
		GenericItem base = GenericItem.getItemBase(e.getSource());
		if (base != null && !base.getType().isAllowVanillaCrafts()) {
			e.setCancelled(true);
			return;
		}
	}
	@EventHandler
	public void onBrewFuel(BrewingStandFuelEvent e) {
		GenericItem base = GenericItem.getItemBase(e.getFuel());
		if (base != null && !base.getType().isAllowVanillaCrafts()) {
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
			if (base != null && !base.getType().isAllowVanillaCrafts())
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
		GenericItem materialBase = GenericItem.getItemBase(material);
		GenericItem ingredientBase = GenericItem.getItemBase(ingredient);
		if (materialBase != null) {
			for (AnvilRecipe recipe : materialBase.getType().getAnvilRecipes()) {
				if (recipe.getSlot() != AnvilSlot.FIRST)
					continue;
				if (recipe.isExactIngredients()) {
					for (ItemStack tempItem : recipe.getIngredients())
						if (UIFUtils.isItemSimilar(ingredient, tempItem, true)) {
							ItemStack result = null;
							if (recipe.getFunction() != null)
								result = recipe.getFunction().apply(e);
							else
								result = recipe.getResult();
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
							ItemStack result = null;
							if (recipe.getFunction() != null)
								result = recipe.getFunction().apply(e);
							else
								result = recipe.getResult();
							if (!inv.getRenameText().equals(ChatColor.stripColor(material.hasItemMeta() ? material.getItemMeta().getDisplayName() : "")) && result.hasItemMeta()) {
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
				}
			}
			if (!materialBase.getType().isAllowVanillaCrafts())
				e.setResult(new ItemStack(Material.AIR));
		}
		if (ingredientBase != null) {
			for (AnvilRecipe recipe : ingredientBase.getType().getAnvilRecipes()) {
				if (recipe.getSlot() != AnvilSlot.SECOND)
					continue;
				if (recipe.isExactIngredients()) {
					for (ItemStack tempItem : recipe.getIngredients())
						if (UIFUtils.isItemSimilar(material, tempItem, true)) {
							ItemStack result = null;
							if (recipe.getFunction() != null)
								result = recipe.getFunction().apply(e);
							else
								result = recipe.getResult();
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
						if (tempItem.getType() == material.getType()) {
							ItemStack result = null;
							if (recipe.getFunction() != null) {
								result = recipe.getFunction().apply(e);
							} else
								result = recipe.getResult();
							if (!inv.getRenameText().equals(ChatColor.stripColor(material.hasItemMeta() ? material.getItemMeta().getDisplayName() : "")) && result.hasItemMeta()) {
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
				}
			}
			if (!ingredientBase.getType().isAllowVanillaCrafts())
				e.setResult(new ItemStack(Material.AIR));
		}
	}
	public static void reload() {
		coloredNames = UIFDataUtils.getConfigBoolean("general.allow_rename_colors");
		refreshOnOpen = UIFDataUtils.getConfigBoolean("general.refresh_on_open");
	}
	public static void attachRecipeMetaFix(ItemMeta meta) {
		meta.getPersistentDataContainer().set(metaMatchFix, PersistentDataType.BYTE, (byte) 0);
	}
}