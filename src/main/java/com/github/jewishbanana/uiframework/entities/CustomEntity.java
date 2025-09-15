package com.github.jewishbanana.uiframework.entities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitTask;

public abstract class CustomEntity<T extends Entity> {
	
	private UUID uuid;
	private Set<BukkitTask> tasks;
	
	public CustomEntity() {
	}
	public CustomEntity(T entity) {
		if (entity == null)
			throw new NullPointerException("The entity cannot be null!");
		this.uuid = entity.getUniqueId();
	}
	/**
	 * This method is called only once when the custom entity is first spawned. This is useful for setting equipment and attributes of the entity without overriding it when the entity is unloaded and then loaded again.
	 */
	public void spawn(Entity entity) {
	}
	public void onDeath(EntityDeathEvent event) {
	}
	public void onDamaged(EntityDamageEvent event) {
	}
	public void hitEntity(EntityDamageByEntityEvent event) {
	}
	public void wasHit(EntityDamageByEntityEvent event) {
	}
	public void launchProjectile(ProjectileLaunchEvent event) {
	}
	public void projectileHit(ProjectileHitEvent event) {
	}
	public void hitByProjectile(ProjectileHitEvent event) {
	}
	public void onTargetEntity(EntityTargetEvent event) {
	}
	public void onTargetLivingEntity(EntityTargetLivingEntityEvent event) {
	}
	public void wasTargeted(EntityTargetEvent event) {
	}
	public void onInteracted(PlayerInteractEntityEvent event) {
	}
	public void onCombust(EntityCombustEvent event) {
	}
	public void onChangeBlock(EntityChangeBlockEvent event) {
	}
	public Entity getEntity() {
		Entity entity = Bukkit.getEntity(uuid);
		if (entity == null || !entity.isValid())
			unload();
		return entity;
	}
	@SuppressWarnings("unchecked")
	public T getCastedEntity() {
		return (T) getEntity();
	}
	public void unload() {
		if (tasks != null)
			tasks.forEach(task -> task.cancel());
	}
	public void scheduleTask(BukkitTask task) {
		if (tasks == null)
			tasks = new HashSet<>();
		tasks.add(task);
	}
	public UUID getUniqueId() {
		return uuid;
	}
	public String getDisplayName() {
		return this.getClass().getSimpleName();
	}
}
