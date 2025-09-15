package com.github.jewishbanana.uiframework.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.projectiles.ProjectileSource;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.entities.CustomEntity;
import com.github.jewishbanana.uiframework.entities.UIEntityManager;
import com.github.jewishbanana.uiframework.events.CustomEntitySpawnEvent;

public class EntitiesListener implements Listener {
	
	private static final RandomGenerator random;
	static {
		random = RandomGenerator.of("SplittableRandom");
	}
	
	private Set<EntitySpawnEvent> spawnEvents = new HashSet<>();

	public EntitiesListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onEntitiesLoad(EntitiesLoadEvent event) {
		event.getEntities().forEach(e -> UIEntityManager.loadEntity(e));
	}
	@EventHandler
	public void onEntitiesUnload(EntitiesUnloadEvent event) {
		event.getEntities().forEach(e -> {
			CustomEntity<? extends Entity> customEntity = UIEntityManager.removeEntity(e.getUniqueId());
			if (customEntity != null)
				customEntity.unload();
		});
	}
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() != SpawnReason.NATURAL || spawnEvents.contains(event))
			return;
		final Location loc = event.getLocation();
		for (UIEntityManager type : UIEntityManager.getRegistry().values())
			if (type.getSpawnRate() != 0.0 && random.nextDouble() < type.getSpawnRate() && type.getSpawnConditions().test(event)) {
				CustomEntity<? extends Entity> customClass = UIEntityManager.spawnEntity(loc, type.getEntityClass());
				Entity entity = customClass.getEntity();
				if (entity == null)
					continue;
				EntitySpawnEvent spawn = new EntitySpawnEvent(entity);
				if (entity instanceof LivingEntity)
					spawn = new CreatureSpawnEvent((LivingEntity) entity, SpawnReason.NATURAL);
				spawnEvents.add(spawn);
				Bukkit.getPluginManager().callEvent(spawn);
				spawnEvents.remove(spawn);
				if (spawn.isCancelled()) {
					if (entity != null) {
						UIEntityManager.removeEntity(entity.getUniqueId());
						entity.remove();
					}
					continue;
				}
				CustomEntitySpawnEvent customSpawnEvent = new CustomEntitySpawnEvent(loc, entity, customClass, SpawnReason.NATURAL);
				Bukkit.getPluginManager().callEvent(customSpawnEvent);
				if (customSpawnEvent.isCancelled()) {
					if (entity != null) {
						UIEntityManager.removeEntity(entity.getUniqueId());
						entity.remove();
					}
					continue;
				}
				event.setCancelled(true);
				return;
			}
	}
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.removeEntity(event.getEntity().getUniqueId());
		if (entity != null) {
			entity.onDeath(event);
			entity.unload();
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getEntity().getUniqueId());
		if (entity != null)
			entity.onDamaged(event);
	}
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getDamager().getUniqueId());
		if (entity != null)
			entity.hitEntity(event);
		if (event.isCancelled())
			return;
		entity = UIEntityManager.getEntity(event.getEntity().getUniqueId());
		if (entity != null)
			entity.wasHit(event);
	}
	@EventHandler(ignoreCancelled = true)
	public void onProjectileLaunch(ProjectileLaunchEvent event) {
		ProjectileSource source = event.getEntity().getShooter();
		if (source instanceof LivingEntity alive) {
			CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(alive.getUniqueId());
			if (entity != null)
				entity.launchProjectile(event);
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void onProjectileHit(ProjectileHitEvent event) {
		ProjectileSource source = event.getEntity().getShooter();
		if (source instanceof LivingEntity alive) {
			CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(alive.getUniqueId());
			if (entity != null)
				entity.projectileHit(event);
		}
		if (event.getHitEntity() != null) {
			CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getHitEntity().getUniqueId());
			if (entity != null)
				entity.hitByProjectile(event);
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getEntity().getUniqueId());
		if (entity != null)
			entity.onTargetEntity(event);
		if (event.isCancelled() || event.getTarget() == null)
			return;
		entity = UIEntityManager.getEntity(event.getTarget().getUniqueId());
		if (entity != null)
			entity.wasTargeted(event);
	}
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityTargetLivingEntityEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getEntity().getUniqueId());
		if (entity != null)
			entity.onTargetLivingEntity(event);
	}
	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getRightClicked().getUniqueId());
		if (entity != null)
			entity.onInteracted(event);
	}
	@EventHandler(ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getEntity().getUniqueId());
		if (entity != null)
			entity.onCombust(event);
	}
	@EventHandler(ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		CustomEntity<? extends Entity> entity = UIEntityManager.getEntity(event.getEntity().getUniqueId());
		if (entity != null)
			entity.onChangeBlock(event);
	}
}
