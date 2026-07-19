package com.github.jewishbanana.uiframework.listeners.menus;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.AnvilResult;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe.UIAnvilInventory;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class AnvilRecipePreviewMenu extends InventoryHandler {

	private final RecipeMenu returnMenu;
	private final AnvilRecipe recipe;
	private final String itemDisplayName;
	private Queue<ItemStack> firstChoices;
	private Queue<ItemStack> secondChoices;
	private BukkitTask cyclingTask;
	private String initialRenameText;
	private boolean suppressReturn;

	public AnvilRecipePreviewMenu(RecipeMenu returnMenu, AnvilRecipe recipe, String itemDisplayName) {
		this.returnMenu = returnMenu;
		this.recipe = recipe;
		this.itemDisplayName = itemDisplayName;
		this.inventory = createInventory();
		decorate();
	}
	@Override
	public void decorate() {
		firstChoices = getDisplayChoices(recipe.getAnvilChoice().getFirstSlot());
		secondChoices = getDisplayChoices(recipe.getAnvilChoice().getSecondSlot());
		inventory.setItem(0, firstChoices.peek().clone());
		inventory.setItem(1, secondChoices.peek().clone());
		updateResult();
	}
	private void updateResult() {
		AnvilResult result = recipe.getAnvilResult(new UIAnvilInventory(inventory));
		inventory.setItem(2, result.result == null || result.result.getType() == Material.AIR ? null : getDisplayItem(result.result));
	}
	@Override
	public void onPrepareAnvil(PrepareAnvilEvent event) {
		AnvilResult result = recipe.getAnvilResult(new UIAnvilInventory(inventory));
		ItemStack display = result.result == null || result.result.getType() == Material.AIR ? null : getDisplayItem(result.result);
		event.setResult(display);
		Bukkit.getScheduler().runTask(UIFramework.getInstance(), this::updateResult);
	}
	@Override
	public void onOpen(InventoryOpenEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;
		initialRenameText = getRenameText(event.getView());
		cyclingTask = new BukkitRunnable() {
			private int cycleTicks;

			@Override
			public void run() {
				if (!player.isOnline() || !inventory.equals(player.getOpenInventory().getTopInventory())) {
					cancel();
					return;
				}
				String renameText = getRenameText(player.getOpenInventory());
				if (initialRenameText == null)
					initialRenameText = renameText;
				else if (renameText != null && !Objects.equals(initialRenameText, renameText)) {
					reopenPreview(player);
					cancel();
					return;
				}
				updateResult();
				if (++cycleTicks < 20)
					return;
				cycleTicks = 0;
				cycleChoice(0, firstChoices);
				cycleChoice(1, secondChoices);
				updateResult();
			}
		}.runTaskTimer(UIFramework.getInstance(), 1L, 1L);
	}
	@Override
	public void onDrag(InventoryDragEvent event) {
		event.setCancelled(true);
	}
	@Override
	public void onClose(InventoryCloseEvent event) {
		if (cyclingTask != null)
			cyclingTask.cancel();
		if (suppressReturn)
			return;
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
		return Bukkit.createInventory(null, InventoryType.ANVIL,
				UIFUtils.convertString(itemDisplayName+" &9"+UIFramework.getLangString("menu.anvilPreview")));
	}
	private static Queue<ItemStack> getDisplayChoices(RecipeChoice choice) {
		Queue<ItemStack> choices = new ArrayDeque<>();
		if (choice instanceof RecipeChoice.ExactChoice exact) {
			exact.getChoices().stream().map(AnvilRecipePreviewMenu::getDisplayItem).forEach(choices::add);
			return choices;
		}
		RecipeChoice.MaterialChoice material = (RecipeChoice.MaterialChoice) choice;
		material.getChoices().stream().map(ItemStack::new).forEach(choices::add);
		return choices;
	}
	private void cycleChoice(int slot, Queue<ItemStack> choices) {
		if (choices.size() < 2)
			return;
		choices.add(choices.remove());
		inventory.setItem(slot, choices.peek().clone());
	}
	private void reopenPreview(Player player) {
		suppressReturn = true;
		AnvilRecipePreviewMenu preview = new AnvilRecipePreviewMenu(returnMenu, recipe, itemDisplayName);
		MenuManager.registerInventory(preview.getInventory(), preview);
		player.openInventory(preview.getInventory());
	}
	private static String getRenameText(InventoryView view) {
		String renameText = invokeRenameText("org.bukkit.inventory.view.AnvilView", view);
		return renameText != null ? renameText : invokeRenameText("org.bukkit.inventory.AnvilInventory", view.getTopInventory());
	}
	private static String invokeRenameText(String className, Object target) {
		try {
			Class<?> type = Class.forName(className);
			if (type.isInstance(target))
				return (String) type.getMethod("getRenameText").invoke(target);
		} catch (ReflectiveOperationException ignored) {
		}
		return null;
	}
	private static ItemStack getDisplayItem(ItemStack source) {
		ItemStack display = source.clone();
		GenericItem base = GenericItem.getItemBaseNoID(display);
		return base == null ? display : base.getItem();
	}
}
