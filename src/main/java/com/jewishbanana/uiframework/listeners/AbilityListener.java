package com.jewishbanana.uiframework.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.Ability;
import com.jewishbanana.uiframework.items.GenericItem;
import com.jewishbanana.uiframework.utils.UIFUtils;
import com.mojang.datafixers.util.Pair;

public class AbilityListener implements Listener {
	
	private static Map<UUID, GenericItem> itemProjectiles = new HashMap<>();
	private static Map<UUID, Pair<Ability, GenericItem>> abilityProjectiles = new HashMap<>();

	public AbilityListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent e) {
		if (e.useItemInHand() == Result.DENY || (e.getClickedBlock() != null && UIFUtils.isInteractable(e.getClickedBlock())))
			return;
		if (e.hasItem()) {
			GenericItem item = GenericItem.getItemBase(e.getItem());
			if (item != null)
				item.interacted(e);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if (e.isCancelled())
			return;
		GenericItem item = GenericItem.getItemBase(e.getPlayer().getEquipment().getItem(e.getHand()));
		if (item != null)
			item.interactedEntity(e);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onHitEntity(EntityDamageByEntityEvent e) {
		if (e.isCancelled())
			return;
		if (e.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getEntity();
			for (ItemStack item : entity.getEquipment().getArmorContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base != null)
					base.wasHit(e);
			}
			GenericItem base = GenericItem.getItemBase(entity.getEquipment().getItemInMainHand());
			if (base != null)
				base.wasHit(e);
			base = GenericItem.getItemBase(entity.getEquipment().getItemInOffHand());
			if (base != null)
				base.wasHit(e);
			if (entity instanceof Player)
				for (ItemStack item : ((Player) entity).getInventory().getStorageContents()) {
					base = GenericItem.getItemBase(item);
					if (base != null)
						base.wasHit(e);
				}
		}
		if (e.getDamager() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getDamager();
			for (ItemStack item : entity.getEquipment().getArmorContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base != null)
					base.hitEntity(e);
			}
			GenericItem base = GenericItem.getItemBase(entity.getEquipment().getItemInMainHand());
			if (base != null)
				base.hitEntity(e);
			base = GenericItem.getItemBase(entity.getEquipment().getItemInOffHand());
			if (base != null)
				base.hitEntity(e);
			if (entity instanceof Player)
				for (ItemStack item : ((Player) entity).getInventory().getStorageContents()) {
					base = GenericItem.getItemBase(item);
					if (base != null)
						base.hitEntity(e);
				}
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onProjectileThrown(ProjectileLaunchEvent e) {
		if (e.isCancelled())
			return;
		if (e.getEntity() instanceof ThrowableProjectile) {
			ItemStack item = ((ThrowableProjectile) e.getEntity()).getItem();
			GenericItem base = GenericItem.getItemBase(item);
			if (base != null)
				base.projectileThrown(e);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.isCancelled())
			return;
		if (e.getEntity() instanceof ThrowableProjectile) {
			ItemStack item = ((ThrowableProjectile) e.getEntity()).getItem();
			GenericItem base = GenericItem.getItemBase(item);
			if (base != null)
				base.projectileHit(e);
		} else {
			GenericItem item = itemProjectiles.remove(e.getEntity().getUniqueId());
			if (item != null)
				item.projectileHit(e);
			Pair<Ability, GenericItem> pair = abilityProjectiles.remove(e.getEntity().getUniqueId());
			if (pair != null)
				pair.getFirst().projectileHit(e, pair.getSecond());
		}
		if (e.getHitEntity() != null && e.getHitEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getHitEntity();
			for (ItemStack item : entity.getEquipment().getArmorContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base != null)
					base.hitByProjectile(e);
			}
			GenericItem base = GenericItem.getItemBase(entity.getEquipment().getItemInMainHand());
			if (base != null)
				base.hitByProjectile(e);
			base = GenericItem.getItemBase(entity.getEquipment().getItemInOffHand());
			if (base != null)
				base.hitByProjectile(e);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onBowShot(EntityShootBowEvent e) {
		if (e.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(e.getBow());
		if (base != null)
			base.shotBow(e);
		if (e.shouldConsumeItem()) {
			base = GenericItem.getItemBase(e.getConsumable());
			if (base != null)
				itemProjectiles.put(e.getProjectile().getUniqueId(), base);
		}
	}
	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.isCancelled())
			return;
		if (e.getCurrentItem() != null) {
			GenericItem base = GenericItem.getItemBase(e.getCurrentItem());
			if (base != null)
				base.inventoryClick(e);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onConsumption(PlayerItemConsumeEvent e) {
		if (e.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(e.getItem());
		if (base != null)
			base.consumeItem(e);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemDrop(EntityDropItemEvent e) {
		if (e.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(e.getItemDrop().getItemStack());
		if (base != null)
			base.dropItem(e);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemPickup(EntityPickupItemEvent e) {
		if (e.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(e.getItem().getItemStack());
		if (base != null)
			base.pickupItem(e);
	}
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDeath(EntityDeathEvent e) {
		LivingEntity entity = e.getEntity();
		for (ItemStack item : entity.getEquipment().getArmorContents()) {
			GenericItem base = GenericItem.getItemBase(item);
			if (base != null)
				base.entityDeath(e);
		}
		GenericItem base = GenericItem.getItemBase(entity.getEquipment().getItemInMainHand());
		if (base != null)
			base.entityDeath(e);
		base = GenericItem.getItemBase(entity.getEquipment().getItemInOffHand());
		if (base != null)
			base.entityDeath(e);
		if (entity instanceof Player)
			for (ItemStack item : ((Player) entity).getInventory().getStorageContents()) {
				base = GenericItem.getItemBase(item);
				if (base != null)
					base.entityDeath(e);
			}
	}
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		Player entity = e.getPlayer();
		for (ItemStack item : entity.getEquipment().getArmorContents()) {
			GenericItem base = GenericItem.getItemBase(item);
			if (base != null)
				base.entityRespawn(e);
		}
		GenericItem base = GenericItem.getItemBase(entity.getEquipment().getItemInMainHand());
		if (base != null)
			base.entityRespawn(e);
		base = GenericItem.getItemBase(entity.getEquipment().getItemInOffHand());
		if (base != null)
			base.entityRespawn(e);
		if (entity instanceof Player)
			for (ItemStack item : ((Player) entity).getInventory().getStorageContents()) {
				base = GenericItem.getItemBase(item);
				if (base != null)
					base.entityRespawn(e);
			}
	}
	public static void assignProjectile(UUID uuid, GenericItem base) {
		itemProjectiles.put(uuid, base);
	}
	public static void assignProjectile(UUID uuid, Ability ability, GenericItem base) {
		abilityProjectiles.put(uuid, Pair.of(ability, base));
	}
	public static void removeProjectile(UUID uuid) {
		itemProjectiles.remove(uuid);
		abilityProjectiles.remove(uuid);
	}
}
