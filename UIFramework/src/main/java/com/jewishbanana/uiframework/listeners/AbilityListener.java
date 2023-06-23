package com.jewishbanana.uiframework.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.GenericItem;

public class AbilityListener implements Listener {
	
	private Map<UUID, GenericItem> arrowMap = new HashMap<>();

	public AbilityListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent e) {
		if (e.useItemInHand() == Result.DENY || e.useInteractedBlock() == Result.ALLOW)
			return;
		if (e.hasItem()) {
			GenericItem item = GenericItem.getItemBase(e.getItem());
			if (item != null)
				item.interacted(e);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onHitEntity(EntityDamageByEntityEvent e) {
		if (e.isCancelled())
			return;
		if (e.getEntity() instanceof LivingEntity) {
			for (ItemStack item : ((LivingEntity) e.getEntity()).getEquipment().getArmorContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base != null)
					base.wasHit(e);
			}
			GenericItem base = GenericItem.getItemBase(((LivingEntity) e.getEntity()).getEquipment().getItemInMainHand());
			if (base != null)
				base.wasHit(e);
			base = GenericItem.getItemBase(((LivingEntity) e.getEntity()).getEquipment().getItemInOffHand());
			if (base != null)
				base.wasHit(e);
		}
		if (e.getDamager() instanceof LivingEntity) {
			for (ItemStack item : ((LivingEntity) e.getDamager()).getEquipment().getArmorContents()) {
				GenericItem base = GenericItem.getItemBase(item);
				if (base != null)
					base.hitEntity(e);
			}
			GenericItem base = GenericItem.getItemBase(((LivingEntity) e.getDamager()).getEquipment().getItemInMainHand());
			if (base != null)
				base.hitEntity(e);
			base = GenericItem.getItemBase(((LivingEntity) e.getDamager()).getEquipment().getItemInOffHand());
			if (base != null)
				base.hitEntity(e);
		}
		if (e.getDamager() instanceof Player)
			Bukkit.broadcastMessage("d "+e.getDamage()+" "+((Math.pow(((((Player) e.getDamager()).getAttackCooldown() * 12.0) + 0.5) / (1.0 / 1.6 * 20), 2.0) * 0.8 + 0.2) * 7.0)+" "+((Player) e.getDamager()).getAttackCooldown());
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
			GenericItem item = arrowMap.get(e.getEntity().getUniqueId());
			if (item != null) {
				item.projectileHit(e);
				arrowMap.remove(e.getEntity().getUniqueId());
			}
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
				arrowMap.put(e.getProjectile().getUniqueId(), base);
		}
	}
}
