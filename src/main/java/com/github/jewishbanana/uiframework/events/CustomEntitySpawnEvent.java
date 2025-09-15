package com.github.jewishbanana.uiframework.events;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.github.jewishbanana.uiframework.entities.CustomEntity;

public class CustomEntitySpawnEvent extends Event implements Cancellable {

	private boolean isCancelled;
	private Location location;
	private Entity entity;
	private CustomEntity<? extends Entity> entityClass;
	private SpawnReason reason;
	
	private static final HandlerList handlers = new HandlerList();
	
	public CustomEntitySpawnEvent(Location location, Entity entity, CustomEntity<? extends Entity> entityClass, SpawnReason reason) {
		this.location = location;
		this.entity = entity;
		this.entityClass = entityClass;
		this.reason = reason;
	}
	public Location getLocation() {
		return location;
	}
	public Entity getEntity() {
		return entity;
	}
	public CustomEntity<? extends Entity> getEntityClass() {
		return entityClass;
	}
	public SpawnReason getReason() {
		return reason;
	}
	@Override
	public boolean isCancelled() {
	    return isCancelled;
	}
	@Override
	public void setCancelled(boolean arg0) {
		this.isCancelled = arg0;
	}
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
