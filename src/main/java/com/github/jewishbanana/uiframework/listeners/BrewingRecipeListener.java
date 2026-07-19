package com.github.jewishbanana.uiframework.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.utils.BrewingRecipe;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class BrewingRecipeListener implements Listener {

	private final UIFramework plugin;
	private final Map<NamespacedKey, BrewingRecipe> recipes = new LinkedHashMap<>();
	private final Map<Location, BukkitTask> activeBrews = new LinkedHashMap<>();
	private final Map<Material, Integer> forcedIngredientMaterials = new HashMap<>();

	public BrewingRecipeListener(UIFramework plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	public void register(BrewingRecipe recipe) {
		if (recipes.putIfAbsent(recipe.getKey(), recipe) == null)
			updateForcedIngredientMaterials(recipe, 1);
	}
	public void unregister(NamespacedKey key) {
		BrewingRecipe recipe = recipes.remove(key);
		if (recipe != null)
			updateForcedIngredientMaterials(recipe, -1);
	}
	public BrewingRecipe getRecipe(NamespacedKey key) {
		return recipes.get(key);
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onIngredientClick(InventoryClickEvent event) {
		if (!(event.getView().getTopInventory() instanceof BrewerInventory inventory))
			return;
		if (inventory.getHolder() == null)
			return;
		if (event.getRawSlot() == 3) {
			if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
				ItemStack hotbar = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
				if (isCustomIngredient(hotbar) || isCustomIngredient(inventory.getIngredient())) {
					event.setCancelled(true);
					ItemStack current = inventory.getIngredient();
					inventory.setIngredient(hotbar == null ? null : hotbar.clone());
					event.getWhoClicked().getInventory().setItem(event.getHotbarButton(), current == null ? null : current.clone());
				}
			} else if (event.getClick() == ClickType.SWAP_OFFHAND) {
				ItemStack offhand = event.getWhoClicked().getInventory().getItemInOffHand();
				if (isCustomIngredient(offhand) || isCustomIngredient(inventory.getIngredient())) {
					event.setCancelled(true);
					ItemStack current = inventory.getIngredient();
					inventory.setIngredient(offhand == null ? null : offhand.clone());
					event.getWhoClicked().getInventory().setItemInOffHand(current == null ? new ItemStack(Material.AIR) : current.clone());
				}
			} else {
				ItemStack cursor = event.getCursor();
				ItemStack current = inventory.getIngredient();
				if (isCustomIngredient(cursor) || isCustomIngredient(current)) {
					event.setCancelled(true);
					if (cursor == null || cursor.getType() == Material.AIR || isCustomIngredient(cursor))
						swapIngredient(event.getWhoClicked(), inventory, cursor, current, event.getClick() == ClickType.RIGHT);
				}
			}
		} else if (event.isShiftClick() && event.getRawSlot() >= event.getView().getTopInventory().getSize()
				&& isCustomIngredient(event.getCurrentItem())) {
			event.setCancelled(true);
			moveIngredientFromSlot(event, inventory);
		}
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			tryStart(inventory);
			if (event.getWhoClicked() instanceof Player player)
				player.updateInventory();
		});
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onIngredientDrag(InventoryDragEvent event) {
		if (!(event.getView().getTopInventory() instanceof BrewerInventory inventory) || inventory.getHolder() == null
				|| !event.getRawSlots().contains(3))
			return;
		ItemStack ingredient = event.getNewItems().get(3);
		if (!isCustomIngredient(ingredient))
			return;
		event.setCancelled(true);
		if (event.getRawSlots().size() != 1)
			return;
		ItemStack remaining = event.getCursor() == null ? null : event.getCursor().clone();
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			inventory.setIngredient(ingredient.clone());
			event.getWhoClicked().setItemOnCursor(remaining);
			tryStart(inventory);
			if (event.getWhoClicked() instanceof Player player)
				player.updateInventory();
		});
	}
	@EventHandler
	public void onOpen(InventoryOpenEvent event) {
		if (event.getInventory() instanceof BrewerInventory inventory)
			plugin.getServer().getScheduler().runTask(plugin, () -> tryStart(inventory));
	}
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onVanillaBrew(BrewEvent event) {
		BrewerInventory inventory = event.getContents();
		boolean customRecipe = hasMatchingRecipe(inventory);
		if (!customRecipe && !hasCustomInput(inventory))
			return;
		event.setCancelled(true);
		if (customRecipe)
			tryStart(inventory);
	}
	private void tryStart(BrewerInventory inventory) {
		ItemStack ingredient = inventory.getIngredient();
		Location location = getLocation(inventory);
		if (location == null)
			return;
		if (activeBrews.containsKey(location))
			return;
		Map<Integer, BrewingRecipe> matches = findMatches(inventory);
		if (matches.isEmpty())
			return;
		if (!consumeFuel(inventory))
			return;
		ItemStack brewedIngredient = inventory.getIngredient().clone();
		int brewingTime = matches.values().stream().mapToInt(BrewingRecipe::getBrewingTime).max().orElse(BrewingRecipe.DEFAULT_BREWING_TIME);
		finishVisual(inventory, BrewingRecipe.DEFAULT_BREWING_TIME);
		BukkitTask task = new BukkitRunnable() {
			private int remaining = brewingTime;

			@Override
			public void run() {
				if (!sameItem(inventory.getIngredient(), brewedIngredient) || matches.entrySet().stream().noneMatch(entry -> entry.getValue().matches(inventory.getItem(entry.getKey()), inventory.getIngredient()))) {
					finishVisual(inventory, 0);
					activeBrews.remove(location);
					cancel();
					return;
				}
				finishVisual(inventory, getVisualBrewingTime(remaining, brewingTime));
				if (--remaining > 0)
					return;
				finishVisual(inventory, 0);
				boolean brewed = false;
				for (Map.Entry<Integer, BrewingRecipe> entry : matches.entrySet()) {
					if (!entry.getValue().matches(inventory.getItem(entry.getKey()), inventory.getIngredient()))
						continue;
					inventory.setItem(entry.getKey(), entry.getValue().getResult());
					brewed = true;
				}
				if (brewed) {
					consumeIngredient(inventory);
					location.getWorld().playSound(location, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
				}
				activeBrews.remove(location);
				cancel();
				plugin.getServer().getScheduler().runTask(plugin, () -> tryStart(inventory));
			}
		}.runTaskTimer(plugin, 1L, 1L);
		activeBrews.put(location, task);
	}
	private Map<Integer, BrewingRecipe> findMatches(BrewerInventory inventory) {
		Map<Integer, BrewingRecipe> matches = new LinkedHashMap<>();
		ItemStack ingredient = inventory.getIngredient();
		for (int slot = 0; slot < 3; slot++) {
			ItemStack input = inventory.getItem(slot);
			for (BrewingRecipe recipe : recipes.values())
				if (recipe.matches(input, ingredient)) {
					matches.put(slot, recipe);
					break;
				}
		}
		return matches;
	}
	private boolean hasMatchingRecipe(BrewerInventory inventory) {
		return !findMatches(inventory).isEmpty();
	}
	private boolean hasCustomInput(BrewerInventory inventory) {
		for (int slot = 0; slot < 3; slot++)
			if (GenericItem.getItemBaseNoID(inventory.getItem(slot)) != null)
				return true;
		return false;
	}
	private boolean isCustomIngredient(ItemStack item) {
		return item != null && forcedIngredientMaterials.containsKey(item.getType());
	}
	private void updateForcedIngredientMaterials(BrewingRecipe recipe, int change) {
		for (Material material : getIngredientMaterials(recipe)) {
			int references = forcedIngredientMaterials.getOrDefault(material, 0) + change;
			if (references <= 0)
				forcedIngredientMaterials.remove(material);
			else
				forcedIngredientMaterials.put(material, references);
		}
	}
	private static Set<Material> getIngredientMaterials(BrewingRecipe recipe) {
		Set<Material> materials = new HashSet<>();
		if (recipe.getIngredient() instanceof RecipeChoice.ExactChoice exact)
			exact.getChoices().forEach(item -> materials.add(item.getType()));
		else if (recipe.getIngredient() instanceof RecipeChoice.MaterialChoice material)
			materials.addAll(material.getChoices());
		return materials;
	}
	private boolean consumeFuel(BrewerInventory inventory) {
		BrewingStand stand = inventory.getHolder();
		if (stand == null)
			return false;
		int fuelLevel = stand.getFuelLevel();
		boolean consumeFuelItem = false;
		if (fuelLevel <= 0) {
			ItemStack fuel = inventory.getFuel();
			if (fuel == null || fuel.getType() != Material.BLAZE_POWDER)
				return false;
			consumeFuelItem = true;
			fuelLevel = 20;
		}
		stand.setFuelLevel(fuelLevel - 1);
		stand.update(true, false);
		if (consumeFuelItem) {
			ItemStack fuel = inventory.getFuel();
			if (fuel == null || fuel.getAmount() <= 1)
				inventory.setFuel(null);
			else
				fuel.setAmount(fuel.getAmount() - 1);
		}
		return true;
	}
	private void finishVisual(BrewerInventory inventory, int ticks) {
		BrewingStand stand = inventory.getHolder();
		if (stand != null) {
			stand.setBrewingTime(ticks);
			stand.update(true, false);
		}
	}
	private int getVisualBrewingTime(int remaining, int brewingTime) {
		return Math.max(0, Math.min(BrewingRecipe.DEFAULT_BREWING_TIME,
				(int) Math.ceil((double) remaining * BrewingRecipe.DEFAULT_BREWING_TIME / brewingTime)));
	}
	private void swapIngredient(HumanEntity player, BrewerInventory inventory, ItemStack cursor, ItemStack current, boolean single) {
		if (cursor == null || cursor.getType() == Material.AIR) {
			player.setItemOnCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
			inventory.setIngredient(null);
			return;
		}
		if (current != null && current.getType() != Material.AIR && sameItem(current, cursor)) {
			int moved = Math.min(single ? 1 : cursor.getAmount(), current.getMaxStackSize() - current.getAmount());
			current.setAmount(current.getAmount() + moved);
			cursor.setAmount(cursor.getAmount() - moved);
			player.setItemOnCursor(cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
			return;
		}
		ItemStack placed = cursor.clone();
		if (single)
			placed.setAmount(1);
		inventory.setIngredient(placed);
		if (single && cursor.getAmount() > 1) {
			cursor.setAmount(cursor.getAmount() - 1);
			player.setItemOnCursor(cursor);
		} else
			player.setItemOnCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
	}
	private void moveIngredientFromSlot(InventoryClickEvent event, BrewerInventory inventory) {
		ItemStack source = event.getCurrentItem();
		ItemStack current = inventory.getIngredient();
		if (current == null || current.getType() == Material.AIR) {
			inventory.setIngredient(source.clone());
			event.setCurrentItem(null);
			return;
		}
		if (!sameItem(current, source))
			return;
		int moved = Math.min(source.getAmount(), current.getMaxStackSize() - current.getAmount());
		current.setAmount(current.getAmount() + moved);
		source.setAmount(source.getAmount() - moved);
		event.setCurrentItem(source.getAmount() <= 0 ? null : source);
	}
	private static void consumeIngredient(BrewerInventory inventory) {
		ItemStack ingredient = inventory.getIngredient();
		if (ingredient == null)
			return;
		if (ingredient.getAmount() <= 1)
			inventory.setIngredient(null);
		else
			ingredient.setAmount(ingredient.getAmount() - 1);
	}
	private static boolean sameItem(ItemStack first, ItemStack second) {
		return first != null && second != null && UIFUtils.isItemSimilar(first, second, true);
	}
	private static Location getLocation(BrewerInventory inventory) {
		BrewingStand stand = inventory.getHolder();
		if (stand == null)
			return null;
		Block block = stand.getBlock();
		return block.getLocation();
	}
}
