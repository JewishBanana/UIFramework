package com.github.jewishbanana.uiframework.listeners.menus;

import java.util.ArrayDeque;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.utils.BrewingRecipe;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class BrewingRecipePreviewMenu extends InventoryHandler {

	private final RecipeMenu returnMenu;
	private final BrewingRecipe recipe;
	private final String itemDisplayName;
	private Queue<ItemStack> inputChoices;
	private Queue<ItemStack> ingredientChoices;
	private BukkitTask cyclingTask;

	public BrewingRecipePreviewMenu(RecipeMenu returnMenu, BrewingRecipe recipe, String itemDisplayName) {
		this.returnMenu = returnMenu;
		this.recipe = recipe;
		this.itemDisplayName = itemDisplayName;
		this.inventory = createInventory();
		decorate();
	}
	@Override
	public void decorate() {
		inputChoices = getDisplayChoices(recipe.getInput());
		ingredientChoices = getDisplayChoices(recipe.getIngredient());
		ItemStack input = inputChoices.peek();
		for (int slot = 0; slot < 3; slot++)
			inventory.setItem(slot, input.clone());
		inventory.setItem(3, ingredientChoices.peek().clone());
		inventory.setItem(4, new ItemStack(Material.BLAZE_POWDER));
	}
	@Override
	public void onOpen(InventoryOpenEvent event) {
		if (inputChoices.size() < 2 && ingredientChoices.size() < 2)
			return;
		cyclingTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (inputChoices.size() > 1)
					inputChoices.add(inputChoices.remove());
				ItemStack input = inputChoices.peek();
				for (int slot = 0; slot < 3; slot++)
					inventory.setItem(slot, input.clone());
				if (ingredientChoices.size() > 1)
					ingredientChoices.add(ingredientChoices.remove());
				inventory.setItem(3, ingredientChoices.peek().clone());
			}
		}.runTaskTimer(UIFramework.getInstance(), 20L, 20L);
	}
	@Override
	public void onDrag(InventoryDragEvent event) {
		event.setCancelled(true);
	}
	@Override
	public void onClose(InventoryCloseEvent event) {
		if (cyclingTask != null)
			cyclingTask.cancel();
		if (!(event.getPlayer() instanceof Player player))
			return;
		Bukkit.getScheduler().runTask(UIFramework.getInstance(), () -> {
			if (!player.isOnline())
				return;
			MenuManager.registerInventory(returnMenu.getInventory(), returnMenu);
			player.openInventory(returnMenu.getInventory());
		});
	}
	@Override
	public Inventory createInventory() {
		return Bukkit.createInventory(null, InventoryType.BREWING,
				UIFUtils.convertString(itemDisplayName+" &9"+UIFramework.getLangString("menu.brewingPreview")));
	}
	private static Queue<ItemStack> getDisplayChoices(RecipeChoice choice) {
		Queue<ItemStack> choices = new ArrayDeque<>();
		if (choice instanceof RecipeChoice.ExactChoice exact) {
			exact.getChoices().stream().map(BrewingRecipePreviewMenu::getDisplayItem).forEach(choices::add);
			return choices;
		}
		RecipeChoice.MaterialChoice material = (RecipeChoice.MaterialChoice) choice;
		material.getChoices().stream().map(ItemStack::new).forEach(choices::add);
		return choices;
	}
	private static ItemStack getDisplayItem(ItemStack source) {
		ItemStack display = source.clone();
		GenericItem base = GenericItem.getItemBaseNoID(display);
		return base == null ? display : base.getItem();
	}
}
