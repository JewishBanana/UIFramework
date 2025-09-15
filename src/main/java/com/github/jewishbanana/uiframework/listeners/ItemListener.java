package com.github.jewishbanana.uiframework.listeners;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
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
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.UIEnchantment;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilChoice.SlotOrder;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilResult;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.UIAnvilInventory;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class ItemListener implements Listener {
	
	private static final NamespacedKey metaMatchFix;
	static {
		metaMatchFix = new NamespacedKey(UIFramework.getInstance(), "uif-cro");
	}
	
	private UIFramework plugin;
	
	public Queue<AnvilRecipe> anvilRecipes = new ArrayDeque<>();
	private NamespacedKey durabilityKey;
	private Map<AnvilInventory, Integer> anvils = new HashMap<>();
	
	private static boolean coloredNames;
	private static boolean refreshOnOpen;

	public ItemListener(UIFramework plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		this.durabilityKey = new NamespacedKey(plugin, "uif-d");
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onItemClick(InventoryClickEvent event) {
		if (event.getWhoClicked().getItemOnCursor().getType() == Material.AIR && event.getClickedInventory() != null && anvils.containsKey(event.getClickedInventory()) && event.getRawSlot() == 2) {
			AnvilInventory inv = (AnvilInventory) event.getClickedInventory();
			Player player = (Player) event.getWhoClicked();
			int cost = anvils.get(inv);
			ItemStack result = inv.getItem(2);
			if (result == null || result.getType() == Material.AIR || player.getLevel() < cost)
				return;
			GenericItem base = GenericItem.getItemBase(result);
			if (base != null)
				base.getType().getBuilder().assembleLore(result, result.getItemMeta(), base.getType(), base);
			ItemMeta meta = result.getItemMeta();
			if (meta.getPersistentDataContainer().has(metaMatchFix, PersistentDataType.BYTE))
				meta.getPersistentDataContainer().remove(metaMatchFix);
			result.setItemMeta(meta);
			player.setItemOnCursor(result);
			for (ItemStack item : inv.getContents())
				item.setAmount(0);
			inv.setItem(2, new ItemStack(Material.AIR));
			Block block = inv.getLocation().getBlock();
			block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
			if (UIFUtils.getRandom().nextInt(3) == 0 && !UIFUtils.isPlayerImmune(player)) {
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
			player.setLevel(player.getLevel()-cost);
			anvils.remove(inv);
			return;
		}
		if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
			GenericItem item = GenericItem.getItemBase(event.getCurrentItem());
			if (item != null)
				item.refreshItemLore();
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!refreshOnOpen)
			return;
		for (ItemStack item : event.getInventory().getContents())
			if (item != null && item.getType() != Material.AIR) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base != null)
					base.refreshItemLore();
			}
	}
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent event) {
		anvils.remove(event.getInventory());
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onItemDamage(PlayerItemDamageEvent event) {
		ItemStack item = event.getItem();
		GenericItem base = GenericItem.getItemBase(item);
		if (base != null && !base.getType().getRegisteredName().equals("_null")) {
			event.setCancelled(true);
			if (base.getType().getDurability() < 0)
				return;
			ItemMeta itemMeta = item.getItemMeta();
			double damage = ((((double) item.getType().getMaxDurability()) / (base.getType().getDurability() == 0.0 ? item.getType().getMaxDurability() : base.getType().getDurability())) * ((double) event.getDamage())) + (itemMeta.getPersistentDataContainer().has(durabilityKey, PersistentDataType.DOUBLE) ? itemMeta.getPersistentDataContainer().get(durabilityKey, PersistentDataType.DOUBLE) : 0.0);
			int realDamage = 0;
			if (damage >= 1.0) {
				while (damage >= 1.0) {
					realDamage++;
					damage -= 1.0;
				}
				Damageable meta = (Damageable) itemMeta;
				if (meta.getDamage()+realDamage >= item.getType().getMaxDurability()-1) {
					event.setCancelled(false);
					return;
				}
				meta.setDamage(meta.getDamage()+realDamage);
			}
			if (damage > 0)
				itemMeta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.DOUBLE, damage);
			item.setItemMeta(itemMeta);
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onItemMend(PlayerItemMendEvent event) {
		ItemStack item = event.getItem();
		GenericItem base = GenericItem.getItemBase(item);
		if (base != null && !base.getType().getRegisteredName().equals("_null")) {
			event.setCancelled(true);
			ItemMeta itemMeta = item.getItemMeta();
			double damage = (itemMeta.getPersistentDataContainer().has(durabilityKey, PersistentDataType.DOUBLE) ? itemMeta.getPersistentDataContainer().get(durabilityKey, PersistentDataType.DOUBLE) : 0.0) - ((((double) item.getType().getMaxDurability()) / (base.getType().getDurability() == 0.0 ? item.getType().getMaxDurability() : base.getType().getDurability())) * ((double) event.getRepairAmount()));
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
	public void onItemBreak(PlayerItemBreakEvent event) {
		GenericItem.removeBaseItem(event.getBrokenItem());
	}
	@EventHandler
	public void onItemCraft(PrepareItemCraftEvent event) {
		if (event.getRecipe() != null) {
			for (ItemStack item : event.getInventory().getMatrix()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base == null || base.getType().isAllowVanillaCrafts() || base.getType().getRecipes().contains(event.getRecipe()))
					continue;
				event.getInventory().setResult(new ItemStack(Material.AIR));
				return;
			}
		} else {
			boolean flag = false;
			ItemStack[] matrix = event.getInventory().getMatrix().clone();
			for (int i=0; i < matrix.length; i++)
				if (matrix[i] != null) {
					GenericItem base = GenericItem.getItemBaseNoID(matrix[i].clone());
					if (base == null)
						continue;
					flag = true;
					matrix[i] = UIFUtils.stripItemTags(base);
				}
			if (flag) {
				World world = Bukkit.getWorlds().get(0);
				if (event.getInventory().getHolder() != null)
					world = event.getInventory().getHolder() instanceof Entity entity ? entity.getWorld() : event.getInventory().getHolder() instanceof Block block ? block.getWorld() : world;
				Recipe recipe = Bukkit.getServer().getCraftingRecipe(matrix, world);
				if (recipe != null)
					event.getInventory().setResult(recipe.getResult());
//				writeTestToFile("table", matrix);
//				writeTestToFile("recipe", ((ShapelessRecipe) UIItemType.getItemType("uc:snow_globe").getRecipes().get(0)).getChoiceList().stream().map(e -> e.getItemStack()).collect(Collectors.toList()).toArray(new ItemStack[0]));
			}
		}
	}
	public static void writeTestToFile(String path, ItemStack[] items) {
		File file = new File(UIFramework.getInstance().getDataFolder().getAbsolutePath(), "test.yml");
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
		ConfigurationSection section = cfg.createSection(path);
		for (int i=0; i < items.length; i++)
			if (items[i] != null)
				section.set("item_"+i, items[i]);
		try {
			cfg.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@EventHandler
	public void onSmithingCraft(PrepareSmithingEvent event) {
		if (event.getInventory().getRecipe() != null) {
			for (ItemStack item : event.getInventory().getContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base == null || base.getType().isAllowVanillaCrafts() || base.getType().getRecipes().contains(event.getInventory().getRecipe()))
					continue;
				event.getInventory().setResult(new ItemStack(Material.AIR));
				return;
			}
		}
	}
	@EventHandler
	public void onSmeltFinish(FurnaceSmeltEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getSource());
		if (base != null && !base.getType().isAllowVanillaCrafts()) {
			event.setCancelled(true);
			return;
		}
	}
	@EventHandler
	public void onBrewFuel(BrewingStandFuelEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getFuel());
		if (base != null && !base.getType().isAllowVanillaCrafts()) {
			event.setCancelled(true);
			return;
		}
	}
	@EventHandler
	public void onBrew(BrewEvent event) {
		if (event.isCancelled())
			return;
		ItemStack[] potions = {event.getContents().getStorageContents()[0], event.getContents().getStorageContents()[1], event.getContents().getStorageContents()[2]};
		for (int i=0; i < 3; i++) {
			GenericItem base = GenericItem.getItemBase(potions[i]);
			if (base != null && !base.getType().isAllowVanillaCrafts())
				event.getResults().set(i, base.getItem());
		}
	}
	@EventHandler
	public void onEnchant(PrepareItemEnchantEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getItem());
		if (base == null)
			return;
		if (!base.getType().isAllowVanillaEnchanting())
			for (int i=0; i < event.getOffers().length; i++)
				event.getOffers()[i] = null;
	}
	@SuppressWarnings("removal")
	@EventHandler
	public void onAnvilCraft(PrepareAnvilEvent event) {
		AnvilInventory inv = event.getInventory();
		ItemStack material = inv.getItem(0);
		ItemStack ingredient = inv.getItem(1);
		if (material == null || ingredient == null)
			return;
		for (AnvilRecipe recipe : anvilRecipes) {
			AnvilChoice anvilChoice = recipe.getAnvilChoice();
			if ((isRecipeChoiceValid(anvilChoice.getFirstSlot(), material) && isRecipeChoiceValid(anvilChoice.getSecondSlot(), ingredient))
					|| (anvilChoice.getSlotOrder() == SlotOrder.MIXED && isRecipeChoiceValid(anvilChoice.getFirstSlot(), ingredient) && isRecipeChoiceValid(anvilChoice.getSecondSlot(), material))) {
				AnvilResult result = recipe.getAnvilResult(new UIAnvilInventory(event.getInventory()));
				ItemStack item = result.result;
				if (item == null || item.getType() == Material.AIR)
					continue;
				final boolean renameLevel = !inv.getRenameText().equals(ChatColor.stripColor(material.hasItemMeta() ? material.getItemMeta().getDisplayName() : ""));
				if (renameLevel) {
					ItemMeta meta = item.getItemMeta();
					if (coloredNames)
						meta.setDisplayName(UIFUtils.convertString(inv.getRenameText()));
					else
						meta.setDisplayName(inv.getRenameText());
					item.setItemMeta(meta);
				}
				event.setResult(item);
				final int finalCost = result.levelCost + (renameLevel ? 1 : 0);
				anvils.put(inv, finalCost);
				plugin.getServer().getScheduler().runTask(plugin, () -> inv.setRepairCost(finalCost));
				return;
			}
		}
		GenericItem materialBase = GenericItem.getItemBase(material);
		GenericItem ingredientBase = GenericItem.getItemBase(ingredient);
		if (materialBase == null && ingredientBase == null)
			return;
		if (material.getType() == ingredient.getType() && material.getType() != Material.ENCHANTED_BOOK && (materialBase == null || ingredientBase == null || !materialBase.getType().getInstance().equals(ingredientBase.getType().getInstance()))) {
			event.setResult(new ItemStack(Material.AIR));
			return;
		}
		ItemStack result = event.getResult();
		if (ingredient.getType() == Material.ENCHANTED_BOOK) {
			if (result == null) {
				if (material.getType() == Material.ENCHANTED_BOOK && material.getEnchantments().size() == 0)
					result = materialBase == null ? material.clone() : ingredient.clone();
				else if (ingredientBase != null && !ingredientBase.getEnchants().keySet().stream().anyMatch(e -> !e.canBeEnchanted(material)))
					result = material.clone();
			}
			if (materialBase != null && !materialBase.getType().isAllowVanillaEnchanting()) {
				event.setResult(new ItemStack(Material.AIR));
				return;
			}
		}
		if (result == null)
			return;
		GenericItem.stripUniqueId(result);
		GenericItem base = GenericItem.createItemBase(result);
		int cost = inv.getRepairCost();
		final boolean addRepairsFlag = cost == -1;
		if (addRepairsFlag)
			cost = 0;
		if (materialBase != null && ingredientBase != null)
			for (Entry<UIEnchantment, Integer> entry : ingredientBase.getEnchants().entrySet()) {
				UIEnchantment enchant = entry.getKey();
				int ingredientLevel = entry.getValue();
				int materialLevel = enchant.getEnchantLevel(material);
				int finalLevel = (materialLevel == ingredientLevel && materialLevel < enchant.getMaxLevel()) ? materialLevel+1 : Math.max(materialLevel, ingredientLevel);
				enchant.addEnchant(base, finalLevel, true, true);
				cost += enchant.getAnvilCost(material, ingredient, finalLevel);
			}
		else {
			Set<Entry<UIEnchantment, Integer>> enchants = materialBase == null ? ingredientBase.getEnchants().entrySet() : materialBase.getEnchants().entrySet();
			for (Entry<UIEnchantment, Integer> entry : enchants) {
				UIEnchantment enchant = entry.getKey();
				if (enchant.canBeEnchanted(material)) {
					enchant.addEnchant(base, entry.getValue(), true, true);
					cost += enchant.getAnvilCost(material, ingredient, entry.getValue());
				}
			}
		}
		base.refreshItemLore();
		result = base.getItem();
		ItemMeta meta = result.getItemMeta();
		final boolean renameLevel = addRepairsFlag && !inv.getRenameText().equals(ChatColor.stripColor(material.hasItemMeta() ? material.getItemMeta().getDisplayName() : ""));
		if (renameLevel) {
			if (coloredNames)
				meta.setDisplayName(UIFUtils.convertString(inv.getRenameText()));
			else
				meta.setDisplayName(inv.getRenameText());
		}
		attachRecipeMetaFix(meta);
		result.setItemMeta(meta);
		event.setResult(result);
		if (addRepairsFlag) {
			if (result.hasItemMeta() && result.getItemMeta() instanceof Repairable repairable) {
				int materialCost = material.hasItemMeta() ? (material.getItemMeta() instanceof Repairable temp ? temp.getRepairCost() : 0) : 0;
				int ingredientCost = ingredient.hasItemMeta() ? (ingredient.getItemMeta() instanceof Repairable temp ? temp.getRepairCost() : 0) : 0;
				int highestCost = Math.max(materialCost, ingredientCost);
				repairable.setRepairCost((highestCost * 2) + 1);
				result.setItemMeta(repairable);
				cost += highestCost;
			}
			if (cost + (renameLevel ? 1 : 0) >= 40)
				try {
					Method getView = event.getClass().getMethod("getView");
					getView.setAccessible(true);
					Object view = getView.invoke(event);
					Method getPlayer = view.getClass().getMethod("getPlayer");
					getPlayer.setAccessible(true);
		            HumanEntity viewer = (HumanEntity) getPlayer.invoke(view);
		            if (viewer != null && viewer instanceof Player && !UIFUtils.isPlayerImmune((Player) viewer)) {
						event.setResult(new ItemStack(Material.AIR));
						final int finalCost = cost + (renameLevel ? 1 : 0);
						plugin.getServer().getScheduler().runTask(plugin, () -> inv.setRepairCost(finalCost));
						return;
		            }
				} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
		            throw new RuntimeException(e);
		        }
		}
		final int finalCost = cost + (renameLevel ? 1 : 0);
		if (inv.getRepairCost() != finalCost) {
			plugin.getServer().getScheduler().runTask(plugin, () -> inv.setRepairCost(finalCost));
		}
		anvils.put(inv, finalCost);
	}
	private boolean isRecipeChoiceValid(RecipeChoice choice, ItemStack sample) {
		if (choice instanceof RecipeChoice.ExactChoice) {
			for (ItemStack item : ((RecipeChoice.ExactChoice) choice).getChoices())
				if (UIFUtils.isItemSimilar(sample, item, true))
					return true;
			return false;
		}
		return choice.test(sample);
	}
	public static void reload() {
		coloredNames = UIFDataUtils.getConfigBoolean("general.allow_rename_colors");
		refreshOnOpen = UIFDataUtils.getConfigBoolean("general.refresh_on_open");
	}
	public static void attachRecipeMetaFix(ItemMeta meta) {
		meta.getPersistentDataContainer().set(metaMatchFix, PersistentDataType.BYTE, (byte) 0);
	}
}