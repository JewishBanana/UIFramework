package com.github.jewishbanana.uiframework.events;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.github.jewishbanana.uiframework.items.Ability;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.UIEnchantment;

/**
 * Represents when a custom enchant from a UIFramework affiliated plugin was triggered. This does not mean that a custom enchant will activate! 
 * This just means that an item with the custom enchant had an event where the custom enchant is about to trigger from the event, some enchants 
 * may do extra condition checks within their triggers meaning that even if this event is passed it does not guarantee that the enchant will 
 * activate. The involved entity will always be the directly involved entity with the associated event and not the activator of the enchant!
 */
public class EnchantTriggerEvent extends Event implements Cancellable {
	
	private boolean isCancelled;
	private UIEnchantment enchant;
	private Ability.Action action;
	private GenericItem baseItem;
	private Entity triggeringEntity;
	
	private static final HandlerList handlers = new HandlerList();
	 
	public EnchantTriggerEvent(UIEnchantment enchant, Ability.Action action, GenericItem baseItem, Entity triggeringEntity) {
		this.enchant = enchant;
		this.action = action;
		this.baseItem = baseItem;
		this.triggeringEntity = triggeringEntity;
	}
	/**
	 * Gets the enchant that will be triggered by this event.
	 * 
	 * @return The custom enchant
	 */
	public UIEnchantment getEnchant() {
		return enchant;
	}
	/**
	 * Sets the enchant that will be triggered by this event.
	 * 
	 * @param enchant The custom enchant
	 */
	public void setEnchant(UIEnchantment enchant) {
		this.enchant = enchant;
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
	 * Gets the related items base class to this enchant being triggered.
	 * 
	 * @return The related items base class
	 */
	public GenericItem getBaseItem() {
		return baseItem;
	}
	/**
	 * Sets the related item base class of this enchant trigger. Useful for enchants that may alter effects based on the exact items stats.
	 * 
	 * @param baseItem The base class of the related item
	 */
	public void setBaseItem(GenericItem baseItem) {
		this.baseItem = baseItem;
	}
	/**
	 * Gets the triggering entity of this event. This does not mean the enchants activator but the directly involved entity of the particular event! 
	 * For example if this event was triggered by an arrow hitting a block the triggering entity will be the arrow and not the shooter of the arrow.
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
