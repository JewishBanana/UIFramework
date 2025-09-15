package com.github.jewishbanana.uiframework.events;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.github.jewishbanana.uiframework.items.Ability;
import com.github.jewishbanana.uiframework.items.GenericItem;

/**
 * Represents when a ability from a UIFramework affiliated plugin was triggered. This does not mean that a ability will activate! 
 * This just means that an item with this ability had an event where the ability is about to trigger from the event, some abilities 
 * may do extra condition checks within their triggers meaning that even if this event is passed it does not guarantee that the ability will 
 * activate. The involved entity will always be the directly involved entity with the associated event and not the activator of the ability!
 */
public class AbilityTriggerEvent extends Event implements Cancellable {
	
	private boolean isCancelled;
	private Ability ability;
	private Ability.Action action;
	private GenericItem baseItem;
	private Entity triggeringEntity;
	
	private static final HandlerList handlers = new HandlerList();
	 
	public AbilityTriggerEvent(Ability ability, Ability.Action action, GenericItem baseItem, Entity triggeringEntity) {
		this.ability = ability;
		this.action = action;
		this.baseItem = baseItem;
		this.triggeringEntity = triggeringEntity;
	}
	/**
	 * Gets the ability that will be triggered by this event.
	 * 
	 * @return The ability
	 */
	public Ability getAbility() {
		return ability;
	}
	/**
	 * Sets the ability that will be triggered by this event.
	 * 
	 * @param ability The ability
	 */
	public void setAbility(Ability ability) {
		this.ability = ability;
	}
	/**
	 * Gets the type of action that triggered this event.
	 * 
	 * @return The action
	 */
	public Ability.Action getAction() {
		return action;
	}
	/**
	 * Gets the related items base class to this ability being triggered.
	 * 
	 * @return The related items base class
	 */
	public GenericItem getBaseItem() {
		return baseItem;
	}
	/**
	 * Sets the related item base class of this ability trigger. Useful for abilities that may alter effects based on the exact items stats.
	 * 
	 * @param baseItem The base class of the related item
	 */
	public void setBaseItem(GenericItem baseItem) {
		this.baseItem = baseItem;
	}
	/**
	 * Gets the triggering entity of this event. This does not mean the abilities activator but the directly involved entity of the particular event! 
	 * For example if this event was triggered by an arrow hitting a block the triggering entity will be the arrow and not the shooter of the arrow which might typically be the activator of the ability.
	 * 
	 * @return This events triggering entity
	 */
	public Entity getTriggeringEntity() {
		return triggeringEntity;
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

